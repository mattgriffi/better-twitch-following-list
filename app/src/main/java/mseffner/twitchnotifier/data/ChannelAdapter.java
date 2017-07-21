package mseffner.twitchnotifier.data;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import mseffner.twitchnotifier.R;


public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private final List<Channel> channelList;

    public ChannelAdapter(List<Channel> channelList) {
        this.channelList = channelList;
        Collections.sort(this.channelList);
        Collections.reverse(this.channelList);
    }

    @Override
    public ChannelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.follower_list_item, parent, false);

        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChannelViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    class ChannelViewHolder extends RecyclerView.ViewHolder {

        private View itemView;

        private ImageView channelLogo;
        private TextView channelName;
        private TextView currentGame;
        private TextView offlineText;
        private TextView streamTitle;
        private LinearLayout streamInfo;
        private TextView viewerCount;
        private TextView uptime;
        private TextView vodcastTag;

        ChannelViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            channelLogo = itemView.findViewById(R.id.channel_logo);
            channelName = itemView.findViewById(R.id.channel_name);
            currentGame = itemView.findViewById(R.id.current_game);
            offlineText = itemView.findViewById(R.id.offline_text);
            streamTitle = itemView.findViewById(R.id.stream_title);
            streamInfo = itemView.findViewById(R.id.live_stream_info);
            vodcastTag = itemView.findViewById(R.id.vodcast_tag);
            viewerCount = itemView.findViewById(R.id.viewer_count);
            uptime = itemView.findViewById(R.id.uptime);
        }

        void bind(int index) {

            Channel channel = channelList.get(index);

            channelName.setText(channel.getDisplayName());
            channelLogo.setImageBitmap(channel.getLogoBmp());

            if (channel.getStream() == null) {
                bindOfflineStream();
            } else {
                bindOnlineStream(channel);
            }
        }

        private void bindOfflineStream() {

            offlineText.setVisibility(View.VISIBLE);

            currentGame.setVisibility(View.INVISIBLE);
            streamInfo.setVisibility(View.INVISIBLE);
            streamTitle.setVisibility(View.INVISIBLE);
            vodcastTag.setVisibility(View.INVISIBLE);
        }

        private void bindOnlineStream(Channel channel) {
            Stream stream = channel.getStream();

            offlineText.setVisibility(View.INVISIBLE);
            currentGame.setVisibility(View.VISIBLE);
            streamTitle.setVisibility(View.VISIBLE);
            streamInfo.setVisibility(View.VISIBLE);

            currentGame.setText(stream.getCurrentGame());
            streamTitle.setText(stream.getStatus());
            viewerCount.setText(Integer.toString(stream.getCurrentViewers()));

            uptime.setText(getUptime(stream.getCreatedAt()));

            if (stream.getStreamType() == ChannelContract.ChannelEntry.STREAM_TYPE_VODCAST)
                vodcastTag.setVisibility(View.VISIBLE);
        }

        private String getUptime(long createdAt) {
            long currentTime = System.currentTimeMillis() / 1000;
            int uptimeInSeconds = (int) (currentTime - createdAt);

            int hours = uptimeInSeconds / 3600;
            uptimeInSeconds %= 3600;
            int minutes = uptimeInSeconds / 60;

            if (hours < 100)  // Marathons can sometimes go over 99 hours straight
                return String.format("%02d:%02d", hours, minutes);
            return String.format("%d:%02d", hours, minutes);

        }
    }
}
