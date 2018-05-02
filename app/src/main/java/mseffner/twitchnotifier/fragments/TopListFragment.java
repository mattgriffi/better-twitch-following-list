package mseffner.twitchnotifier.fragments;

import android.support.annotation.NonNull;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.ListModeChangedEvent;
import mseffner.twitchnotifier.events.TopListRefreshedEvent;
import mseffner.twitchnotifier.settings.SettingsManager;

public class TopListFragment extends BaseListFragment {

    private static final int NUM_TOP_STREAMS = 25;

    @Override
    public void onStart() {
        super.onStart();
        startMessage.setVisibility(View.GONE);
        updateList();
    }

    @Override
    protected boolean getLongClickSetting() {
        return false;
    }

    @Override
    protected void updateList() {
        ThreadManager.post(() -> {
            List<ListEntry> list = Database.getTopStreams();
            // If reruns are set to be shown as offline, remove them from the top list entirely
            if (SettingsManager.getRerunSetting() == SettingsManager.RERUN_OFFLINE)
                list = removeNonliveChannels(list);
            // Limit list size to NUM_TOP_STREAMS
            if (list.size() > NUM_TOP_STREAMS)
                list.subList(NUM_TOP_STREAMS, list.size()).clear();
            EventBus.getDefault().post(new TopListRefreshedEvent(list));
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTopListRefreshedEvent(TopListRefreshedEvent event) {
        updateAdapter(event.list);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCompactModeChangedEvent(ListModeChangedEvent event) {
        // Force recyclerView to redraw with new layout
        recyclerView.setAdapter(channelAdapter);
    }

    private List<ListEntry> removeNonliveChannels(@NonNull List<ListEntry> list) {
        List<ListEntry> newList = new ArrayList<>();
        for (ListEntry listEntry : list)
            if (listEntry.type == ChannelContract.StreamEntry.STREAM_TYPE_LIVE)
                newList.add(listEntry);
        return newList;
    }
}

