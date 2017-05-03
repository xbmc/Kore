package org.xbmc.kore.ui.sections.favourites;

import android.support.v4.app.Fragment;

import org.xbmc.kore.R;
import org.xbmc.kore.ui.BaseMediaActivity;

public class FavouritesActivity extends BaseMediaActivity {

    @Override
    protected String getActionBarTitle() {
        return getString(R.string.favourites);
    }

    @Override
    protected Fragment createFragment() {
        return new FavouritesListFragment();
    }
}
