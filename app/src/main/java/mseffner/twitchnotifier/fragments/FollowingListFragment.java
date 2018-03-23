package mseffner.twitchnotifier.fragments;

import android.view.View;

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
import mseffner.twitchnotifier.events.ListRefreshedEvent;
import mseffner.twitchnotifier.events.PreStreamUpdateEvent;
import mseffner.twitchnotifier.events.StreamsUpdateStartedEvent;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;

public class FollowingListFragment extends BaseListFragment {

    @Override
    public void onStart() {
        super.onStart();
        /* If the PreStreamUpdateEvent is present, then PeriodicUpdater was just
        started and the streams data has probably been deleted, so use the list
        provided in the event. This prevents the list from showing everything as
        offline for a second before the streams update completes. */
        PreStreamUpdateEvent stickyEvent = EventBus.getDefault().getStickyEvent(PreStreamUpdateEvent.class);
        if (stickyEvent != null) {
            sortList(stickyEvent.list);
            EventBus.getDefault().removeStickyEvent(stickyEvent);
        } else {
            updateList();
        }
    }

    @Override
    protected boolean getLongClickSetting() {
        return true;
    }

    @Override
    protected void updateList() {
        ThreadManager.post(() -> sortList(ChannelDb.getAllFollows()));
    }

    private void sortList(List<ListEntry> list) {
        ThreadManager.post(() -> {
            ListEntrySorter.sort(list);
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
    public void onStreamsUpdatedEvent(StreamsUpdatedEvent event) {
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
