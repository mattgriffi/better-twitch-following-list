package mseffner.twitchnotifier.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.networking.NetworkUtils;

public class TopListFragment extends BaseListFragment {

    private static final String LOG_TAG_ERROR = "Error";
    private static final int MAX_ALLOWED_ERROR_COUNT = 3;

    private ChannelAdapter channelAdapter;

    private UpdateTopStreamsAsyncTask updateTopStreamsAsyncTask;

    @Override
    protected void refreshList() {
        runUpdateTopStreamsAsyncTask();
    }

    @Override
    public void onStart() {
        super.onStart();
        runUpdateTopStreamsAsyncTask();
    }

    @Override
    protected void cancelAsyncTasks() {
        if (updateTopStreamsAsyncTask != null)
            updateTopStreamsAsyncTask.cancel(true);
    }

    private void runUpdateTopStreamsAsyncTask() {
        if (updateTopStreamsAsyncTask == null) {
            updateTopStreamsAsyncTask = new UpdateTopStreamsAsyncTask();
            updateTopStreamsAsyncTask.execute();
        }
    }

    private class UpdateTopStreamsAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {

            List<Channel> channelList;

            // Try a few times, silently retrying if it fails
            for (int errorCount = 0; errorCount < MAX_ALLOWED_ERROR_COUNT; errorCount++) {
                if (isCancelled())
                    return null;
                channelList = tryGetTopStreams();
                if (channelList != null)
                    return channelList;
                SystemClock.sleep(1000);
            }

            // Try one last time, and raise errors if it fails
            return tryGetTopStreams();
        }

        private List<Channel> tryGetTopStreams() {
            try {
                return NetworkUtils.getTopStreams();
            } catch (NetworkUtils.NetworkException e) {
                Log.e(LOG_TAG_ERROR, "tryGetTopStreams has caught NetworkException");
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {
            updateTopStreamsAsyncTask = null;

            if (!isAdded() || isCancelled()) {
                return;
            }

            if (channelList == null) {
                Toast.makeText(context, "A network error has occurred", Toast.LENGTH_LONG).show();
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Resources resources = recyclerView.getResources();
            String vodcastSetting = preferences.getString(resources.getString(R.string.pref_vodcast_key), "");

            // Reset adapter if it exists, else create a new one
            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, vodcastSetting, false);
            } else {
                channelAdapter.clear();
                channelAdapter.addAll(channelList);
                channelAdapter.updateVodcastSetting(vodcastSetting);
            }

            recyclerView.setAdapter(channelAdapter);

            // Disable the refreshing animation
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}

