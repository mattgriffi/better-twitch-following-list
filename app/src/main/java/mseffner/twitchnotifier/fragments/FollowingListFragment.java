package mseffner.twitchnotifier.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.NetworkUtils;

public class FollowingListFragment extends BaseListFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String LOG_TAG_ERROR = "Error";
    private static final int MAX_ALLOWED_ERROR_COUNT = 3;

    private ChannelAdapter channelAdapter;

    private UpdateAdapterAsyncTask updateAdapterAsyncTask;
    private UpdateFollowingListAsyncTask updateFollowingListAsyncTask;
    private UpdateStreamsAsyncTask updateStreamsAsyncTask;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void refreshList() {
        runUpdateStreamsAsyncTask();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (context != null && key.equals(context.getString(R.string.pref_username_key)))
            // If the username is changed, empty the database
            new ChannelDb(context).deleteAllChannels();
    }

    @Override
    public void onStart() {
        super.onStart();
        // On start, we want to recheck the whole following list in case the user has
        // followed or unfollowed any channels
        runUpdateFollowingListAsyncTask();
    }

    @Override
    protected void cancelAsyncTasks() {
        if (updateAdapterAsyncTask != null)
            updateAdapterAsyncTask.cancel(true);
        if (updateStreamsAsyncTask != null)
            updateStreamsAsyncTask.cancel(true);
        if (updateFollowingListAsyncTask != null)
            updateFollowingListAsyncTask.cancel(true);
    }

    private void runUpdateAdapterAsyncTask() {
        if (updateAdapterAsyncTask == null) {
            updateAdapterAsyncTask = new UpdateAdapterAsyncTask();
            updateAdapterAsyncTask.execute();
        }
    }

    private void runUpdateStreamsAsyncTask() {
        if (updateStreamsAsyncTask == null) {
            updateStreamsAsyncTask = new UpdateStreamsAsyncTask();
            updateStreamsAsyncTask.execute();
        }
    }

    private void runUpdateFollowingListAsyncTask() {
        if (updateFollowingListAsyncTask == null) {
            updateFollowingListAsyncTask = new UpdateFollowingListAsyncTask();
            updateFollowingListAsyncTask.execute();
        }
    }

    private class UpdateAdapterAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {

            ChannelDb database = new ChannelDb(context);
            return database.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {
            updateAdapterAsyncTask = null;

            if (!isAdded() || isCancelled())
                return;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Resources resources = recyclerView.getResources();
            String vodcastSetting = preferences.getString(resources.getString(R.string.pref_vodcast_key), "");

            // Reset adapter if it exists, else create a new one
            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, vodcastSetting, true);
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

            recyclerView.setAdapter(channelAdapter);

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
            ChannelDb database = new ChannelDb(context);

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
                if (isCancelled())
                    return false;
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
            updateStreamsAsyncTask = null;

            if (!isAdded() || isCancelled())
                return;

            if (!success) {
                Toast.makeText(context, "A network error has occurred", Toast.LENGTH_LONG).show();
            }
            runUpdateAdapterAsyncTask();
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

    private class UpdateFollowingListAsyncTask extends AsyncTask<Void, Void, Integer> {

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

            if (!isAdded()) {
                return ABORT;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String username = sharedPreferences.getString(getString(R.string.pref_username_key), "");
            if (username.equals("")) {
                return ABORT;
            }

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
                if (isCancelled())
                    return ABORT;
                int result = tryPopulateUserFollowedChannels(username);
                if (result == SUCCESS)
                    return SUCCESS;
                SystemClock.sleep(1000);
            }

            // Try one last time, and raise errors if it fails
            return tryPopulateUserFollowedChannels(username);
        }

        @Override
        protected void onPostExecute(Integer result) {
            updateFollowingListAsyncTask = null;

            if (!isAdded() || isCancelled())
                return;

            switch (result) {
                case NETWORK_ERROR:
                    Toast.makeText(context, "A network error has occurred", Toast.LENGTH_LONG).show();
                    runUpdateAdapterAsyncTask();
                    return;
                case INVALID_USERNAME_ERROR:
                    Toast.makeText(context, "Invalid username", Toast.LENGTH_LONG).show();
                    runUpdateAdapterAsyncTask();
                    return;
                case SUCCESS:
                    runUpdateStreamsAsyncTask();
                    return;
                case ABORT:
                    runUpdateAdapterAsyncTask();
                    return;
            }

            swipeRefreshLayout.setRefreshing(false);

        }

        private int tryPopulateUserFollowedChannels(String newUsername) {
            try {
                NetworkUtils.populateUserFollowedChannels(newUsername, new ChannelDb(context));
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
