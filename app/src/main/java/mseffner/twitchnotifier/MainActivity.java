package mseffner.twitchnotifier;

import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.NetworkUtils;


public class MainActivity extends AppCompatActivity {

    private RecyclerView followingList;
    private static final String USER_NAME = "holokraft";
    private ChannelAdapter channelAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        followingList = (RecyclerView) findViewById(R.id.following_list);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        swipeRefreshLayout.setRefreshing(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        followingList.setLayoutManager(layoutManager);
        followingList.setHasFixedSize(true);

        swipeRefreshLayout.setColorSchemeColors(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new UpdateStreamsAsyncTask().execute();
            }
        });

        new UpdateAdapterAsyncTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_following_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        ChannelDb database = new ChannelDb(this);

        switch (item.getItemId()) {

            case R.id.action_empty_database:
                database.deleteAllChannels();
                new UpdateAdapterAsyncTask().execute();
                return true;

            case R.id.action_update_stream_data:
                new UpdateStreamsAsyncTask().execute();
                return true;

            case R.id.action_user_follow:
                // TODO add way for user to input their twitch name
                new ChangeUserAsyncTask().execute(USER_NAME);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class UpdateAdapterAsyncTask extends AsyncTask<Void, Void, List<Channel>> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected List<Channel> doInBackground(Void... voids) {

            ChannelDb database = new ChannelDb(getApplicationContext());
            return database.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {

            swipeRefreshLayout.setRefreshing(false);

            if (channelAdapter == null) {
                channelAdapter = new ChannelAdapter(channelList);
                followingList.setAdapter(channelAdapter);
            } else {
                channelAdapter.clear();
                channelAdapter.addAll(channelList);
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private class UpdateStreamsAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ChannelDb database = new ChannelDb(getApplicationContext());
            database.resetAllStreamData();
            NetworkUtils.updateStreamData(database);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new UpdateAdapterAsyncTask().execute();
        }
    }

    private class ChangeUserAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(String... strings) {
            NetworkUtils.populateUserFollowedChannels(strings[0], new ChannelDb(getApplicationContext()));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new UpdateStreamsAsyncTask().execute();
            new UpdateAdapterAsyncTask().execute();
        }
    }
}
