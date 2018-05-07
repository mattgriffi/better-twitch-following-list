package mseffner.twitchnotifier;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import mseffner.twitchnotifier.adapters.ListPagerAdapter;
import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.DarkModeChangedEvent;
import mseffner.twitchnotifier.networking.PeriodicUpdater;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.settings.SettingsManager;


public class MainActivity extends AppCompatActivity {

    private static PeriodicUpdater updater = new PeriodicUpdater();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the theme based on whether dark mode is on;
        String key = getString(R.string.pref_dark_mode);
        boolean darkMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(key, false);
        int theme = darkMode ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
        setTheme(theme);
        getApplicationContext().setTheme(theme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up static classes
        EventBus.getDefault().register(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsManager.initialize(sharedPreferences, getResources());
        ThreadManager.initialize();
        Database.initialize(this);
        Requests.initialize(this);
        ToastMaker.initialize(this);

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
        Database.destroy();
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
            recreate();
    }
}
