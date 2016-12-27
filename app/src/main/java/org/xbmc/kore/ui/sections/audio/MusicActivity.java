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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseActivity;
import org.xbmc.kore.ui.generic.NavigationDrawerFragment;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * Controls the presentation of Music information (list, details)
 * All the information is presented by specific fragments
 */
public class MusicActivity extends BaseActivity
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

    private NavigationDrawerFragment navigationDrawerFragment;

    private MusicListFragment musicListFragment;

    private boolean clearSharedElements;

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_media);

        // Set up the drawer.
        navigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        navigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

        if (savedInstanceState == null) {
            musicListFragment = new MusicListFragment();

            // Setup animations
            if (Utils.isLollipopOrLater()) {
                musicListFragment.setExitTransition(null);
                musicListFragment.setReenterTransition(TransitionInflater
                        .from(this)
                        .inflateTransition(android.R.transition.fade));
                musicListFragment.setExitSharedElementCallback(new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        // Clearing must be done in the reentering fragment
                        // as this is called last. Otherwise, the app will crash during transition setup. Not sure, but might
                        // be a v4 support package bug.
                        if (clearSharedElements) {
                            names.clear();
                            sharedElements.clear();
                            clearSharedElements = false;
                        }
                    }
                });
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, musicListFragment)
                    .commit();
        } else {
            selectedAlbumId = savedInstanceState.getInt(ALBUMID, -1);
            selectedArtistId = savedInstanceState.getInt(ARTISTID, -1);
            selectedGenreId = savedInstanceState.getInt(GENREID, -1);
            selectedMusicVideoId = savedInstanceState.getInt(MUSICVIDEOID, -1);
            selectedAlbumTitle = savedInstanceState.getString(ALBUMTITLE, null);
            selectedArtistName = savedInstanceState.getString(ARTISTNAME, null);
            selectedGenreTitle = savedInstanceState.getString(GENRETITLE, null);
            selectedMusicVideoTitle = savedInstanceState.getString(MUSICVIDEOTITLE, null);
        }

        setupActionBar(selectedAlbumTitle, selectedArtistName, selectedGenreTitle, selectedMusicVideoTitle);

