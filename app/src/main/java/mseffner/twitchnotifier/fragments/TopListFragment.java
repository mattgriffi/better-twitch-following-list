package mseffner.twitchnotifier.fragments;

import android.support.annotation.NonNull;
import android.view.View;

import com.android.volley.ServerError;
import com.android.volley.VolleyError;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.events.TopStreamsUpdatedEvent;
import mseffner.twitchnotifier.networking.ErrorHandler;
import mseffner.twitchnotifier.settings.SettingsManager;

public class TopListFragment extends BaseListFragment {

    private static final int NUM_TOP_STREAMS = 25;

    private boolean updating = false;

    @Override
    protected void refreshList() {
        if (updating) return;
        refreshStart();
        DataUpdateManager.getTopStreamsData(new TopStreamsErrorHandler());
    }

    @Override
    public void onStart() {
        super.onStart();
        startMessage.setVisibility(View.GONE);
        EventBus.getDefault().register(this);
        refreshList();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTopStreamsUpdatedEvent(TopStreamsUpdatedEvent event) {
        // If vodcasts are set to be shown as offline, remove them from the top list entirely
        if (SettingsManager.getRerunSetting() == SettingsManager.RERUN_OFFLINE)
            event.list = removeNonliveChannels(event.list);

        // Limit list size to NUM_TOP_STREAMS
        if (event.list.size() > NUM_TOP_STREAMS)
            event.list = event.list.subList(0, NUM_TOP_STREAMS);

        updateAdapter(event.list);
        refreshStop();
    }

    private List<ListEntry> removeNonliveChannels(@NonNull List<ListEntry> list) {
        List<ListEntry> newList = new ArrayList<>();
        for (ListEntry listEntry : list)
            if (listEntry.type == ChannelContract.ChannelEntry.STREAM_TYPE_LIVE)
                newList.add(listEntry);
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

