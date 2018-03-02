package mseffner.twitchnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.android.volley.Response;

import java.lang.reflect.Method;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.Containers;
import mseffner.twitchnotifier.networking.Netcode;
import mseffner.twitchnotifier.settings.SettingsManager;


public class MainActivity extends AppCompatActivity implements SettingsManager.OnSettingsChangedListener,
        Response.Listener<Containers.Users> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up SettingsManager
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsManager.initialize(sharedPreferences, getResources());
        SettingsManager.registerOnSettingsChangedListener(this);

        // Set up database
        ChannelDb.initialize(this);

        // Set up Netcode
        Netcode.initialize(this);

        // Set the theme based on whether dark mode is on;
        boolean darkMode = SettingsManager.getDarkModeSetting();
        setTheme(darkMode ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        // Set the content view
        setContentView(R.layout.activity_main);

        // Set up ViewPager and TabLayout
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        ListPagerAdapter adapter = new ListPagerAdapter(this, getFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        String url = "https://api.twitch.tv/helix/users?login=takagi";
        Netcode.makeRequest(Netcode.REQUEST_TYPE_USERS, url, this, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SettingsManager.destroy();
        ChannelDb.destroy();
        Netcode.destroy();
    }

    @Override
    public void onSettingsChanged(int setting) {
        // recreate the activity to apply the new theme if needed
        if (setting == SettingsManager.SETTING_DARKMODE) {
            boolean dark = SettingsManager.getDarkModeSetting();
            int themeID = getThemeId();
            if (dark && themeID != R.style.AppTheme_Dark)
                recreate();
            else if (!dark && themeID != R.style.AppTheme_Light)
                recreate();
        }
        // If the username is changed, empty the database
        else if (setting == SettingsManager.SETTING_USERNAME)
            ChannelDb.deleteAllChannels();
    }

    private int getThemeId() {
        // Thank you StackOverflow for helping me to work around Android's garbage API
        try {
            Class<?> wrapper = Context.class;
            Method method = wrapper.getMethod("getThemeResId");
            method.setAccessible(true);
            return (Integer) method.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 0 shows an invalid resource ID
        return 0;
    }

    @Override
    public void onResponse(Containers.Users response) {
        Containers.Users.Data data = response.data.get(0);
        Log.e("id", data.id);
        Log.e("login", data.login);
        Log.e("display_name", data.display_name);
    }
}
