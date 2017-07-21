package mseffner.twitchnotifier;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import java.util.List;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.networking.NetworkUtils;


public class MainActivity extends AppCompatActivity {

    private RecyclerView followingList;
    private static final String[] CHANNEL_NAMES = {"cirno_tv", "dansgaming", "spamfish", "bobross",
    "b0aty", "admiralbahroo", "firedragon", "chessnetwork", "northernlion", "bisnap", "pgl"};
    private static final String USER_NAME = "holokraft";
    private ChannelAdapter channelAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        followingList = (RecyclerView) findViewById(R.id.following_list);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        progressBar.setVisibility(View.VISIBLE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        followingList.setLayoutManager(layoutManager);
        followingList.setHasFixedSize(true);

        new TestAsyncTask().execute(USER_NAME);
    }

    private class TestAsyncTask extends AsyncTask<String, Void, List<Channel>> {
        @Override
        protected List<Channel> doInBackground(String... channelNames) {


            ChannelDb database = new ChannelDb(getApplicationContext());
            NetworkUtils.getUserFollowChannels(channelNames[0], database);
            return database.getAllChannels();
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {
            progressBar.setVisibility(View.INVISIBLE);
            followingList.setAdapter(new ChannelAdapter(channelList));
        }
    }
}
