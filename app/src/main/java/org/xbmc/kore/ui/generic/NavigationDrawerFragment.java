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
package org.xbmc.kore.ui.generic;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.MaterialColors;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.ui.sections.addon.AddonsActivity;
import org.xbmc.kore.ui.sections.audio.MusicActivity;
import org.xbmc.kore.ui.sections.favourites.FavouritesActivity;
import org.xbmc.kore.ui.sections.file.FileActivity;
import org.xbmc.kore.ui.sections.hosts.HostManagerActivity;
import org.xbmc.kore.ui.sections.localfile.LocalFileActivity;
import org.xbmc.kore.ui.sections.remote.RemoteActivity;
import org.xbmc.kore.ui.sections.settings.SettingsActivity;
import org.xbmc.kore.ui.sections.video.MoviesActivity;
import org.xbmc.kore.ui.sections.video.PVRActivity;
import org.xbmc.kore.ui.sections.video.TVShowsActivity;
import org.xbmc.kore.utils.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {
    private static final String TAG = LogUtils.makeLogTag(NavigationDrawerFragment.class);

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    public static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    public static final int ACTIVITY_HOSTS = 0,
            ACTIVITY_REMOTE = 1,
            ACTIVITY_MOVIES = 2,
            ACTIVITY_TVSHOWS = 3,
            ACTIVITY_MUSIC = 4,
            ACTIVITY_PVR = 5,
            ACTIVITY_FILES = 6,
            ACTIVITY_ADDONS = 7,
            ACTIVITY_SETTINGS = 8,
            ACTIVITY_FAVOURITES = 9,
            ACTIVITY_LOCAL_FILES = 10;


    // The current selected item id (based on the activity)
    private static int selectedItemId = -1;

    // Delay to close the drawer (ms)
    private static final int CLOSE_DELAY = 250;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private boolean mUserLearnedDrawer;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(R.layout.fragment_navigation_drawer,
                container, false);
        mDrawerListView.setOnItemClickListener((parent, view, position, id) -> {
            DrawerItem item = (DrawerItem)parent.getItemAtPosition(position);
            selectItem(item, position);
        });

        HostInfo hostInfo = HostManager.getInstance(requireContext()).getHostInfo();
        String hostName = (hostInfo != null) ? hostInfo.getName() : getString(R.string.xbmc_media_center);
        int hostId = (hostInfo != null) ? hostInfo.getId() : 0;

        Set<String> shownItems = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getStringSet(Settings.getNavDrawerItemsPrefKey(hostId),
                              new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.entry_values_nav_drawer_items))));

        ArrayList<DrawerItem> items = new ArrayList<>(15);
        items.add(new DrawerItem(DrawerItem.TYPE_HOST, ACTIVITY_HOSTS, hostName, R.drawable.ic_round_devices_24));
        items.add(new DrawerItem()); // Divider
        items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_REMOTE,
                                 getString(R.string.remote), R.drawable.ic_round_gamepad_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_MOVIES)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_MOVIES,
                                     getString(R.string.movies), R.drawable.ic_round_movie_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_TVSHOWS)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_TVSHOWS,
                                     getString(R.string.tv_shows), R.drawable.ic_round_tv_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_MUSIC)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_MUSIC,
                                     getString(R.string.music), R.drawable.ic_round_headphones_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_PVR)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_PVR,
                                     getString(R.string.pvr), R.drawable.ic_round_dvr_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_FAVOURITES)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_FAVOURITES,
                    getString(R.string.favourites), R.drawable.ic_round_star_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_FILES)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_FILES,
                    getString(R.string.files), R.drawable.ic_round_folder_24));
        if (shownItems.contains(String.valueOf(ACTIVITY_LOCAL_FILES)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_LOCAL_FILES,
                    getString(R.string.local_files), R.drawable.ic_round_snippet_folder_24));

        if (shownItems.contains(String.valueOf(ACTIVITY_ADDONS)))
            items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_ADDONS,
                                     getString(R.string.addons), R.drawable.ic_round_extension_24));
        items.add(new DrawerItem()); // Divider
        items.add(new DrawerItem(DrawerItem.TYPE_NORMAL_ITEM, ACTIVITY_SETTINGS,
                                 getString(R.string.settings), R.drawable.ic_round_settings_24));
        mDrawerListView.setAdapter(new DrawerItemAdapter(
                getActivity(),
                R.layout.item_navigation_drawer,
                items.toArray(new DrawerItem[items.size()])));

        return mDrawerListView;
    }

    @Override
    public void onResume() {
        super.onResume();
        selectedItemId = getItemIdFromActivity();
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = requireActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        );

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(() -> mDrawerToggle.syncState());

        // Not using mDrawerToggle as the listener to not confuse the drawer icon.
        // The icon will be used exclusively for navigation, not for indicating whether the drawer layout is opened or closed.
        // Nevertheless, a listener needs to be set up to save the userLearnedDrawer preference.
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                if (!isAdded()) {
                    return;
                }
                saveUserLearnedDrawer();
                requireActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (!isAdded()) {
                    return;
                }
                saveUserLearnedDrawer();
                requireActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerStateChanged(int newState) { }
        });
        selectedItemId = getItemIdFromActivity();
    }

    /**
     * Animates the drawerToggle from the hamburger to an arrow or vice versa
     * @param toArrow True, hamburger to arrow, false arrow to hamburger
     */
    public void animateDrawerToggle(final boolean toArrow) {
        float start = toArrow ? 0.0f : 1.0f,
                end = 1.0f - start;

        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.addUpdateListener(valueAnimator -> {
            float slideOffset = (Float) valueAnimator.getAnimatedValue();
            mDrawerToggle.onDrawerSlide(mDrawerLayout, slideOffset);
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(500);
        anim.start();
    }

    /**
     * Auxiliary method to show/hide the drawer indicator
     * @param isEnabled Show/hide enable drawer indicator
     */
    public void setDrawerIndicatorEnabled(boolean isEnabled) {
        mDrawerToggle.setDrawerIndicatorEnabled(isEnabled);
    }

    public boolean isDrawerIndicatorEnabled() {
        return mDrawerToggle.isDrawerIndicatorEnabled();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    private void saveUserLearnedDrawer() {
        if (!mUserLearnedDrawer) {
            mUserLearnedDrawer = true;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();

            // Sync the drawer toggle on the first run
            mDrawerLayout.post(() -> mDrawerToggle.syncState());
        }
    }

    /**
     * Maps from the current activity to the private Item Id on the drawer
     * @return Item if of the current activity
     */
    private int getItemIdFromActivity() {
        Activity activity = getActivity();

        if (activity instanceof HostManagerActivity)
            return ACTIVITY_HOSTS;
        else if (activity instanceof RemoteActivity)
            return ACTIVITY_REMOTE;
        else if (activity instanceof MoviesActivity)
            return ACTIVITY_MOVIES;
        else if (activity instanceof TVShowsActivity)
            return ACTIVITY_TVSHOWS;
        else if (activity instanceof MusicActivity)
            return ACTIVITY_MUSIC;
        else if (activity instanceof PVRActivity)
            return ACTIVITY_PVR;
        else if (activity instanceof FavouritesActivity)
            return ACTIVITY_FAVOURITES;
        else if (activity instanceof FileActivity)
            return ACTIVITY_FILES;
        else if (activity instanceof LocalFileActivity)
            return ACTIVITY_LOCAL_FILES;
        else if (activity instanceof AddonsActivity)
            return ACTIVITY_ADDONS;
        else if (activity instanceof SettingsActivity)
            return ACTIVITY_SETTINGS;

        return -1;
    }

    /**
     * Map from the Item Id to the activities
     */
    private static final SparseArray<Class> activityItemIdMap;
    static {
        activityItemIdMap = new SparseArray<>(11);
        activityItemIdMap.put(ACTIVITY_HOSTS, HostManagerActivity.class);
        activityItemIdMap.put(ACTIVITY_REMOTE, RemoteActivity.class);
        activityItemIdMap.put(ACTIVITY_MOVIES, MoviesActivity.class);
        activityItemIdMap.put(ACTIVITY_MUSIC, MusicActivity.class);
        activityItemIdMap.put(ACTIVITY_FAVOURITES, FavouritesActivity.class);
        activityItemIdMap.put(ACTIVITY_FILES, FileActivity.class);
        activityItemIdMap.put(ACTIVITY_LOCAL_FILES, LocalFileActivity.class);
        activityItemIdMap.put(ACTIVITY_TVSHOWS, TVShowsActivity.class);
        activityItemIdMap.put(ACTIVITY_PVR, PVRActivity.class);
        activityItemIdMap.put(ACTIVITY_ADDONS, AddonsActivity.class);
        activityItemIdMap.put(ACTIVITY_SETTINGS, SettingsActivity.class);

    }
    private void selectItem(DrawerItem item, int position) {
        if (item.type == DrawerItem.TYPE_DIVIDER) return;

        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        mDrawerLayout.closeDrawer(GravityCompat.START);

        // Same activity, just return
        if (item.id == getItemIdFromActivity())
            return;

        final Intent launchIntentFinal = new Intent(getActivity(),
                activityItemIdMap.get(item.id))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mDrawerLayout.postDelayed(() -> {
            startActivity(launchIntentFinal);
            requireActivity().overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
        }, CLOSE_DELAY);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
//		outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

    }

    /**
     * Auxiliary class to hold the description and icon of a drawer icon
     */
    public static class DrawerItem {
        public static final int TYPE_HOST = 0,
                TYPE_DIVIDER = 1,
                TYPE_NORMAL_ITEM = 2;
        public static final int DEFAULT_DIVIDER_ID = -1;

        public final int id;
        public final int type;
        public final String desc;
        public final int iconResourceId;

        /**
         * Creates a standard drawer item
         * @param desc Name of the item
         * @param icon Icon to show
         */
        public DrawerItem(int type, int id, String desc, int icon) {
            this.type = type;
            this.id = id;
            this.desc = desc;
            this.iconResourceId = icon;
        }

        /**
         * Creates a divider drawer item
         */
        public DrawerItem() {
            this.id = DEFAULT_DIVIDER_ID;
            this.type = TYPE_DIVIDER;
            this.desc = null;
            this.iconResourceId = 0;
        }
    }

    public static class DrawerItemAdapter extends ArrayAdapter<DrawerItem> {

        private final int itemBackgroundColor, itemColor, hostBackgroundColor, hostColor;

        public DrawerItemAdapter(Context context, int layoutId, DrawerItem[] objects) {
            super(context, layoutId, objects);

            itemBackgroundColor = MaterialColors.getColor(context, R.attr.colorPrimaryContainer, null);
            itemColor = MaterialColors.getColor(context, R.attr.colorOnPrimaryContainer, null);
            hostBackgroundColor = MaterialColors.getColor(context, R.attr.colorPrimaryContainer, null);
            hostColor = MaterialColors.getColor(context, R.attr.colorOnPrimaryContainer, null);
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).type;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            DrawerItem item = getItem(position);

            ImageView icon;
            TextView desc;
            switch (item.type) {
                case DrawerItem.TYPE_DIVIDER:
                    if (convertView == null) {
                        convertView = ((LayoutInflater)getContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.item_divider, parent, false);
                    }
                    break;
                case DrawerItem.TYPE_NORMAL_ITEM:
                    if (convertView == null) {
                        convertView = ((LayoutInflater)getContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.item_navigation_drawer, parent, false);
                    }
                    icon = convertView.findViewById(R.id.drawer_item_icon);
                    icon.setImageResource(item.iconResourceId);
                    desc = convertView.findViewById(R.id.drawer_item_title);
                    desc.setText(item.desc);
                    if (selectedItemId == item.id) {
                        convertView.setBackgroundColor(itemBackgroundColor);
                        icon.setColorFilter(itemColor);
                        desc.setTextColor(itemColor);
                    }
                    break;
                case DrawerItem.TYPE_HOST:
                    if (convertView == null) {
                        convertView = ((LayoutInflater)getContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.item_navigation_drawer_host, parent, false);
                    }
                    icon = convertView.findViewById(R.id.drawer_item_icon);
                    icon.setImageResource(item.iconResourceId);
                    desc = convertView.findViewById(R.id.drawer_item_title);
                    desc.setText(item.desc);
                    if (selectedItemId == item.id) {
                        convertView.setBackgroundColor(hostBackgroundColor);
                        icon.setColorFilter(hostColor);
                        desc.setTextColor(hostColor);
                    }
                    break;
            }
            return convertView;
        }
    }
}
