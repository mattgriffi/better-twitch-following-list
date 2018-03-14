package mseffner.twitchnotifier.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.data.ListEntrySorter;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.FollowsUpdateStartedEvent;
import mseffner.twitchnotifier.events.FollowsUpdatedEvent;
import mseffner.twitchnotifier.events.ListRefreshedEvent;
import mseffner.twitchnotifier.events.StreamsUpdateStartedEvent;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;
import mseffner.twitchnotifier.networking.ErrorHandler;

public class FollowingListFragment extends BaseListFragment {

    private static long start;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        start = System.nanoTime();
        return view;
    }

    @Override
    protected void refreshList() {
        updateList();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        updateList();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected boolean getLongClickSetting() {
        return true;
    }

    @Override
    protected void cancelAsyncTasks() {}

    private void updateList() {
        ThreadManager.post(() -> {
            List<ListEntry> list = ChannelDb.getAllFollows();
            ListEntrySorter.sort(list);
            Log.e("list size", "" + list.size());
            EventBus.getDefault().post(new ListRefreshedEvent(list));
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFollowsUpdateStartedEvent(FollowsUpdateStartedEvent event) {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamsUpdateStartedEvent(StreamsUpdateStartedEvent event) {
        swipeRefreshLayout.setRefreshing(true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFollowsUpdatedEvent(FollowsUpdatedEvent event) {
        Log.e("TEST", "Follows update time: " + (System.nanoTime() - start) / 1000000);
        DataUpdateManager.updateStreamsData(new ErrorHandler() {});
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamsUpdatedEvent(StreamsUpdatedEvent event) {
        Log.e("TEST", "Total update time: "  + (System.nanoTime() - start) / 1000000);
        updateList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onListRefreshedEvent(ListRefreshedEvent event) {
        updateAdapter(event.list);
        // Show startMessage if adapter is empty, else hide it
        if (channelAdapter.getItemCount() == 0)
            startMessage.setVisibility(View.VISIBLE);
        else
            startMessage.setVisibility(View.GONE);

        if (!DataUpdateManager.updateInProgress())
            swipeRefreshLayout.setRefreshing(false);
    }
}
