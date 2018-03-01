package mseffner.twitchnotifier.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.R;

/**
 * SettingsManager is a class with static methods allowing all of the preferences
 * in SharedPreferences to be accessed easily from anywhere in the app without
 * requiring a context. It stores Context and Resources objects that must be
 * supplied at startup via SettingsManager.initialize().
 */
public class SettingsManager {

    public interface OnSettingsChangedListener {
        /**
         * This method is called every time a setting is changed. It is the
         * listener's job to call SettingsManager's methods to see if a
         * relevant setting has changed.
         */
        void onSettingsChanged();
    }

    // Public constants returned by getRerunSetting
    public static final int RERUN_ONLINE = 0;
    public static final int RERUN_ONLINE_TAG = 1;
    public static final int RERUN_OFFLINE = 2;

    private static SharedPreferences sharedPreferences;
    private static Resources resources;
    private static List<OnSettingsChangedListener> listeners;
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            SettingsManager.onSharedPreferenceChanged();
        }
    };

    private SettingsManager() {}

    /**
     * Initializes the SettingsManager singleton.
     *
     * @param sharedPreferences a SharedPreferences object
     * @param resources         a Resources object for resolving Strings
     */
    public static void initialize(SharedPreferences sharedPreferences, Resources resources) {
        SettingsManager.sharedPreferences = sharedPreferences;
        SettingsManager.resources = resources;
        SettingsManager.listeners = new ArrayList<>();
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    /**
     * Removes the references to SharedPreferences and Resources.
     * This should be called in MainActivity.onDestroy to prevent memory leaks.
     */
    public static void destroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        sharedPreferences = null;
        resources = null;
        listeners = null;
    }

    public static void registerOnSettingsChangedListener(OnSettingsChangedListener listener) {
        listeners.add(listener);

    }

    /**
     * @return the user's username, may be empty String
     */
    public static String getUsername() {
        String key = resources.getString(R.string.pref_username_key);
        return sharedPreferences.getString(key, "");
    }

    /**
     * Returns the current rerun setting. Compare result to public constants
     * RERUN_ONLINE, RERUN_ONLINE_TAG, and RERUN_OFFLINE to determine what the
     * setting is.
     *
     * @return the rerun setting
     */
    public static int getRerunSetting() {
        String key = resources.getString(R.string.pref_vodcast_key);
        String setting = sharedPreferences.getString(key, "");

        String online = resources.getString(R.string.pref_vodcast_online);
        String onlineTag = resources.getString(R.string.pref_vodcast_online_tag);

        if (setting.equals(online))
            return RERUN_ONLINE;
        else if (setting.equals(onlineTag))
            return RERUN_ONLINE_TAG;
        else
            return RERUN_OFFLINE;
    }

    /**
     * Returns the current dark mode setting.
     *
     * @return true if dark mode is on, else false
     */
    public static boolean getDarkModeSetting() {
        String key = resources.getString(R.string.pref_dark_mode);
        return sharedPreferences.getBoolean(key, false);
    }

    /**
     * Called by preferenceChangeListener when a preference has changed.
     * Notifies all OnSettingsChangedListeners.
     */
    private static void onSharedPreferenceChanged() {
        for (OnSettingsChangedListener listener : listeners) {
            listener.onSettingsChanged();
        }
    }
}