//        // Setup system bars and content padding, allowing averlap with the bottom bar
//        setupSystemBarsColors();
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.fragment_container), true, true, true);
//        UIUtils.setPaddingForSystemBars(this, findViewById(R.id.drawer_layout), true, true, true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!navigationDrawerFragment.isDrawerOpen()) {
//            getMenuInflater().inflate(R.menu.media_info, menu);
//        }
        getMenuInflater().inflate(R.menu.media_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_remote:
                // Starts remote
                Intent launchIntent = new Intent(this, RemoteActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(launchIntent);
                return true;
            case android.R.id.home:
                // Only respond to this if we are showing some details, which can be checked by
                // checking if some id != -1, in which case we should go back to the previous
                // fragment, which is the list.
                // The default behaviour is handled by the nav drawer (open/close)
                if (selectedAlbumId != -1) {
                    selectedAlbumId = -1;
                    selectedAlbumTitle = null;
                    setupActionBar(null, selectedArtistName, selectedGenreTitle, selectedMusicVideoTitle);
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else if (selectedArtistId != -1) {
                    selectedArtistId = -1;
                    selectedArtistName = null;
                    setupActionBar(selectedAlbumTitle, null, selectedGenreTitle, selectedMusicVideoTitle);
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else if (selectedGenreId != -1) {
                    selectedGenreId = -1;
                    selectedGenreTitle = null;
                    setupActionBar(selectedAlbumTitle, selectedArtistName, null, selectedMusicVideoTitle);
                    getSupportFragmentManager().popBackStack();
                    return true;
                } else if (selectedMusicVideoId != -1) {
                    selectedMusicVideoId = -1;
                    selectedMusicVideoTitle = null;
                    setupActionBar(selectedAlbumTitle, selectedArtistName, selectedGenreTitle, null);
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
            setupActionBar(null, selectedArtistName, selectedGenreTitle, selectedMusicVideoTitle);
        } else if (selectedArtistId != -1) {
            selectedArtistId = -1;
            selectedArtistName = null;
            setupActionBar(selectedAlbumTitle, null, selectedGenreTitle, selectedMusicVideoTitle);
        } else if (selectedGenreId != -1) {
            selectedGenreId = -1;
            selectedGenreTitle = null;
            setupActionBar(selectedAlbumTitle, selectedArtistName, null, selectedMusicVideoTitle);
        } else if (selectedMusicVideoId != -1) {
            selectedMusicVideoId = -1;
            selectedMusicVideoTitle = null;
            setupActionBar(selectedAlbumTitle, selectedArtistName, selectedGenreTitle, null);
        }
        super.onBackPressed();
    }

    private boolean drawerIndicatorIsArrow = false;
    private void setupActionBar(String albumTitle, String artistName, String genreTitle,
                                String musicVideoTitle) {
        Toolbar toolbar = (Toolbar)findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);


        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (albumTitle != null) {
            actionBar.setTitle(albumTitle);
        } else if (artistName != null) {
            actionBar.setTitle(artistName);
        } else if (genreTitle != null) {
            actionBar.setTitle(genreTitle);
        } else if (musicVideoTitle != null) {
            actionBar.setTitle(musicVideoTitle);
        } else {
            actionBar.setTitle(R.string.music);
        }

        if ((albumTitle != null) || (artistName != null) || (genreTitle != null) || (musicVideoTitle != null)) {
            if (!drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(true);
                drawerIndicatorIsArrow = true;
            }
        } else {
            if (drawerIndicatorIsArrow) {
                navigationDrawerFragment.animateDrawerToggle(false);
                drawerIndicatorIsArrow = false;
            }
        }

    }

    @TargetApi(21)
    public void onArtistSelected(ArtistListFragment.ViewHolder viewHolder) {
        selectedArtistId = viewHolder.artistId;
        selectedArtistName = viewHolder.artistName;

        // Replace list fragment
        final ArtistDetailsFragment artistDetailsFragment = ArtistDetailsFragment.newInstance(viewHolder);

        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();
        // Setup animations
        if (Utils.isLollipopOrLater()) {
            android.support.v4.app.SharedElementCallback seCallback = new android.support.v4.app.SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    View sharedView = artistDetailsFragment.getSharedElement();
                    if (sharedView == null) { // shared element not visible
                        clearSharedElements = true;
                    }
                }
            };
            artistDetailsFragment.setEnterSharedElementCallback(seCallback);

            artistDetailsFragment.setEnterTransition(TransitionInflater
                    .from(this)
                    .inflateTransition(R.transition.media_details));
            artistDetailsFragment.setReturnTransition(null);

            Transition changeImageTransition = TransitionInflater.from(
                    this).inflateTransition(R.transition.change_image);
            artistDetailsFragment.setSharedElementReturnTransition(changeImageTransition);
            artistDetailsFragment.setSharedElementEnterTransition(changeImageTransition);
            fragTrans.addSharedElement(viewHolder.artView, viewHolder.artView.getTransitionName());
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0, R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, artistDetailsFragment)
                .addToBackStack(null)
                .commit();

        navigationDrawerFragment.animateDrawerToggle(true);
        setupActionBar(null, selectedArtistName, null, null);
    }

    @TargetApi(21)
    public void onAlbumSelected(AlbumListFragment.ViewHolder vh) {
        selectedAlbumId = vh.albumId;
        selectedAlbumTitle = vh.albumTitle;

        // Replace list fragment
        final AlbumDetailsFragment albumDetailsFragment = AlbumDetailsFragment.newInstance(vh);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            android.support.v4.app.SharedElementCallback seCallback = new android.support.v4.app.SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    //On returning onMapSharedElements for the exiting fragment is called before the onMapSharedElements
                    // for the reentering fragment. We use this to determine if we are returning and if
                    // we should clear the shared element lists. Note that, clearing must be done in the reentering fragment
                    // as this is called last. Otherwise it the app will crash during transition setup. Not sure, but might
                    // be a v4 support package bug.
                    if (albumDetailsFragment.isVisible()) {
                        View sharedView = albumDetailsFragment.getSharedElement();
                        if (sharedView == null) { // shared element not visible
                            clearSharedElements = true;
                        }
                    }
                }
            };
            albumDetailsFragment.setEnterSharedElementCallback(seCallback);

            albumDetailsFragment.setEnterTransition(TransitionInflater
                    .from(this)
                    .inflateTransition(R.transition.media_details));
            albumDetailsFragment.setReturnTransition(null);

            Transition changeImageTransition = TransitionInflater.from(
                    this).inflateTransition(R.transition.change_image);
            albumDetailsFragment.setSharedElementReturnTransition(changeImageTransition);
            albumDetailsFragment.setSharedElementEnterTransition(changeImageTransition);

            fragTrans.addSharedElement(vh.artView, vh.artView.getTransitionName());
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                    R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, albumDetailsFragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(selectedAlbumTitle, null, null, null);
    }

    public void onAudioGenreSelected(int genreId, String genreTitle) {
        selectedGenreId = genreId;
        selectedGenreTitle = genreTitle;

        // Replace list fragment
        AlbumListFragment albumListFragment = AlbumListFragment.newInstanceForGenre(genreId);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_details_enter, 0, R.anim.fragment_list_popenter, 0)
                .replace(R.id.fragment_container, albumListFragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(null, null, genreTitle, null);
    }

    @TargetApi(21)
    public void onMusicVideoSelected(MusicVideoListFragment.ViewHolder vh) {
        selectedMusicVideoId = vh.musicVideoId;
        selectedMusicVideoTitle = vh.musicVideoTitle;

        // Replace list fragment
        final MusicVideoDetailsFragment detailsFragment = MusicVideoDetailsFragment.newInstance(vh);
        FragmentTransaction fragTrans = getSupportFragmentManager().beginTransaction();

        // Set up transitions
        if (Utils.isLollipopOrLater()) {
            android.support.v4.app.SharedElementCallback seCallback = new android.support.v4.app.SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    //On returning onMapSharedElements for the exiting fragment is called before the onMapSharedElements
                    // for the reentering fragment. We use this to determine if we are returning and if
                    // we should clear the shared element lists. Note that, clearing must be done in the reentering fragment
                    // as this is called last. Otherwise it the app will crash during transition setup. Not sure, but might
                    // be a v4 support package bug.
                    if (detailsFragment.isVisible()) {
                        View sharedView = detailsFragment.getSharedElement();
                        if (sharedView == null) { // shared element not visible
                            LogUtils.LOGD(TAG, "onMusicVideoSelected: setting clearedSharedElements to true");
                            clearSharedElements = true;
                        }
                    }
                }
            };
            detailsFragment.setEnterSharedElementCallback(seCallback);

            detailsFragment.setEnterTransition(TransitionInflater
                    .from(this)
                    .inflateTransition(R.transition.media_details));
            detailsFragment.setReturnTransition(null);

            Transition changeImageTransition = TransitionInflater.from(
                    this).inflateTransition(R.transition.change_image);
            detailsFragment.setSharedElementReturnTransition(changeImageTransition);
            detailsFragment.setSharedElementEnterTransition(changeImageTransition);

            fragTrans.addSharedElement(vh.artView, vh.artView.getTransitionName());
        } else {
            fragTrans.setCustomAnimations(R.anim.fragment_details_enter, 0,
                    R.anim.fragment_list_popenter, 0);
        }

        fragTrans.replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
        setupActionBar(null, null, null, selectedMusicVideoTitle);
    }
}
