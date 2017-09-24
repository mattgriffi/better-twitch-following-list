package mseffner.twitchnotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.List;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.NetworkUtils;


public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG_ERROR = "Error";
    private static final int MAX_ALLOWED_ERROR_COUNT = 3;

    private RecyclerView followingList;
    private ChannelAdapter channelAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton scrollTopButton;
    private RelativeLayout startMessage;

    private boolean usernameChanged = false;
    private boolean startup = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        ListPagerAdapter adapter = new ListPagerAdapter(this, getFragmentManager());

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        // Register as shared preference listener
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        if (startup) {
//            // When the app is restarted, update the following list
//            new ChangeUserAsyncTask().execute();
//            startup = false;
//        } else if (usernameChanged) {
//            // If the user changes their username, empty the database and fetch the new list
//            new ChannelDb(this).deleteAllChannels();
//            new ChangeUserAsyncTask().execute();
//            usernameChanged = false;
//        } else {
//            // Otherwise, just refresh the stream data
//            new UpdateStreamsAsyncTask().execute();
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_following_list, menu);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // This will cause onStart to delete all data and run ChangeUserAsyncTask
        if (key.equals(getString(R.string.pref_username_key))) {
            usernameChanged = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class UpdateAdapterAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {

            ChannelDb database = new ChannelDb(getApplicationContext());
            return database.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Resources resources = followingList.getResources();
            String vodcastSetting = preferences.getString(resources.getString(R.string.pref_vodcast_key), "");

            // Reset adapter if it exists, else create a new one
            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, vodcastSetting);
                followingList.setAdapter(channelAdapter);
            } else {
                channelAdapter.clear();
                channelAdapter.addAll(channelList);
                channelAdapter.updateVodcastSetting(vodcastSetting);
            }

            // Show startMessage if adapter is empty, else hide it
            if (channelAdapter.getItemCount() == 0) {
                startMessage.setVisibility(View.VISIBLE);
            } else {
                startMessage.setVisibility(View.GONE);
            }

            // Disable the refreshing animation
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private class UpdateStreamsAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            ChannelDb database = new ChannelDb(getApplicationContext());

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
                boolean success = tryUpdateStreamData(database);
                if (success)
                    return true;
                SystemClock.sleep(1000);
            }

            // Try one last time, and raise errors if it fails
            return tryUpdateStreamData(database);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(getApplicationContext(), "A network error has occurred", Toast.LENGTH_LONG).show();
            }
            new UpdateAdapterAsyncTask().execute();
        }

        private boolean tryUpdateStreamData(ChannelDb database) {
            try {
                NetworkUtils.updateStreamData(database);
            } catch (NetworkUtils.NetworkException e) {
                Log.e(LOG_TAG_ERROR, "tryUpdateStreamData has caught NetworkException");
                return false;
            }
            return true;
        }
    }

    private class ChangeUserAsyncTask extends AsyncTask<Void, Void, Integer> {

        private static final int SUCCESS = 0;
        private static final int NETWORK_ERROR = 1;
        private static final int INVALID_USERNAME_ERROR = 2;
        private static final int ABORT = 3;

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Integer doInBackground(Void... strings) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String newUsername = sharedPreferences.getString(getString(R.string.pref_username_key), "");
            if (newUsername.equals("")) {
                return ABORT;
            }

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
                int result = tryPopulateUserFollowedChannels(newUsername);
                if (result == SUCCESS)
                    return SUCCESS;
                SystemClock.sleep(1000);
            }

            // Try one last time, and raise errors if it fails
            return tryPopulateUserFollowedChannels(newUsername);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == NETWORK_ERROR) {
                Toast.makeText(getApplicationContext(), "A network error has occurred", Toast.LENGTH_LONG).show();
            } else if (result == INVALID_USERNAME_ERROR) {
                Toast.makeText(getApplicationContext(), "Invalid username", Toast.LENGTH_LONG).show();
            } else if (result == SUCCESS) {
                new UpdateStreamsAsyncTask().execute();
                return;
            } else if (result == ABORT) {
                new UpdateAdapterAsyncTask().execute();
                return;
            }
            swipeRefreshLayout.setRefreshing(false);

        }

        private int tryPopulateUserFollowedChannels(String newUsername) {
            try {
                NetworkUtils.populateUserFollowedChannels(newUsername, new ChannelDb(getApplicationContext()));
            } catch (NetworkUtils.NetworkException e) {
                Log.e(LOG_TAG_ERROR, "tryPopulateUserFollowedChannels has caught NetworkException");
                return NETWORK_ERROR;
            } catch (NetworkUtils.InvalidUsernameException e) {
                Log.e(LOG_TAG_ERROR, "tryPopulateUserFollowedChannels has caught InvalidUsernameException");
                return INVALID_USERNAME_ERROR;
            }
            return SUCCESS;
        }
    }
}
