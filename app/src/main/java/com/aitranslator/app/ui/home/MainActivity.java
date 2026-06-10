package com.aitranslator.app.ui.home;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.aitranslator.app.R;
import com.aitranslator.app.databinding.ActivityMainBinding;

/**
 * Hosts the bottom-nav and the NavHostFragment.
 *
 * Bottom-nav uses a hand-rolled listener instead of
 * NavigationUI.setupWithNavController() because the default helper has
 * two broken behaviours in a 3-tab setup:
 *
 *   1. After the first tab switch the highlight on subsequent taps stops
 *      tracking the current fragment (the helper grows the back-stack on
 *      every tap, breaking the fragment→tab mapping).
 *
 *   2. If the user is on a secondary destination reachable from a tab
 *      (e.g. History → Export), tapping that tab again is a no-op — the
 *      helper sees the destination is already in the graph and does
 *      nothing. The user gets stuck on the export screen.
 *
 * Both fixes come from one rule: "before navigating to a tab, pop the
 * back-stack to the start destination, THEN navigate". A separate
 * destination-changed listener keeps the highlight in sync with whichever
 * tab the current fragment belongs to.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            wireBottomNav();
            wireHighlightSync();
        }
    }

    /** Tab tap handler — see class-level Javadoc for why this is custom. */
    private void wireBottomNav() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int destinationId = item.getItemId();
            int currentId = navController.getCurrentDestination() != null
                    ? navController.getCurrentDestination().getId() : -1;

            // Re-tapping the active tab is a no-op (matches Material default).
            if (destinationId == currentId) return true;

            // Pop everything back to Home (the start destination) so every
            // tab switch starts from a clean slate.
            navController.popBackStack(R.id.homeFragment, false);

            // Then navigate to the chosen destination — unless it IS Home,
            // in which case the popBackStack already put us there.
            if (destinationId != R.id.homeFragment) {
                navController.navigate(destinationId);
            }
            return true;
        });

        // Re-selecting the active tab does nothing (don't recreate fragment).
        binding.bottomNavigation.setOnItemReselectedListener(item -> { /* no-op */ });
    }

    /**
     * Keeps the bottom-nav pill highlight in sync with the current
     * destination, including secondary destinations reached via a Home
     * card or the More-features bottom sheet. Without this the highlight
     * stays on the last-tapped tab even after the user has navigated
     * deep into a different area.
     */
    private void wireHighlightSync() {
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();
            int tabId;

            // Secondary destinations are mapped back to their owning tab
            // so the user always has a sense of "where am I".
            if (id == R.id.historyFragment || id == R.id.historyExportFragment) {
                tabId = R.id.historyFragment;
            } else if (id == R.id.settingsFragment
                    || id == R.id.changeLanguageFragment) {
                tabId = R.id.settingsFragment;
            } else {
                tabId = R.id.homeFragment;
            }

            if (binding.bottomNavigation.getSelectedItemId() != tabId) {
                binding.bottomNavigation.setSelectedItemId(tabId);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    /**
     * Universal back handler for the Lernix-style header back arrows.
     * Wired in every screen's XML via android:onClick="onBackPressed".
     * Goes through the NavController so deep-linked screens still pop
     * cleanly.
     */
    public void onBackPressed(android.view.View v) {
        if (navController == null || !navController.navigateUp()) {
            // No more nav stack — fall through to the system back, which
            // closes the activity.
            getOnBackPressedDispatcher().onBackPressed();
        }
    }
}
