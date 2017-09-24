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

    private Context context;
    private ChannelAdapter channelAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void refreshList() {
        new UpdateStreamsAsyncTask().execute();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(context.getString(R.string.pref_username_key)))
            // If the username is changed, empty the database
            new ChannelDb(getActivity()).deleteAllChannels();
    }

    @Override
    public void onStart() {
        super.onStart();
        // On start, we want to recheck the whole following list in case the user has
        // followed or unfollowed any channels
        new UpdateFollowingListAsyncTask().execute();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context.getApplicationContext();
    }

    private class UpdateAdapterAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {

            ChannelDb database = new ChannelDb(getActivity());
            return database.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Resources resources = recyclerView.getResources();
            String vodcastSetting = preferences.getString(resources.getString(R.string.pref_vodcast_key), "");

            // Reset adapter if it exists, else create a new one
            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, vodcastSetting);
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
            ChannelDb database = new ChannelDb(getActivity());

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
                Toast.makeText(getActivity(), "A network error has occurred", Toast.LENGTH_LONG).show();
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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String username = sharedPreferences.getString(getString(R.string.pref_username_key), "");
            if (username.equals("")) {
                return ABORT;
            }

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
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
            if (result == NETWORK_ERROR) {
                Toast.makeText(getActivity(), "A network error has occurred", Toast.LENGTH_LONG).show();
            } else if (result == INVALID_USERNAME_ERROR) {
                Toast.makeText(getActivity(), "Invalid username", Toast.LENGTH_LONG).show();
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
                NetworkUtils.populateUserFollowedChannels(newUsername, new ChannelDb(getActivity()));
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
