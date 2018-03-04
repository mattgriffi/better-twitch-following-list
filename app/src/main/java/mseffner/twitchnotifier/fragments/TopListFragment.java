package mseffner.twitchnotifier.fragments;

import android.support.annotation.NonNull;
import android.view.View;

import com.android.volley.ServerError;
import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.networking.ErrorHandler;
import mseffner.twitchnotifier.settings.SettingsManager;

public class TopListFragment extends BaseListFragment implements DataUpdateManager.TopStreamsListener {

    private static final String TAG = TopListFragment.class.getSimpleName();
    private static final int NUM_TOP_STREAMS = 25;

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
    protected boolean getLongClickSetting() {
        return false;
    }

    @Override
    protected void cancelAsyncTasks() {}

    @Override
    public void onTopStreamsResponse(@NonNull List<Channel> channels) {
        // If vodcasts are set to be shown as offline, remove them from the top list entirely
        if (SettingsManager.getRerunSetting() == SettingsManager.RERUN_OFFLINE)
            channels = removeNonliveChannels(channels);

        // Limit list size to NUM_TOP_STREAMS
        if (channels.size() > NUM_TOP_STREAMS)
            channels = channels.subList(0, NUM_TOP_STREAMS);

        updateAdapter(channels);
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

