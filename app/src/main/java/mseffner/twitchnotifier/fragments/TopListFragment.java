package mseffner.twitchnotifier.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.networking.NetworkUtils;
import mseffner.twitchnotifier.settings.SettingsManager;

public class TopListFragment extends BaseListFragment {

    private static final String LOG_TAG_ERROR = "Error";
    private static final int MAX_ALLOWED_ERROR_COUNT = 3;
    private static final int NUM_TOP_STREAMS = 25;

    private ChannelAdapter channelAdapter;

    private UpdateTopStreamsAsyncTask updateTopStreamsAsyncTask;

    @Override
    protected void refreshList() {
        runUpdateTopStreamsAsyncTask();
    }

    @Override
    public void onStart() {
        super.onStart();
        startMessage.setVisibility(View.GONE);
        runUpdateTopStreamsAsyncTask();
    }

    @Override
    protected void cancelAsyncTasks() {
        if (updateTopStreamsAsyncTask != null)
            updateTopStreamsAsyncTask.cancel(true);
        updateTopStreamsAsyncTask = null;
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
                // Use application context to get default toast style
                Toast.makeText(context.getApplicationContext(), "A network error has occurred", Toast.LENGTH_LONG).show();
            }

            int rerunSetting = SettingsManager.getRerunSetting();

            // If vodcasts are set to be shown as offline, remove them from the top list entirely
            if (rerunSetting == SettingsManager.RERUN_OFFLINE) {
                channelList = removeNonliveChannels(channelList);
            }

            // Limit list size to NUM_TOP_STREAMS
            if (channelList != null && channelList.size() > NUM_TOP_STREAMS) {
                channelList = channelList.subList(0, NUM_TOP_STREAMS);
            }

            // Reset adapter if it exists, else create a new one
            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList, rerunSetting, false);
            } else {
                channelAdapter.clear();
                channelAdapter.addAll(channelList);
                channelAdapter.updateVodcastSetting(rerunSetting);
            }

            // Update recycler view while saving scroll position
            Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            recyclerView.setAdapter(channelAdapter);
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

            // Disable the refreshing animation
            swipeRefreshLayout.setRefreshing(false);
        }

        private List<Channel> removeNonliveChannels(List<Channel> list) {

            if (list == null)
                return null;

            List<Channel> newList = new ArrayList<>();
            for (Channel channel : list) {
                if (channel.getStream().getStreamType() == ChannelContract.ChannelEntry.STREAM_TYPE_LIVE) {
                    newList.add(channel);
                }
            }
            return newList;
        }
    }
}

