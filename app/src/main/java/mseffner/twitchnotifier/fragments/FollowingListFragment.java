package mseffner.twitchnotifier.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.data.ListEntrySorter;
import mseffner.twitchnotifier.networking.ErrorHandler;

public class FollowingListFragment extends BaseListFragment implements DataUpdateManager.FollowsUpdateListener,
        DataUpdateManager.StreamsUpdateListener {

    private UpdateAdapterAsyncTask updateAdapterAsyncTask;

    private static long start;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        DataUpdateManager.registerFollowsUpdateListener(this);
        DataUpdateManager.registerStreamsUpdateListener(this);
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
        runUpdateAdapterAsyncTask();
    }

    @Override
    public void onStop() {
        super.onStop();
        DataUpdateManager.unregisterFollowsUpdateListener();
        DataUpdateManager.unregisterStreamsUpdateListener();
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

    @Override
    public void onFollowsDataUpdated() {
        Log.e("TEST", "Follows update time: " + (System.nanoTime() - start) / 1000000);
        DataUpdateManager.updateStreamsData(new ErrorHandler() {});
    }

    @Override
    public void onStreamsDataUpdated() {
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
