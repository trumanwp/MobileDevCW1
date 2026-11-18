package org.me.gcu.bowieday_adam_s1831372;

/**
 * Implemented by MainActivity so the fragments can ask it to change screen
 * (same idea as the listener interfaces in the Pizza fragments lab).
 * The data itself is passed through the shared TrafficViewModel.
 */
public interface NavigationListener
{
    void onSituationSelected();   // show the detail of viewModel.getSelected()

    void onShowMap();             // show viewModel.getMapSituations() on the map
}
