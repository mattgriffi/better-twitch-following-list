package mseffner.twitchnotifier;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import mseffner.twitchnotifier.data.Channel;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.networking.NetworkUtils;


public class MainActivity extends AppCompatActivity {

    private RecyclerView followingList;
    private static final String[] CHANNEL_NAMES = {"cirno_tv", "dansgaming", "spamfish", "bobross",
    "b0aty", "admiralbahroo", "firedragon", "chessnetwork", "northernlion", "bisnap", };
    private ChannelAdapter channelAdapter;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        followingList = (RecyclerView) findViewById(R.id.following_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        followingList.setLayoutManager(layoutManager);
        followingList.setHasFixedSize(true);

        new TestAsyncTask().execute(CHANNEL_NAMES);
    }

    private class TestAsyncTask extends AsyncTask<String, Void, List<Channel>> {
        @Override
        protected List<Channel> doInBackground(String... channelNames) {

            return NetworkUtils.getChannels(channelNames);
        }

        @Override
        protected void onPostExecute(List<Channel> channelList) {
            followingList.setAdapter(new ChannelAdapter(channelList));
        }
    }

}
