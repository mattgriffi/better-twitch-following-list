package mseffner.twitchnotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.NetworkUtils;


public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String LOG_TAG_ERROR = "Error";
    private static final int MAX_ALLOWED_ERROR_COUNT = 3;

    private RecyclerView followingList;
    private ChannelAdapter channelAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean usernameChanged = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        followingList = (RecyclerView) findViewById(R.id.following_list);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        swipeRefreshLayout.setRefreshing(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        followingList.setLayoutManager(layoutManager);
        followingList.setHasFixedSize(true);

        swipeRefreshLayout.setColorSchemeColors(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new UpdateStreamsAsyncTask().execute();
            }
        });

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (usernameChanged) {
            new ChannelDb(this).deleteAllChannels();
            new ChangeUserAsyncTask().execute();
            usernameChanged = false;
        } else {
            new UpdateStreamsAsyncTask().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_following_list, menu);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // This will cause onStart to reload the following list
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
            swipeRefreshLayout.setRefreshing(false);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Resources resources = followingList.getResources();
            String vodcastSetting = preferences.getString(resources.getString(R.string.pref_vodcast_key), "");

            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, vodcastSetting);
                followingList.setAdapter(channelAdapter);
            } else {
                channelAdapter.clear();
                channelAdapter.addAll(channelList);
                channelAdapter.updateVodcastSetting(vodcastSetting);
            }

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
            int errorCount = 0;
            while (errorCount < MAX_ALLOWED_ERROR_COUNT) {
                boolean successful = tryUpdateStreamDataSilently(database);
                if (successful) {
                    return true;
                }
                errorCount ++;
            }

            // Try one last time, and raise errors if it fails
            return tryUpdateStreamDataWithErrors(database);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(getApplicationContext(), "A network error has occurred", Toast.LENGTH_LONG).show();
            }
            new UpdateAdapterAsyncTask().execute();
        }

        private boolean tryUpdateStreamDataSilently(ChannelDb database) {
            try {
                NetworkUtils.updateStreamData(database);
            } catch (NetworkUtils.NetworkException e) {
                Log.e(LOG_TAG_ERROR, "tryUpdateStreamDataSilently has caught NetworkException");
                SystemClock.sleep(1000); // Wait before the next retry
                return false;
            }
            return true;
        }

        private boolean tryUpdateStreamDataWithErrors(ChannelDb database) {
            try {
                NetworkUtils.updateStreamData(database);
            } catch (NetworkUtils.NetworkException e) {
                Log.e(LOG_TAG_ERROR, "tryUpdateStreamDataWithErrors has caught NetworkException");
                return false;
            }
            return true;
        }
    }

    private class ChangeUserAsyncTask extends AsyncTask<Void, Void, Integer> {

        private static final int SUCCESS = 0;
        private static final int NETWORK_ERROR = 1;
        private static final int INVALID_USERNAME_ERROR = 2;

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Integer doInBackground(Void... strings) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String newUsername = sharedPreferences.getString(getString(R.string.pref_username_key), "");

            // Try a few times, silently retrying if it fails
            int errorCount = 0;
            while (errorCount < MAX_ALLOWED_ERROR_COUNT) {
                boolean successful = tryPopulateUserFollowedChannelsSilently(newUsername);
                if (successful) {
                    return SUCCESS;
                }
                errorCount ++;
            }

            // Try one last time, and raise errors if it fails
            return tryPopulateUserFollowedChannelsWithErrors(newUsername);
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
            }
            swipeRefreshLayout.setRefreshing(false);

        }

        private boolean tryPopulateUserFollowedChannelsSilently(String newUsername) {
            try {
                NetworkUtils.populateUserFollowedChannels(newUsername, new ChannelDb(getApplicationContext()));
            } catch (NetworkUtils.NetworkException | NetworkUtils.InvalidUsernameException e) {
                Log.e(LOG_TAG_ERROR, "tryPopulateUserFollowedChannelsWithErrors has caught " + e.getClass().getSimpleName());
                SystemClock.sleep(1000); // Wait before the next retry
                return false;
            }
            return true;
        }

        private int tryPopulateUserFollowedChannelsWithErrors(String newUsername) {
            try {
                NetworkUtils.populateUserFollowedChannels(newUsername, new ChannelDb(getApplicationContext()));
            } catch (NetworkUtils.NetworkException e) {
                Log.e(LOG_TAG_ERROR, "tryPopulateUserFollowedChannelsWithErrors has caught NetworkException");
                return NETWORK_ERROR;
            } catch (NetworkUtils.InvalidUsernameException e) {
                Log.e(LOG_TAG_ERROR, "tryPopulateUserFollowedChannelsWithErrors has caught InvalidUsernameException");
                return INVALID_USERNAME_ERROR;
            }
            return SUCCESS;
        }
    }
}
