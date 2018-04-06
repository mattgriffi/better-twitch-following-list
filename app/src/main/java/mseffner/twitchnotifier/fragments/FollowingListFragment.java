package mseffner.twitchnotifier.fragments;

import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.data.ListEntrySorter;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.ListRefreshedEvent;
import mseffner.twitchnotifier.events.PreStreamUpdateEvent;
import mseffner.twitchnotifier.networking.UpdateCoordinator;
import mseffner.twitchnotifier.settings.SettingsManager;

public class FollowingListFragment extends BaseListFragment {

    private int onlineFollows = 0;
    private int totalFollows = 0;

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
        ThreadManager.post(() -> sortList(Database.getAllFollowsLegacy()));
    }

    private void sortList(List<ListEntry> list) {
        ThreadManager.post(() -> {
            totalFollows = list.size();
            onlineFollows =  countOnline(list);
            ListEntrySorter.sort(list);
            EventBus.getDefault().post(new ListRefreshedEvent(list));
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onListRefreshedEvent(ListRefreshedEvent event) {
        updateAdapter(event.list);
        // Show startMessage if adapter is empty, else hide it
        if (channelAdapter.getItemCount() == 0) {
            startMessage.setVisibility(View.VISIBLE);
            counterView.setVisibility(View.GONE);
        } else {
            startMessage.setVisibility(View.GONE);
            setupCounter();
        }
        if (!UpdateCoordinator.updateInProgress())
            swipeRefreshLayout.setRefreshing(false);
    }

    private void setupCounter() {
        if (SettingsManager.getCounterSetting()) {
            counterView.setVisibility(View.VISIBLE);
            String text = getString(R.string.counter_text_format, onlineFollows, totalFollows);
            counterView.setText(text);
        } else {
            counterView.setVisibility(View.GONE);
        }
    }

    private int countOnline(List<ListEntry> list) {
        int i = 0;
        for (ListEntry item : list)
            if (item.type != ChannelContract.StreamEntry.STREAM_TYPE_OFFLINE)
                i++;
        return i;
    }
}
