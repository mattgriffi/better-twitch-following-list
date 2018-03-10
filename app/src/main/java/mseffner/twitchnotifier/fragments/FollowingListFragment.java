package mseffner.twitchnotifier.fragments;

import android.os.AsyncTask;
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
import mseffner.twitchnotifier.events.FollowsUpdatedEvent;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;
import mseffner.twitchnotifier.networking.ErrorHandler;

public class FollowingListFragment extends BaseListFragment {

    private UpdateAdapterAsyncTask updateAdapterAsyncTask;

    private static long start;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        start = System.nanoTime();
        DataUpdateManager.updateFollowsData(new ErrorHandler() {});
        return view;
    }

    @Override
    protected void refreshList() {
        DataUpdateManager.updateStreamsData(new ErrorHandler() {});
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        runUpdateAdapterAsyncTask();
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
    protected void cancelAsyncTasks() {
        if (updateAdapterAsyncTask != null)
            updateAdapterAsyncTask.cancel(true);
        updateAdapterAsyncTask = null;
    }

    private void runUpdateAdapterAsyncTask() {
        if (updateAdapterAsyncTask == null) {
            updateAdapterAsyncTask = new UpdateAdapterAsyncTask();
            updateAdapterAsyncTask.execute();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFollowsUpdatedEvent(FollowsUpdatedEvent event) {
        Log.e("TEST", "Follows update time: " + (System.nanoTime() - start) / 1000000);
        DataUpdateManager.updateStreamsData(new ErrorHandler() {});
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStreamsUpdatedEvent(StreamsUpdatedEvent event) {
        Log.e("TEST", "Total update time: "  + (System.nanoTime() - start) / 1000000);
        runUpdateAdapterAsyncTask();
    }

    private class UpdateAdapterAsyncTask extends AsyncTask<Void, Void, List<ListEntry>> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<ListEntry> doInBackground(Void... voids) {
            List<ListEntry> list = ChannelDb.getAllChannels();
            ListEntrySorter.sort(list);
            Log.e("list size", "" + list.size());
            return list;
        }

        @Override
        protected void onPostExecute(List<ListEntry> channelList) {

            if (!isAdded() || isCancelled())
                return;

            updateAdapter(channelList);

            // Show startMessage if adapter is empty, else hide it
            if (channelAdapter.getItemCount() == 0)
                startMessage.setVisibility(View.VISIBLE);
            else
                startMessage.setVisibility(View.GONE);

            // Disable the refreshing animation if other tasks are not running
            swipeRefreshLayout.setRefreshing(false);

            updateAdapterAsyncTask = null;
        }
    }
}
