package org.xbmc.kore.ui.sections.remote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import org.xbmc.kore.R;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.method.Playlist;
import org.xbmc.kore.jsonrpc.type.PlayerType.GetActivePlayersReturnType;
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.utils.Task;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless fragment that sends shared URLs to Kodi plugins.
 */
public class ShareHandlingFragment extends Fragment {

    /**
     * Static factory method.
     *
     * Creates and attaches a new instance or returns a previously added one.
     * This should be called early in whatever activity that declares the
     * intent filters. Sibling fragments can then safely call this and rest
     * assured that they are getting the same instance.
     *
     * @param fm **support** fragment manager.
     * @return the fragment instance. The host activity should immediately
     * follow this with a call to {@link #connect(HostConnection)}.
     */
    @NonNull
    public static ShareHandlingFragment of(FragmentManager fm) {
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new ShareHandlingFragment();
            fm.beginTransaction().add(fragment, TAG).commit();
        }
        return (ShareHandlingFragment) fragment;
    }

    private static final String TAG = ShareHandlingFragment.class.getCanonicalName();
    private static final String KEY_HANDLED = "share.handled";
    private static final String YOUTUBE_PREFIX = "plugin://plugin.video.youtube/play/?video_id=";
    private static final String TWITCH_PREFIX = "plugin://plugin.video.twitch/playLive/";
    private static final String VIMEO_PREFIX = "plugin://plugin.video.vimeo/play/?video_id=";
    private static final String YOUTUBE_SHORT_URL = "://youtu\\.be/([^\\?\\s/]+)";
    private static final String YOUTUBE_LONG_URL = "://(?:www\\.|m\\.)?youtube\\.com/watch\\S*[\\?&]v=([^&\\s]+)";
    private static final String TWITCH_URL = "://www\\.twitch\\.tv/([^\\?\\s/]+)";
    private static final String VIMEO_URL = "://(?:www\\.|player\\.)?vimeo\\.com[^\\?\\s]*?/(\\d+)";

    interface ShareHandler {
        /**
         * @return null when no url is recognized from this piece of data
         * passed through the intent.
         */
        @Nullable String urlFrom(@Nullable String source);
    }

    /**
     * Generic URL sharing, e.g. from reddit is fun. The link is sent through
     * the intent's {@link Intent#getData() uri data}.
     */
    static final ShareHandler GENERIC = new ShareHandler() {
        @Nullable
        @Override
        public String urlFrom(@Nullable String data) {
            if (data == null) {
                return null;
            }
            Matcher m = Pattern.compile(YOUTUBE_LONG_URL).matcher(data);
            if (m.find()) {
                return YOUTUBE_PREFIX + m.group(1);
            }
            m = Pattern.compile(YOUTUBE_SHORT_URL).matcher(data);
            if (m.find()) {
                return YOUTUBE_PREFIX + m.group(1);
            }
            m = Pattern.compile(VIMEO_URL).matcher(data);
            if (m.find()) {
                return VIMEO_PREFIX + m.group(1);
            }
            return null;
        }
    };

    /**
     * The YouTube app puts the shortened url in the intent's EXTRA_TEXT.
     */
    static final ShareHandler YOUTUBE_APP = new ShareHandler() {
        @Nullable
        @Override
        public String urlFrom(@Nullable String text) {
            if (text == null) {
                return null;
            }
            Matcher id = Pattern.compile(YOUTUBE_SHORT_URL).matcher(text);
            return id.find() ? YOUTUBE_PREFIX + id.group(1) : null;
        }
    };

    /**
     * The Twitch app puts the url in the intent's EXTRA_TEXT along with
     * some garbage.
     */
    static final ShareHandler TWITCH_APP = new ShareHandler() {
        @Nullable
        @Override
        public String urlFrom(@Nullable String text) {
            if (text == null) {
                return null;
            }
            Matcher name = Pattern.compile(TWITCH_URL).matcher(text);
            return name.find() ? TWITCH_PREFIX + name.group(1) + "/" : null;
        }
    };

    private boolean handled;
    private HostConnection connection;
    private String playlistTag;

    /**
     * @param connection Share won't be handled when null.
     */
    public void connect(HostConnection connection) {
        this.connection = connection;
    }

    /**
     * The sibling PlaylistFragment should call this when visible so that
     * it can be refreshed when the plugin url is added.
     *
     * @param tag The playlist fragment's tag. Use {@link Fragment#getTag()},
     *            don't rely on FragmentPagerAdapter's implementation details.
     */
    public void setPlaylistTag(@Nullable String tag) {
        playlistTag = tag;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            handled = savedInstanceState.getBoolean(KEY_HANDLED, false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (handled || connection == null) {
            return;
        }
        handled = true;

        Intent intent = getActivity().getIntent();
        String pluginUrl;
        switch (intent.getAction()) {
            case Intent.ACTION_SEND:
                String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
                pluginUrl = YOUTUBE_APP.urlFrom(extraText);
                pluginUrl = pluginUrl != null ? pluginUrl : TWITCH_APP.urlFrom(extraText);
                break;
            case Intent.ACTION_VIEW:
                pluginUrl = GENERIC.urlFrom(intent.getDataString());
                break;
            default:
                return;
        }

        if (pluginUrl == null) {
            say(R.string.error_share_video);
            return;
        }

        final Handler handler = new Handler();
        final String file = pluginUrl;
        isPlayingVideo(handler).start(new Task.OnFinish<Boolean>() {
            @Override
            public void got(Boolean isPlaying) {
                if (isPlaying) {
                    new Task.Sequence<>(enqueue(handler, file))
                            .then(hostNotify(handler, getString(R.string.shared_video_added)))
                            .start(refreshPlaylist());
                } else {
                    new Task.Sequence<>(clearPlaylist(handler))
                            .then(enqueue(handler, file))
                            .then(play(handler))
                            .start(refreshPlaylist());
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_HANDLED, handled);
    }

    private Task<Boolean> isPlayingVideo(final Handler handler) {
        return new Task<Boolean>() {
            @Override
            public void start(@NonNull final OnFinish<? super Boolean> then) {
                connection.execute(
                        new Player.GetActivePlayers(),
                        callback(new OnFinish<ArrayList<GetActivePlayersReturnType>>() {
                            @Override
                            public void got(ArrayList<GetActivePlayersReturnType> result) {
                                for (GetActivePlayersReturnType player : result) {
                                    if (player.type.equals(GetActivePlayersReturnType.VIDEO)) {
                                        then.got(true);
                                        return;
                                    }
                                }
                                then.got(false);
                            }
                        }, R.string.error_get_active_player),
                        handler);
            }
        };
    }

    private Task<String> clearPlaylist(final Handler handler) {
        return new Task<String>() {
            @Override
            public void start(@NonNull OnFinish<? super String> then) {
                connection.execute(
                        new Playlist.Clear(PlaylistType.VIDEO_PLAYLISTID),
                        callback(then, R.string.error_queue_media_file),
                        handler);
            }
        };
    }

    private Task<String> enqueue(final Handler handler, String file) {
        final PlaylistType.Item item = new PlaylistType.Item();
        item.file = file;
        return new Task<String>() {
            @Override
            public void start(@NonNull OnFinish<? super String> then) {
                connection.execute(
                        new Playlist.Add(PlaylistType.VIDEO_PLAYLISTID, item),
                        callback(then, R.string.error_queue_media_file),
                        handler);
            }
        };
    }

    private Task<String> hostNotify(final Handler handler, final String message) {
        return new Task<String>() {
            @Override
            public void start(@NonNull final OnFinish<? super String> then) {
                connection.execute(
                        new Player.Notification(getString(R.string.app_name), message),
                        new ApiCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                // this will never be hit, but call it anyway. it might be fixed in
                                // the future, who knows.
                                then.got(result);
                            }

                            @Override
                            public void onError(int errorCode, String description) {
                                // okhttp will barf here because of the 0-length response.
                                // there's literally no response, the server just drops the socket.
                                // is there a way to tell okhttp that this is expected?
                                if (errorCode == ApiException.IO_EXCEPTION_WHILE_SENDING_REQUEST) {
                                    then.got("");
                                } else {
                                    say(R.string.error_message, description);
                                }
                            }
                        },
                        handler);
            }
        };
    }

    private Task<String> play(final Handler handler) {
        return new Task<String>() {
            @Override
            public void start(@NonNull OnFinish<? super String> then) {
                connection.execute(
                        new Player.Open(Player.Open.TYPE_PLAYLIST, PlaylistType.VIDEO_PLAYLISTID),
                        callback(then, R.string.error_play_media_file),
                        handler);
            }
        };
    }

    private <T> ApiCallback<T>
    callback(final Task.OnFinish<? super T> then, @StringRes final int error) {
        return new ApiCallback<T>() {
            @Override
            public void onSuccess(T result) {
                then.got(result);
            }

            @Override
            public void onError(int errorCode, String description) {
                say(error, description);
            }
        };
    }

    private Task.OnFinish<Object> refreshPlaylist() {
        return new Task.OnFinish<Object>() {
            @Override
            public void got(Object unused) {
                if (playlistTag != null) {
                    ((PlaylistFragment) getFragmentManager()
                            .findFragmentByTag(playlistTag))
                            .forceRefreshPlaylist();
                }
            }
        };
    }

    private void say(@StringRes int message, Object... fmtArgs) {
        if (isAdded()) {
            Toast.makeText(getContext(), getString(message, fmtArgs), Toast.LENGTH_SHORT).show();
        }
    }

}
