# Browse & Play Saved Music Playlists in Kore

## Summary

Kore lets you control whatever is *currently* playing on Kodi, but had no way to
browse the audio playlists saved on the Kodi machine (e.g. `Summer Hits.m3u`,
`Chill.pls`) and start one playing. This closes that gap by replacing the
**"Videos"** tab in the Music section with a **"Playlists"** tab that lists saved
playlists and lets you play or queue them, with correct track metadata shown
during playback.

This addresses the feature request in
[xbmc/Kore#808](https://github.com/xbmc/Kore/issues/808).

## How it works

A "saved playlist" isn't a database entity in Kodi's music library — it's a file
sitting in a special virtual folder, `special://musicplaylists/`. The feature
is built entirely on JSON-RPC calls Kore already had wrappers for:

1. **List playlists** — `Files.GetDirectory` with `directory:
   "special://musicplaylists/"`, `media: "music"`.
2. **Expand a playlist into tracks** — `Files.GetDirectory` again, this time
   passing the *playlist's own file path* as the directory. Kodi treats a
   `.m3u`/`.pls`/`.xsp` file as a browsable "directory" of its own tracks, and
   marks it as `filetype: "directory"` in listings — this quirk is the source
   of most of the work below.
3. **Play with correct metadata** — rather than calling `Player.Open` on the
   playlist file directly (which plays audio but leaves Now Playing and the
   queue empty, since Kodi has no track-level metadata for "the file that is
   the whole playlist"), we clear Kodi's active music playlist
   (`Playlist.Clear`), add each track individually (`Playlist.Add`), then call
   `Player.Open` on Kodi's *playlist*, not the file. This is the same
   mechanism Kore already uses elsewhere (e.g. playing an album), just applied
   to a playlist file's expanded contents.
4. **Queue without interrupting** — same track expansion, but append instead
   of clear, and only auto-start playback if nothing is currently playing.

Rather than write a new browsing screen from scratch, the feature reuses
`MediaFileListFragment` — the fragment that already powers Kore's "Files"
section — pointed at the playlists folder instead of the file-source root.
This meant the new functionality is almost entirely small, targeted edits to
existing, well-established code paths, instead of a parallel implementation.

## Files changed

| File | Change |
|---|---|
| `ui/sections/audio/MusicListFragment.java` | "Videos" tab → "Playlists" tab, reusing `MediaFileListFragment` |
| `ui/sections/file/MediaFileListFragment.java` | Playlist detection, correct play/queue behavior, context menu |
| `res/menu/playlist_list_item.xml` | **New.** Play/Queue context menu for playlist rows |
| `res/values/strings.xml` | Add `playlists` string (and optionally remove now-unused `music_videos`) |
| `ui/sections/audio/MusicActivity.java` | Removed now-dead music-video selection state/callback |

---

## 1. `MusicListFragment.java`

This fragment defines which fragments appear as tabs in the Music section.

**Before:**
```java
package org.xbmc.kore.ui.sections.audio;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

public class MusicListFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);

        getViewPager().registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getCurrentSelectedFragment();
                if (fragment instanceof AbstractCursorListFragment) {
                    ((AbstractCursorListFragment)fragment).saveSearchState();
                }
            }
        });
    }

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        return new TabsAdapter(this)
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(SongsListFragment.class, getArguments(), R.string.songs, 4)
                .addTab(MusicVideoListFragment.class, getArguments(), R.string.music_videos, 5);
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}
```

**After:**
```java
package org.xbmc.kore.ui.sections.audio;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.method.Files;
import org.xbmc.kore.ui.AbstractCursorListFragment;
import org.xbmc.kore.ui.AbstractTabsFragment;
import org.xbmc.kore.ui.sections.file.MediaFileListFragment;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.TabsAdapter;

public class MusicListFragment extends AbstractTabsFragment {
    private static final String TAG = LogUtils.makeLogTag(MusicListFragment.class);

    // Kodi's special virtual folder that holds saved music playlists (.m3u/.pls files)
    private static final String MUSIC_PLAYLISTS_PATH = "special://musicplaylists/";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(false);

        getViewPager().registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getCurrentSelectedFragment();
                if (fragment instanceof AbstractCursorListFragment) {
                    ((AbstractCursorListFragment)fragment).saveSearchState();
                }
            }
        });
    }

    @Override
    protected TabsAdapter createTabsAdapter(DataHolder dataHolder) {
        return new TabsAdapter(this)
                .addTab(ArtistListFragment.class, getArguments(), R.string.artists, 1)
                .addTab(AlbumListFragment.class, getArguments(), R.string.albums, 2)
                .addTab(AudioGenresListFragment.class, getArguments(), R.string.genres, 3)
                .addTab(SongsListFragment.class, getArguments(), R.string.songs, 4)
                .addTab(MediaFileListFragment.class, createPlaylistsTabArgs(), R.string.playlists, 5);
    }

    /**
     * Builds the arguments that tell the reused {@link MediaFileListFragment} to start
     * browsing directly inside Kodi's saved music playlists folder, instead of starting
     * at the top-level file Sources list (which is what it does with no arguments).
     */
    private Bundle createPlaylistsTabArgs() {
        Bundle args = new Bundle();
        args.putString(MediaFileListFragment.MEDIA_TYPE, Files.Media.MUSIC);
        args.putParcelable(MediaFileListFragment.CURRENT_LOCATION,
                new MediaFileListFragment.FileLocation(
                        getString(R.string.playlists),
                        MUSIC_PLAYLISTS_PATH,
                        true,   // isDirectory
                        null,   // no parent - this is the top of this tab
                        true)); // isRoot - stops back-navigation going further up
        return args;
    }

    @Override
    protected boolean shouldRememberLastTab() {
        return true;
    }
}
```

**Why:** `MediaFileListFragment` already supports starting from an arbitrary
directory via a `CURRENT_LOCATION` argument (used by the existing "Files"
section for its Video/Music/Pictures tabs, which all start from Sources).
Passing a `FileLocation` for `special://musicplaylists/` with `isRoot = true`
makes it open directly into the playlists folder, with no Sources screen and
no ".." row above it.

---

## 2. `MediaFileListFragment.java`

This is the shared fragment behind Kore's "Files" browsing screens. All
changes here are additive/conditional — existing behavior for regular files
and folders is unchanged.

### 2a. New import

```java
import java.util.Locale;
```
Needed for locale-independent, case-insensitive extension matching in
`isPlaylistFile()` below.

### 2b. Detecting playlist files

**New method:**
```java
    /**
     * Kodi marks saved playlist files (.m3u/.m3u8/.pls/.xsp) as "directories" in
     * Files.GetDirectory results, since their contents can be browsed like a folder.
     * We want tapping one to play/queue it instead of browsing into it.
     */
    private static boolean isPlaylistFile(String file) {
        String lower = file.toLowerCase(Locale.ROOT);
        return lower.endsWith(".m3u") || lower.endsWith(".m3u8")
                || lower.endsWith(".pls") || lower.endsWith(".xsp");
    }
```

**Why:** Kodi's `Files.GetDirectory` reports playlist files with
`filetype: "directory"` (so `ListType.ItemFile.isDirectory` is `true` for
them), because their contents can be browsed the same way a folder's can. Every
other piece of this feature exists to work around that one API quirk.

### 2c. Tapping an item — play instead of browse

**Before:**
```java
    void handleFileSelect(FileLocation f) {
        if (f == null || f.file == null) {
            // If at first level, browse Sources
            browseSources();
        } else if (f.isDirectory) {
            browseDirectory(f);
        } else {
            playMediaFile(f.file);
        }
    }
```

**After:**
```java
    void handleFileSelect(FileLocation f) {
        if (f == null || f.file == null) {
            // If at first level, browse Sources
            browseSources();
        } else if (isPlaylistFile(f.file)) {
            playPlaylistFile(f.file);
        } else if (f.isDirectory) {
            browseDirectory(f);
        } else {
            playMediaFile(f.file);
        }
    }
```

**Why:** The playlist check has to come *before* the `isDirectory` check,
since playlist files are (mis)reported as directories. Everything else is
unaffected — regular folders and regular files behave exactly as before.

### 2d. Playing/queueing a playlist with correct track metadata

```java
    private void playPlaylistFile(final String playlistFile) {
        expandAndQueuePlaylist(playlistFile, true);
    }

    private void queuePlaylistFile(final String playlistFile) {
        expandAndQueuePlaylist(playlistFile, false);
    }

    /**
     * Fetches a saved playlist's tracks. If startPlayback is true, clears Kodi's
     * active music playlist first and plays from the top once queued. If false,
     * appends to the end of whatever's already queued, and only starts a player
     * if nothing is currently playing (so we don't interrupt other music).
     */
    private void expandAndQueuePlaylist(final String playlistFile, final boolean startPlayback) {
        Files.GetDirectory action = new Files.GetDirectory(playlistFile, mediaType, null,
                ListType.FieldsFiles.TITLE, ListType.FieldsFiles.ARTIST,
                ListType.FieldsFiles.ALBUM, ListType.FieldsFiles.DURATION,
                ListType.FieldsFiles.FILE);
        action.execute(hostManager.getConnection(), new ApiCallback<List<ListType.ItemFile>>() {
            @Override
            public void onSuccess(List<ListType.ItemFile> result) {
                if (!isAdded() || result.isEmpty()) return;
                final Queue<ListType.ItemFile> tracks = new LinkedList<>(result);
                final HostConnection connection = hostManager.getConnection();

                if (startPlayback) {
                    Playlist.Clear clearAction = new Playlist.Clear(getPlaylistId(mediaType));
                    clearAction.execute(connection, new ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            queuePlaylistTracks(connection, tracks, true);
                        }

                        @Override
                        public void onError(int errorCode, String description) {
                            if (!isResumed()) return;
                            UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_queue_media_file), description));
                        }
                    }, callbackHandler);
                } else {
                    queuePlaylistTracks(connection, tracks, false);
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isResumed()) return;
                UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_play_media_file), description));
            }
        }, callbackHandler);
    }

    // Adds tracks one at a time (same "queue is a to-do list" pattern used by
    // queueMediaQueueFileLocations above). Once done: starts playback from the
    // top if startPlayback, otherwise only starts a player if nothing's playing.
    private void queuePlaylistTracks(final HostConnection connection, final Queue<ListType.ItemFile> tracks,
                                      final boolean startPlayback) {
        ListType.ItemFile track = tracks.poll();
        if (track == null) {
            if (startPlayback) {
                Player.Open openAction = new Player.Open(Player.Open.TYPE_PLAYLIST, getPlaylistId(mediaType), 0);
                openAction.execute(connection, new ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) { }

                    @Override
                    public void onError(int errorCode, String description) {
                        if (!isResumed()) return;
                        UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_play_media_file), description));
                    }
                }, callbackHandler);
            } else {
                startPlaylistIfNoActivePlayers(connection, getPlaylistId(mediaType), callbackHandler);
            }
            return;
        }

        PlaylistType.Item item = new PlaylistType.Item();
        item.file = track.file;
        Playlist.Add addAction = new Playlist.Add(getPlaylistId(mediaType), item);
        addAction.execute(connection, new ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                queuePlaylistTracks(connection, tracks, startPlayback);
            }

            @Override
            public void onError(int errorCode, String description) {
                if (!isResumed()) return;
                UIUtils.showSnackbar(getView(), String.format(getString(R.string.error_queue_media_file), description));
                queuePlaylistTracks(connection, tracks, startPlayback); // skip bad track
            }
        }, callbackHandler);
    }
```

**Why not just call `Player.Open` on the playlist file directly?** That was
the first approach tried, and it does play the audio — but Kodi treats the
whole playlist file as a single opaque "item" it's now playing, with no
per-track metadata. Now Playing and the queue both come up empty. Every other
media type in Kore that plays a *set* of items (e.g. an album) expands it into
individual tracks and queues them via `Playlist.Add`, then opens Kodi's
playlist rather than a file — this mirrors that existing pattern instead of
introducing a new one. `queuePlaylistTracks` recurses instead of looping
because each `Playlist.Add` call is asynchronous; chaining off each
`onSuccess` guarantees tracks are added in order, one at a time, before
playback starts.

No new imports were needed for this section — `Playlist`, `Player`,
`PlaylistType`, `Queue`, and `LinkedList` were already imported in this file
for its existing queueing logic.

### 2e. Context menu — show it for playlists, with playlist-appropriate actions

**Before (`ViewHolder.bindView`):**
```java
            // For the popup menu
            if (fileLocation.isDirectory) {
                contextMenu.setVisibility(View.GONE);
            } else {
                contextMenu.setVisibility(View.VISIBLE);
                contextMenu.setTag(position);
            }
```

**After:**
```java
            // For the popup menu - show it for regular files, and also for
            // playlist files even though Kodi reports those as "directories"
            if (fileLocation.isDirectory && !isPlaylistFile(fileLocation.file)) {
                contextMenu.setVisibility(View.GONE);
            } else {
                contextMenu.setVisibility(View.VISIBLE);
                contextMenu.setTag(position);
            }
```

**Before (`itemMenuClickListener`):**
```java
        private final View.OnClickListener itemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                if (fileLocationItems != null) {
                    final FileLocation loc = fileLocationItems.get(position);
                    if (!loc.isDirectory) {
                        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.getMenuInflater().inflate(R.menu.file_list_item, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(item -> {
                            int itemId = item.getItemId();
                            if (itemId == R.id.action_queue_item) {
                                queueMediaFile(loc.file);
                                return true;
                            } else if (itemId == R.id.action_play_item) {
                                playMediaFile(loc.file);
                                return true;
                            } else if (itemId == R.id.action_play_local_item) {
                                playMediaFileLocally(loc.file);
                                return true;
                            } else if (itemId == R.id.action_play_from_this_item) {
                                mediaQueueFileLocation.clear();
                                FileLocation fl;
                                for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                    fl = fileLocationItems.get(i);
                                    if (!fl.isDirectory) {
                                        mediaQueueFileLocation.add(fl);
                                    }
                                }
                                playMediaFile(loc.file);
                                return true;
                            }
                            return false;
                        });
                        popupMenu.show();
                    }
                }
            }
        };
```

**After:**
```java
        private final View.OnClickListener itemMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = (Integer)v.getTag();
                if (fileLocationItems == null) return;
                final FileLocation loc = fileLocationItems.get(position);

                if (isPlaylistFile(loc.file)) {
                    final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                    popupMenu.getMenuInflater().inflate(R.menu.playlist_list_item, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_queue_item) {
                            queuePlaylistFile(loc.file);
                            return true;
                        } else if (itemId == R.id.action_play_item) {
                            playPlaylistFile(loc.file);
                            return true;
                        }
                        return false;
                    });
                    popupMenu.show();
                } else if (!loc.isDirectory) {
                    final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                    popupMenu.getMenuInflater().inflate(R.menu.file_list_item, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_queue_item) {
                            queueMediaFile(loc.file);
                            return true;
                        } else if (itemId == R.id.action_play_item) {
                            playMediaFile(loc.file);
                            return true;
                        } else if (itemId == R.id.action_play_local_item) {
                            playMediaFileLocally(loc.file);
                            return true;
                        } else if (itemId == R.id.action_play_from_this_item) {
                            mediaQueueFileLocation.clear();
                            FileLocation fl;
                            for (int i = position + 1; i < fileLocationItems.size(); i++) {
                                fl = fileLocationItems.get(i);
                                if (!fl.isDirectory) {
                                    mediaQueueFileLocation.add(fl);
                                }
                            }
                            playMediaFile(loc.file);
                            return true;
                        }
                        return false;
                    });
                    popupMenu.show();
                }
            }
        };
```

**Why:** "Play locally"/"stream" and "Play from this item" (which queues the
rest of the current directory listing behind the tapped item) are both
meaningless for a playlist row — it represents a whole playlist, not a
position within a directory of individual songs. So playlist rows get their
own, smaller menu (`playlist_list_item.xml`, below) instead of reusing
`file_list_item.xml`; regular file rows are completely unaffected.

---

## 3. `res/menu/playlist_list_item.xml` (new file)

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item android:id="@+id/action_queue_item"
        android:title="@string/queue_file"
        android:orderInCategory="1"
        app:showAsAction="never" />
    <item android:id="@+id/action_play_item"
        android:title="@string/play_file"
        android:orderInCategory="2"
        app:showAsAction="never" />
</menu>
```

Reuses the existing `queue_file`/`play_file` strings and `action_queue_item`/
`action_play_item` IDs from `file_list_item.xml`, so wording stays consistent
with the rest of the app and no new string resources were needed for the menu
itself.

---

## 4. `res/values/strings.xml`

Add (if not already present):
```xml
<string name="playlists">Playlists</string>
```

The now-unused `music_videos` string can be removed once confirmed unreferenced
elsewhere (see cleanup note below).

---

## 5. `MusicActivity.java` — cleanup

Once Playlists fully replaced the Videos tab, the music-video selection state
and callback in `MusicActivity` became dead code (Option 1 — reusing
`MediaFileListFragment`, which handles browsing *and* playback internally —
meant `MusicActivity` needed no new callback of its own, so this is a pure
subtraction with no corresponding addition).

**Before:**
```java
public class MusicActivity extends BaseMediaActivity
        implements ArtistListFragment.OnArtistSelectedListener,
                   AlbumListFragment.OnAlbumSelectedListener,
                   AudioGenresListFragment.OnAudioGenreSelectedListener,
                   MusicVideoListFragment.OnMusicVideoSelectedListener {
    ...
    public static final String MUSICVIDEOID = "music_video_id";
    public static final String MUSICVIDEOTITLE = "music_video_title";
    ...
    private int selectedMusicVideoId = -1;
    private String selectedMusicVideoTitle = null;
    ...
    // + branches for selectedMusicVideoId/-Title in getActionBarTitle(),
    //   onCreate(), onSaveInstanceState(), onOptionsItemSelected(), onBackPressed()
    ...
    public void onMusicVideoSelected(AbstractInfoFragment.DataHolder dataHolder, ImageView sharedImageView) {
        selectedMusicVideoId = dataHolder.getId();
        selectedMusicVideoTitle = dataHolder.getTitle();
        dataHolder.setSquarePoster(true);
        showFragment(MusicVideoInfoFragment.class, dataHolder.getBundle(), sharedImageView);
        updateActionBar(selectedMusicVideoTitle, true);
    }
}
```

**After:** all of the above removed — the `implements` clause drops
`MusicVideoListFragment.OnMusicVideoSelectedListener`, the now-unused
`import org.xbmc.kore.ui.AbstractInfoFragment;` is removed, the two constants
and two fields are gone, every method's music-video branch is gone, and
`onMusicVideoSelected(...)` is deleted entirely. `MusicActivity` now only
tracks album/artist/genre selection state.

**Follow-up cleanup** (verified safe via *Find Usages* in Android Studio
before deleting, since "Music Videos" is also a real Kodi video-library
category and these classes needed to be confirmed as audio-section-only
first):
- Deleted `ui/sections/audio/MusicVideoListFragment.java`
- Deleted `ui/sections/audio/MusicVideoInfoFragment.java`
- Deleted their now-unused layout XML files
- Removed the unreferenced `music_videos` string

---

## Testing performed

- Playlists tab lists saved `.m3u`/`.pls` files from a real Kodi instance
- Subfolders inside the playlists folder (if any) browse correctly, unaffected by these changes
- Tapping a playlist clears the current queue, plays it from the start, and Now Playing shows correct track/artist info
- Context menu **Queue** appends a playlist to the end of an already-playing queue without interrupting playback
- Context menu **Play** behaves the same as tapping the row
- Regular (non-playlist) files and folders elsewhere in the Files section behave exactly as before
- Clean build with no references to the removed music-video classes/strings remaining

## Known limitations / possible follow-ups

- Tapping a playlist plays/queues it immediately; there's no way to preview
  its track list before committing (an intermediate "browse into it" step was
  functional during development, since Kodi does support it, but was replaced
  in favor of immediate play/queue for a simpler v1).
- A dedicated "playlist details" screen — showing the track list with
  play/queue as a deliberate follow-up action, similar to how Album details
  work — is a natural next step if immediate play/queue turns out to be too
  eager for some users.
