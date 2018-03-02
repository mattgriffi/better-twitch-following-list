package mseffner.twitchnotifier.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.NetworkUtils;
import mseffner.twitchnotifier.settings.SettingsManager;

public class FollowingListFragment extends BaseListFragment {

    private static final String TAG = FollowingListFragment.class.getSimpleName();
    private static final String LOG_TAG_ERROR = "Error";
    private static final int MAX_ALLOWED_ERROR_COUNT = 3;

    private ChannelAdapter channelAdapter;

    private UpdateAdapterAsyncTask updateAdapterAsyncTask;
    private UpdateFollowingListAsyncTask updateFollowingListAsyncTask;
    private UpdateStreamsAsyncTask updateStreamsAsyncTask;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void refreshList() {
        Log.i(TAG, "refreshList");
        runUpdateStreamsAsyncTask();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        // Quickly update the adapter so the screen isn't blank
        runUpdateAdapterAsyncTask();
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

        updateAdapterAsyncTask = null;
        updateStreamsAsyncTask = null;
        updateFollowingListAsyncTask = null;
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
            return ChannelDb.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {
            Log.i(TAG, "UpdateAdapterAsyncTask onPostExecute");

            if (!isAdded() || isCancelled())
                return;

            int rerunSetting = SettingsManager.getRerunSetting();

            // Reset adapter if it exists, else create a new one
            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, rerunSetting, true);
            } else {
                channelAdapter.clear();
                channelAdapter.addAll(channelList);
                channelAdapter.updateVodcastSetting(rerunSetting);
            }

            // Show startMessage if adapter is empty, else hide it
            if (channelAdapter.getItemCount() == 0)
                startMessage.setVisibility(View.VISIBLE);
            else
                startMessage.setVisibility(View.GONE);

            // Update recycler view while saving scroll position
            Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            recyclerView.setAdapter(channelAdapter);
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

            // Disable the refreshing animation if other tasks are not running
            if (updateStreamsAsyncTask == null && updateFollowingListAsyncTask == null)
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

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
                if (isCancelled())
                    return false;
                boolean success = tryUpdateStreamData();
                if (success)
                    return true;
                SystemClock.sleep(1000);
            }

            // Try one last time, and raise errors if it fails
            return tryUpdateStreamData();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Log.i(TAG, "UpdateStreamsAsyncTask onPostExecute");
            updateStreamsAsyncTask = null;

            if (!isAdded() || isCancelled())
                return;

            if (!success)
                ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
            runUpdateAdapterAsyncTask();
        }

        private boolean tryUpdateStreamData() {
            try {
                NetworkUtils.updateStreamData();
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

            if (!isAdded())
                return ABORT;

            String username = SettingsManager.getUsername();
            if (username.equals(""))
                return ABORT;

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
                    ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
                    break;
                case INVALID_USERNAME_ERROR:
                    ToastMaker.makeToastLong(ToastMaker.MESSAGE_INVALID_USERNAME);
                    break;
            }
            runUpdateStreamsAsyncTask();
        }

        private int tryPopulateUserFollowedChannels(String newUsername) {
            try {
                NetworkUtils.populateUserFollowedChannels(newUsername);
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
