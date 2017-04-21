/*
 * Copyright 2015 Synced Synapse. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.ui.sections.audio;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.AbstractInfoFragment;
import org.xbmc.kore.ui.BaseMediaActivity;
import org.xbmc.kore.utils.LogUtils;

/**
 * Controls the presentation of Music information (list, details)
 * All the information is presented by specific fragments
 */
public class MusicActivity extends BaseMediaActivity
        implements ArtistListFragment.OnArtistSelectedListener,
                   AlbumListFragment.OnAlbumSelectedListener,
                   AudioGenresListFragment.OnAudioGenreSelectedListener,
                   MusicVideoListFragment.OnMusicVideoSelectedListener {
    private static final String TAG = LogUtils.makeLogTag(MusicActivity.class);

    public static final String ALBUMID = "album_id";
    public static final String ALBUMTITLE = "album_title";
    public static final String ARTISTID = "artist_id";
    public static final String ARTISTNAME = "artist_name";
    public static final String GENREID = "genre_id";
    public static final String GENRETITLE = "genre_title";
    public static final String MUSICVIDEOID = "music_video_id";
    public static final String MUSICVIDEOTITLE = "music_video_title";

    private int selectedAlbumId = -1;
    private int selectedArtistId = -1;
    private int selectedGenreId = -1;
    private int selectedMusicVideoId = -1;
    private String selectedAlbumTitle = null;
    private String selectedArtistName = null;
    private String selectedGenreTitle = null;
    private String selectedMusicVideoTitle = null;

    @Override
    protected String getActionBarTitle() {
        if (selectedAlbumTitle != null) {
            return selectedAlbumTitle;
        } else if (selectedArtistName != null) {
            return selectedArtistName;
        } else if (selectedGenreTitle != null) {
            return selectedGenreTitle;
        } else if (selectedMusicVideoTitle != null) {
            return selectedMusicVideoTitle;
        } else {
            return getResources().getString(R.string.music);
        }
    }

    @Override
    protected Fragment createFragment() {
        return new MusicListFragment();
    }

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedAlbumId = savedInstanceState.getInt(ALBUMID, -1);
            selectedArtistId = savedInstanceState.getInt(ARTISTID, -1);
            selectedGenreId = savedInstanceState.getInt(GENREID, -1);
            selectedMusicVideoId = savedInstanceState.getInt(MUSICVIDEOID, -1);
            selectedAlbumTitle = savedInstanceState.getString(ALBUMTITLE, null);
            selectedArtistName = savedInstanceState.getString(ARTISTNAME, null);
            selectedGenreTitle = savedInstanceState.getString(GENRETITLE, null);
            selectedMusicVideoTitle = savedInstanceState.getString(MUSICVIDEOTITLE, null);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ALBUMID, selectedAlbumId);
        outState.putInt(ARTISTID, selectedArtistId);
        outState.putInt(GENREID, selectedGenreId);
        outState.putInt(MUSICVIDEOID, selectedMusicVideoId);
        outState.putString(ALBUMTITLE, selectedAlbumTitle);
        outState.putString(ARTISTNAME, selectedArtistName);
        outState.putString(GENRETITLE, selectedGenreTitle);
        outState.putString(MUSICVIDEOTITLE, selectedMusicVideoTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Only respond to this if we are showing some details, which can be checked by
                // checking if some id != -1, in which case we should go back to the previous
                // fragment, which is the list.
                // The default behaviour is handled by the nav drawer (open/close)
                boolean respond = false;
                if (selectedAlbumId != -1) {
                    selectedAlbumId = -1;
                    selectedAlbumTitle = null;
                    respond = true;
                } else if (selectedArtistId != -1) {
                    selectedArtistId = -1;
                    selectedArtistName = null;
                    respond = true;
                } else if (selectedGenreId != -1) {
                    selectedGenreId = -1;
                    selectedGenreTitle = null;
                    respond = true;
                } else if (selectedMusicVideoId != -1) {
                    selectedMusicVideoId = -1;
                    selectedMusicVideoTitle = null;
                    respond = true;
                }
                if (respond) {
                    if (selectedArtistId == -1 &&
                        selectedGenreId == -1 &&
                        selectedMusicVideoId == -1) {
                        updateActionBar(getActionBarTitle(), false);
                    } else {
                        updateActionBar(getActionBarTitle(), true);
                    }
                    getSupportFragmentManager().popBackStack();
                    return true;
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If we are showing episode or show details in portrait, clear selected and show action bar
        if (selectedAlbumId != -1) {
            selectedAlbumId = -1;
            selectedAlbumTitle = null;
        } else if (selectedArtistId != -1) {
            selectedArtistId = -1;
            selectedArtistName = null;
        } else if (selectedGenreId != -1) {
            selectedGenreId = -1;
            selectedGenreTitle = null;
        } else if (selectedMusicVideoId != -1) {
            selectedMusicVideoId = -1;
            selectedMusicVideoTitle = null;
        }

        if (selectedArtistId == -1 &&
            selectedGenreId == -1 &&
            selectedMusicVideoId == -1) {
            updateActionBar(getActionBarTitle(), false);
        } else {
            updateActionBar(getActionBarTitle(), true);
        }
        super.onBackPressed();
    }

    @TargetApi(21)
    public void onArtistSelected(ArtistListFragment.ViewHolder vh) {
        selectedArtistId = vh.dataHolder.getId();
        selectedArtistName = vh.dataHolder.getTitle();

        // Replace list fragment
        final ArtistDetailsFragment artistDetailsFragment = new ArtistDetailsFragment();
        artistDetailsFragment.setDataHolder(vh.dataHolder);
        vh.dataHolder.setSquarePoster(true);

        showFragment(artistDetailsFragment, vh.artView, vh.dataHolder);

        updateActionBar(selectedArtistName, true);
    }

    @TargetApi(21)
    public void onAlbumSelected(AlbumListFragment.ViewHolder vh) {
        selectedAlbumId = vh.dataHolder.getId();
        selectedAlbumTitle = vh.dataHolder.getTitle();

        // Replace list fragment
        final AbstractInfoFragment albumInfoFragment = new AlbumInfoFragment();
        vh.dataHolder.setSquarePoster(true);
        albumInfoFragment.setDataHolder(vh.dataHolder);

        showFragment(albumInfoFragment, vh.artView, vh.dataHolder);

        updateActionBar(selectedAlbumTitle, true);
    }

    public void onAudioGenreSelected(int genreId, String genreTitle) {
        selectedGenreId = genreId;
        selectedGenreTitle = genreTitle;

        // Replace list fragment
        AlbumListFragment albumListFragment = new AlbumListFragment();
        albumListFragment.setGenre(genreId);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_details_enter, 0, R.anim.fragment_list_popenter, 0)
                .replace(R.id.fragment_container, albumListFragment)
                .addToBackStack(null)
                .commit();

        updateActionBar(selectedGenreTitle, true);
    }

    @TargetApi(21)
    public void onMusicVideoSelected(MusicVideoListFragment.ViewHolder vh) {
        selectedMusicVideoId = vh.dataHolder.getId();
        selectedMusicVideoTitle = vh.dataHolder.getTitle();

        // Replace list fragment
        final MusicVideoInfoFragment musicVideoInfoFragment = new MusicVideoInfoFragment();
        vh.dataHolder.setSquarePoster(true);
        musicVideoInfoFragment.setDataHolder(vh.dataHolder);

        showFragment(musicVideoInfoFragment, vh.artView, vh.dataHolder);

        updateActionBar(selectedMusicVideoTitle, true);
    }
}
