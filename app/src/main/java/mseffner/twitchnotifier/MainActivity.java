package mseffner.twitchnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Method;

import mseffner.twitchnotifier.adapters.ListPagerAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.DarkModeChangedEvent;
import mseffner.twitchnotifier.networking.PeriodicUpdater;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.settings.SettingsManager;


public class MainActivity extends AppCompatActivity {

    private static PeriodicUpdater updater = new PeriodicUpdater();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up static classes
        EventBus.getDefault().register(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsManager.initialize(sharedPreferences, getResources());
        ThreadManager.initialize();
        ChannelDb.initialize(this);
        Requests.initialize(this);
        ToastMaker.initialize(this);

        // Set the theme based on whether dark mode is on;
        boolean darkMode = SettingsManager.getDarkModeSetting();
        setTheme(darkMode ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        // Set the content view
        setContentView(R.layout.activity_main);

        // Set up ViewPager and TabLayout
        ViewPager viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tabs);
        ListPagerAdapter adapter = new ListPagerAdapter(this, getFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Destroy static classes to prevent memory leaks
        EventBus.getDefault().unregister(this);
        SettingsManager.destroy();
        ChannelDb.destroy();
        Requests.destroy();
        ToastMaker.destroy();
        ThreadManager.destroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updater.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        updater.stop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDarkModeChanged(DarkModeChangedEvent event) {
        int themeID = getThemeId();
        if (event.darkModeEnabled && themeID != R.style.AppTheme_Dark)
            recreate();
        else if (!event.darkModeEnabled && themeID != R.style.AppTheme_Light)
            recreate();
    }

    private int getThemeId() {
        // Thank you Stack Overflow for helping me to work around Android's garbage API
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
}
