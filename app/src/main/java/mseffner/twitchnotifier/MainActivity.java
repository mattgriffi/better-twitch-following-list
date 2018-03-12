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
import com.android.volley.ServerError;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Method;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.DarkModeChangedEvent;
import mseffner.twitchnotifier.events.UsernameChangedEvent;
import mseffner.twitchnotifier.networking.Containers;
import mseffner.twitchnotifier.networking.ErrorHandler;
import mseffner.twitchnotifier.networking.PeriodicUpdater;
import mseffner.twitchnotifier.networking.Requests;
import mseffner.twitchnotifier.networking.URLTools;
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

        // Update the follows on the first update
        SettingsManager.setFollowsNeedUpdate(true);

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUsernameChanged(UsernameChangedEvent event) {
        ChannelDb.deleteAllFollows();
        Requests.makeRequest(Requests.REQUEST_TYPE_USERS, URLTools.getUserIdUrl(),
                new Response.Listener<Containers.Users>() {
                    @Override
                    public void onResponse(Containers.Users response) {
                        if (response.data.isEmpty()) {
                            ToastMaker.makeToastLong(ToastMaker.MESSAGE_INVALID_USERNAME);
                            return;
                        }
                        Containers.Users.Data data = response.data.get(0);
                        Log.e("New username", data.display_name);
                        long newId = Long.parseLong(data.id);
                        SettingsManager.setUsernameId(newId);
                    }
                }, new ErrorHandler() {
                    @Override
                    protected void handleServerError(ServerError error) {
                        int code = error.networkResponse.statusCode;
                        if (code == 400)
                            ToastMaker.makeToastLong(ToastMaker.MESSAGE_INVALID_USERNAME);
                        else
                            ToastMaker.makeToastLong(ToastMaker.MESSAGE_SERVER_ERROR);
                    }
                });
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
}
