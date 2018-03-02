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
         * This method is called every time a setting is changed. Use
         * SettingsManager public constants to determine what the
         * setting is.
         *
         * @param settingChanged    the setting that was changed
         */
        void onSettingsChanged(int settingChanged);
    }

    // Public constants returned by getRerunSetting
    public static final int RERUN_ONLINE = 0;
    public static final int RERUN_ONLINE_TAG = 1;
    public static final int RERUN_OFFLINE = 2;

    // Public constant for invalid username id
    public static final long INVALID_USERNAME_ID = -1;

    // Public constants to indicate what setting has changed
    public static final int SETTING_USERNAME = 0;
    public static final int SETTING_RERUN = 1;
    public static final int SETTING_DARKMODE = 2;

    private static SharedPreferences sharedPreferences;
    private static Resources resources;
    private static List<OnSettingsChangedListener> listeners;
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SettingsManager.onSharedPreferenceChanged(key);
        }
    };

    private static String usernameKey;
    private static String usernameIdKey;
    private static String rerunKey;
    private static String darkmodeKey;

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
        SettingsManager.usernameKey = resources.getString(R.string.pref_username_key);
        SettingsManager.usernameIdKey = resources.getString(R.string.pref_username_id_key);
        SettingsManager.rerunKey = resources.getString(R.string.pref_vodcast_key);
        SettingsManager.darkmodeKey = resources.getString(R.string.pref_dark_mode);
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

    /**
     * Registers listener as a listener for settings changes. onSettingsChanged will
     * be called any time a setting is changed.
     * @param listener  an OnSettingsChangedListener
     */
    public static void registerOnSettingsChangedListener(OnSettingsChangedListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener  OnSettingsChangedListener to unregister
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static void unregisterOnSettingsChangedListener(OnSettingsChangedListener listener) {
        while (listeners.remove(listener));
    }

    /**
     * @return the user's username, may be empty String
     */
    public static String getUsername() {
        return sharedPreferences.getString(usernameKey, "");
    }

    /**
     * @return  the user's id or INVALID_USERNAME_ID
     */
    public static long getUsernameId() {
        return sharedPreferences.getLong(usernameIdKey, INVALID_USERNAME_ID);
    }

    /**
     * Writes a new username id into persistent storage.
     * @param newId the new username id
     */
    public static void setUsernameId(long newId) {
        sharedPreferences.edit().putLong(usernameIdKey, newId).apply();
    }

    /**
     * Returns the current rerun setting. Compare result to public constants
     * RERUN_ONLINE, RERUN_ONLINE_TAG, and RERUN_OFFLINE to determine what the
     * setting is.
     *
     * @return the rerun setting
     */
    public static int getRerunSetting() {
        String setting = sharedPreferences.getString(rerunKey, "");

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
        return sharedPreferences.getBoolean(darkmodeKey, false);
    }

    /**
     * Called by preferenceChangeListener when a preference has changed.
     * Notifies all OnSettingsChangedListeners.
     */
    private static void onSharedPreferenceChanged(String key) {
        int setting;
        if (key.equals(usernameKey))
            setting = SETTING_USERNAME;
        else if (key.equals(rerunKey))
            setting = SETTING_RERUN;
        else
            setting = SETTING_DARKMODE;

        for (OnSettingsChangedListener listener : listeners)
            listener.onSettingsChanged(setting);
    }
}
