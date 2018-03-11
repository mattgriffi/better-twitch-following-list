package mseffner.twitchnotifier.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.events.DarkModeChangedEvent;
import mseffner.twitchnotifier.events.UsernameChangedEvent;

/**
 * SettingsManager is a class with static methods allowing all of the preferences
 * in SharedPreferences to be accessed easily from anywhere in the app without
 * requiring a context. It stores Context and Resources objects that must be
 * supplied at startup via SettingsManager.initialize().
 */
public class SettingsManager {

    // Public constants for rerun settings
    public static final int RERUN_ONLINE = 0;
    public static final int RERUN_ONLINE_TAG = 1;
    public static final int RERUN_OFFLINE = 2;

    //Public constants for sort order settings
    public static final int SORT_BY_VIEWER_COUNT = 0;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_GAME = 2;
    public static final int SORT_BY_UPTIME = 3;

    // Public constant for invalid username id
    public static final long INVALID_USERNAME_ID = -1;

    public static final long RATE_LIMIT_MILLISECONDS = 60 * 1000;

    private static SharedPreferences sharedPreferences;
    private static Resources resources;
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPreferences, key) -> onSharedPreferenceChanged(key);

    private static String usernameKey;
    private static String usernameIdKey;
    private static String rerunKey;
    private static String darkmodeKey;
    private static String lastUpdatedKey;
    private static String sortByKey;
    private static String sortAscDescKey;

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
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        SettingsManager.usernameKey = resources.getString(R.string.pref_username_key);
        SettingsManager.usernameIdKey = resources.getString(R.string.pref_username_id_key);
        SettingsManager.rerunKey = resources.getString(R.string.pref_vodcast_key);
        SettingsManager.darkmodeKey = resources.getString(R.string.pref_dark_mode);
        SettingsManager.lastUpdatedKey = resources.getString(R.string.last_updated);
        SettingsManager.sortByKey = resources.getString(R.string.pref_order_by_key);
        SettingsManager.sortAscDescKey = resources.getString(R.string.pref_order_ascending_key);
    }

    /**
     * Removes the references to SharedPreferences and Resources.
     * This should be called in MainActivity.onDestroy to prevent memory leaks.
     */
    public static void destroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        sharedPreferences = null;
        resources = null;
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
     * @return whether a valid username is set
     */
    public static boolean validUsername() {
        return !getUsername().equals("");
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
     * Returns the current order by setting. Compare result to public constants
     * SORT_BY_VIEWER_COUNT, SORT_BY_NAME, SORT_BY_GAME, and SORT_BY_UPTIME to
     * determine what the setting is.
     *
     * @return the sort by setting
     */
    public static int getSortBySetting() {
        String setting = sharedPreferences.getString(sortByKey, "");

        String name = resources.getString(R.string.pref_order_name);
        String game = resources.getString(R.string.pref_order_game);
        String uptime = resources.getString(R.string.pref_order_uptime);

        if (setting.equals(name))
            return SORT_BY_NAME;
        else if (setting.equals(game))
            return SORT_BY_GAME;
        else if (setting.equals(uptime))
            return SORT_BY_UPTIME;
        else
            return SORT_BY_VIEWER_COUNT;
    }

    /**
     * @return true if ascending, false if descending
     */
    public static boolean getSortAscendingSetting() {
        String setting = sharedPreferences.getString(sortAscDescKey, "");
        String asc = resources.getString(R.string.pref_order_ascending);
        return setting.equals(asc);
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
     * Sets the last updated time.
     */
    public static void setLastUpdated() {
        sharedPreferences.edit().putLong(lastUpdatedKey, System.nanoTime()).apply();
    }

    /**
     * @return whether or not the rate limit has reset
     */
    public static boolean rateLimitReset() {
        long lastTime = sharedPreferences.getLong(lastUpdatedKey, 0L);
        return (System.nanoTime() - lastTime) / 1000000 > RATE_LIMIT_MILLISECONDS;
    }

    /**
     * Called by preferenceChangeListener when a preference has changed.
     * Notifies all OnSettingsChangedListeners.
     */
    private static void onSharedPreferenceChanged(String key) {
        if (key.equals(usernameKey))
            EventBus.getDefault().post(new UsernameChangedEvent());
        else if (key.equals(darkmodeKey))
            EventBus.getDefault().post(new DarkModeChangedEvent(getDarkModeSetting()));
    }
}
