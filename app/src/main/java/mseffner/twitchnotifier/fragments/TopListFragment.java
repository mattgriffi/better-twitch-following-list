package mseffner.twitchnotifier.fragments;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.View;

import com.android.volley.ServerError;
import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.networking.ErrorHandler;
import mseffner.twitchnotifier.settings.SettingsManager;

public class TopListFragment extends BaseListFragment implements DataUpdateManager.TopStreamsListener {

    private static final String TAG = TopListFragment.class.getSimpleName();
    private static final int NUM_TOP_STREAMS = 25;

    private ChannelAdapter channelAdapter;

    private boolean updating = false;

    @Override
    protected void refreshList() {
        if (!updating) {
            refreshStart();
            DataUpdateManager.getTopStreamsData(this, new TopStreamsErrorHandler());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startMessage.setVisibility(View.GONE);
        refreshList();
    }

    private void refreshStart() {
        swipeRefreshLayout.setRefreshing(true);
        updating = true;
    }

    private void refreshStop() {
        swipeRefreshLayout.setRefreshing(false);
        updating = false;
    }

    @Override
    protected void cancelAsyncTasks() {}

    @Override
    public void onTopStreamsResponse(@NonNull List<Channel> channels) {

        int rerunSetting = SettingsManager.getRerunSetting();

        // If vodcasts are set to be shown as offline, remove them from the top list entirely
        if (rerunSetting == SettingsManager.RERUN_OFFLINE)
            channels = removeNonliveChannels(channels);

        // Limit list size to NUM_TOP_STREAMS
        if (channels != null && channels.size() > NUM_TOP_STREAMS)
            channels = channels.subList(0, NUM_TOP_STREAMS);

        // Reset adapter if it exists, else create a new one
        if (channelAdapter == null) {
            channelAdapter = new ChannelAdapter(channels, rerunSetting, false);
        } else {
            channelAdapter.clear();
            channelAdapter.addAll(channels);
            channelAdapter.updateVodcastSetting(rerunSetting);
        }

        // Update recycler view while saving scroll position
        Parcelable recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        recyclerView.setAdapter(channelAdapter);
        recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

        // Disable the refreshing animation
        refreshStop();
    }

    private List<Channel> removeNonliveChannels(@NonNull List<Channel> list) {
        List<Channel> newList = new ArrayList<>();
        for (Channel channel : list)
            if (channel.getStream().getStreamType() == ChannelContract.ChannelEntry.STREAM_TYPE_LIVE)
                newList.add(channel);
        return newList;
    }

    private class TopStreamsErrorHandler extends ErrorHandler {
        @Override
        public void onErrorResponse(VolleyError error) {
            super.onErrorResponse(error);
            refreshStop();
        }

        @Override
        protected boolean customHandling(VolleyError error) {
            // 429 indicates rate limiting
            if (error instanceof ServerError && error.networkResponse.statusCode == 429) {
                ToastMaker.makeToastLong("Too many refreshes, please wait a little while");
                return true;
            }
            return false;
        }
    }
}

