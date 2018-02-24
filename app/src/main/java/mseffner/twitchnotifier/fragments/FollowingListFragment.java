package mseffner.twitchnotifier.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
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

    private static final String TAG = FollowingListFragment.class.getSimpleName();
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
        Log.i(TAG, "refreshList");
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
        Log.i(TAG, "onStart");
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
        Log.i(TAG, "runUpdateAdapterAsyncTask attempted");
        if (updateAdapterAsyncTask == null) {
            Log.i(TAG, "runUpdateAdapterAsyncTask executing");
            updateAdapterAsyncTask = new UpdateAdapterAsyncTask();
            updateAdapterAsyncTask.execute();
        }
    }

    private void runUpdateStreamsAsyncTask() {
        Log.i(TAG, "runUpdateStreamsAsyncTask attempted");
        if (updateStreamsAsyncTask == null) {
            Log.i(TAG, "runUpdateStreamsAsyncTask executing");
            updateStreamsAsyncTask = new UpdateStreamsAsyncTask();
            updateStreamsAsyncTask.execute();
        }
    }

    private void runUpdateFollowingListAsyncTask() {
        Log.i(TAG, "runUpdateFollowingListAsyncTask attempted");
        if (updateFollowingListAsyncTask == null) {
            Log.i(TAG, "runUpdateFollowingListAsyncTask executing");
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
            Log.i(TAG, "UpdateAdapterAsyncTask doInBackground");
            ChannelDb database = new ChannelDb(context);
            return database.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {
            Log.i(TAG, "UpdateAdapterAsyncTask onPostExecute");

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
                swipeRefreshLayout.setVisibility(View.GONE);
                startMessage.setVisibility(View.VISIBLE);
            } else {
                startMessage.setVisibility(View.GONE);
            }

            // Update recycler view while saving scroll position
            Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            recyclerView.setAdapter(channelAdapter);
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

            // Disable the refreshing animation
            swipeRefreshLayout.setRefreshing(false);

            updateAdapterAsyncTask = null;
        }
    }

    private class UpdateStreamsAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.i(TAG, "UpdateStreamsAsyncTask doInBackground");
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
            Log.i(TAG, "UpdateStreamsAsyncTask onPostExecute");
            updateStreamsAsyncTask = null;

            if (!isAdded() || isCancelled())
                return;

            if (!success) {
                // Use application context to get default toast style
                Toast.makeText(context.getApplicationContext(), "A network error has occurred", Toast.LENGTH_LONG).show();
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
            Log.i(TAG, "UpdateFollowingListAsyncTask doInBackground");

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
            Log.i(TAG, "UpdateFollowingListAsyncTask onPostExecute");
            updateFollowingListAsyncTask = null;

            if (!isAdded() || isCancelled())
                return;

            switch (result) {
                case NETWORK_ERROR:
                    // Use application context to get default toast style
                    Toast.makeText(context.getApplicationContext(), "A network error has occurred", Toast.LENGTH_LONG).show();
                    break;
                case INVALID_USERNAME_ERROR:
                    // Use application context to get default toast style
                    Toast.makeText(context.getApplicationContext(), "Invalid username", Toast.LENGTH_LONG).show();
                    break;
            }
            runUpdateStreamsAsyncTask();
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
