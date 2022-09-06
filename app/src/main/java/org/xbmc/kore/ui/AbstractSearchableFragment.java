package org.xbmc.kore.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

import org.xbmc.kore.R;

public abstract class AbstractSearchableFragment
        extends AbstractListFragment
        implements SearchView.OnQueryTextListener {
    private String searchFilter = null;
    private String savedSearchFilter;
    private boolean supportsSearch;
    private boolean isPaused;

    private SearchView searchView;

    private final String BUNDLE_KEY_SEARCH_QUERY = "search_query";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            savedSearchFilter = savedInstanceState.getString(BUNDLE_KEY_SEARCH_QUERY);
        }
        searchFilter = savedSearchFilter;

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.abstractcursorlistfragment, menu);

        if (supportsSearch) {
            setupSearchMenuItem(menu, inflater);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Use this to indicate your fragment supports search queries.
     * Get the entered search query using {@link #getSearchFilter()}
     * <br/>
     * Note: make sure this is set before {@link #onCreateOptionsMenu(Menu, MenuInflater)} is called.
     * For instance in {@link #onAttach(Activity)}
     *
     * @param supportsSearch true if you support search queries, false otherwise
     */
    public void setSupportsSearch(boolean supportsSearch) {
        this.supportsSearch = supportsSearch;
    }

    /**
     * Save the search state of the list fragment
     */
    public void saveSearchState() {
        savedSearchFilter = searchFilter;
    }

    /**
     * @return text entered in searchview
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    private void setupSearchMenuItem(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.media_search, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        if (searchMenuItem != null) {
            searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setOnQueryTextListener(this);
            searchView.setQueryHint(getString(R.string.action_search));
            if (!TextUtils.isEmpty(savedSearchFilter)) {
                searchMenuItem.expandActionView();
                searchView.setQuery(savedSearchFilter, false);
                //noinspection RestrictedApi
                searchView.clearFocus();
            }

            searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    searchFilter = savedSearchFilter = null;
                    refreshList();
                    return true;
                }
            });
        }

        //Handle clearing search query using the close button (X button).
        View view = searchView.findViewById(R.id.search_close_btn);
        if (view != null) {
            view.setOnClickListener(v -> {
                EditText editText = (EditText) searchView.findViewById(R.id.search_src_text);
                editText.setText("");
                searchView.setQuery("", false);
                searchFilter = savedSearchFilter = "";
                refreshList();
            });
        }
    }


    /*
     * Search view callbacks
     */
    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextChange(String newText) {
        if ((!searchView.hasFocus()) && TextUtils.isEmpty(newText)) {
            //onQueryTextChange called as a result of manually expanding the searchView in setupSearchMenuItem(...)
            return true;
        }

        /*
         * When this fragment is paused, onQueryTextChange is called with an empty string.
         * This causes problems restoring the list fragment when returning.
         */
        if (isPaused)
            return true;

        searchFilter = newText;

        refreshList();

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onQueryTextSubmit(String newText) {
        // All is handled in onQueryTextChange
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            refreshList();
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (!TextUtils.isEmpty(searchFilter)) {
            savedSearchFilter = searchFilter;
        }
        outState.putString(BUNDLE_KEY_SEARCH_QUERY, savedSearchFilter);
        super.onSaveInstanceState(outState);
    }

     /**
     * Use this to reload the items in the list
     */
    protected abstract void refreshList();
}