package org.xbmc.kore.ui;

/**
 * Interface to pass OnBack events from activities to fragments
 */
public interface OnBackPressedListener {
    /**
     * Implement this method to handle onBackPressed events received by the activity
     *
     * @return True if the event was handled and consumed, False if the activity should handle it (pop back stack)
     */
    boolean onBackPressed();
}
