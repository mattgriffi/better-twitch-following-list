package mseffner.twitchnotifier.data;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.settings.SettingsManager;


public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private final List<Channel> channelList;
    private int vodcastSetting;
    private Boolean allowLongClick;

    public ChannelAdapter(List<Channel> channelList, int vodcastSetting, Boolean allowLongClick) {
        this.channelList = channelList;
        this.vodcastSetting = vodcastSetting;
        this.allowLongClick = allowLongClick;
    }

    public void updateVodcastSetting(int newSetting) {
        vodcastSetting = newSetting;
    }

    public void clear() {
        if (channelList != null) {
            channelList.clear();
            notifyDataSetChanged();
        }
    }

    public void addAll(List<Channel> list) {
        if (channelList != null && list != null) {
            channelList.addAll(list);
            notifyDataSetChanged();
        }
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
        if (channelList != null) {
            return channelList.size();
        } else {
            return 0;
        }
    }

    class ChannelViewHolder extends RecyclerView.ViewHolder {

        private final String LOG_TAG = this.getClass().getSimpleName();

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
        private ImageView pinIcon;

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
            pinIcon = itemView.findViewById(R.id.pin_icon);
        }

        void bind(int index) {

            final Channel channel = channelList.get(index);

            // Set the channel name and pin icon
            channelName.setText(channel.getDisplayName());
            if (channel.getPinned() == ChannelContract.ChannelEntry.IS_PINNED) {
                pinIcon.setVisibility(View.VISIBLE);
            } else if (!allowLongClick){
                // If pins are disabled, remove the pin icon space
                pinIcon.setVisibility(View.GONE);
            } else {
                // If pins are enabled, leave the pin icon space
                pinIcon.setVisibility(View.INVISIBLE);
            }

            // Set up Picasso to load the channel logo
            Picasso.with(itemView.getContext())
                    .load(channel.getLogoUrl())
                    .placeholder(R.drawable.default_logo_300x300)
                    .into(channelLogo);

            // LongClickListener to toggle pin (does not apply to top streams list)
            if (allowLongClick) {
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        ChannelDb.toggleChannelPin(channel);
                        channel.togglePinned();
                        updatePinIcon(channel, pinIcon);
                        return true;
                    }
                });
            }

            // Determine whether to treat stream as online or offline, and finish binding there
            if (channel.getStream() == null ||
                    (channel.getStream().getStreamType() == ChannelContract.ChannelEntry.STREAM_TYPE_RERUN &&
                    vodcastSetting == SettingsManager.RERUN_OFFLINE)) {
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

            itemView.setOnClickListener(null);
        }

        private void bindOnlineStream(final Channel channel) {
            Stream stream = channel.getStream();

            // OnClickListener to open the stream
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri channelPage = Uri.parse(channel.getStreamUrl());
                    Intent intent = new Intent(Intent.ACTION_VIEW, channelPage);
                    // Start intent to open stream
                    if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                        view.getContext().startActivity(intent);
                    } else {
                        Log.e(LOG_TAG, "No app can handle intent");
                    }
                }
            });

            offlineText.setVisibility(View.INVISIBLE);

            currentGame.setVisibility(View.VISIBLE);
            streamTitle.setVisibility(View.VISIBLE);
            streamInfo.setVisibility(View.VISIBLE);

            currentGame.setText(stream.getCurrentGame());
            streamTitle.setText(stream.getStatus());
            viewerCount.setText(Integer.toString(stream.getCurrentViewers()));
            uptime.setText(getUptime(stream.getCreatedAt()));

            // Display vodcast tag depending on setting
            if (stream.getStreamType() == ChannelContract.ChannelEntry.STREAM_TYPE_RERUN &&
                    vodcastSetting == SettingsManager.RERUN_ONLINE_TAG) {
                vodcastTag.setVisibility(View.VISIBLE);
            } else {
                vodcastTag.setVisibility(View.INVISIBLE);
            }
        }

        private void updatePinIcon(Channel channel, View pinIconView) {
            // This is used to visually toggle the pin icon without refreshing the whole list
            if (channel.getPinned() == ChannelContract.ChannelEntry.IS_PINNED) {
                pinIconView.setVisibility(View.VISIBLE);
            } else {
                pinIcon.setVisibility(View.INVISIBLE);
            }
        }

        private String getUptime(long createdAt) {
            // Uptime is stored as a unix timestamp; this will convert it to a more readable format
            long currentTime = System.currentTimeMillis() / 1000;
            int uptimeInSeconds = (int) (currentTime - createdAt);

            int hours = uptimeInSeconds / 3600;
            uptimeInSeconds %= 3600;
            int minutes = uptimeInSeconds / 60;

            return String.format(Locale.US,"%d:%02d", hours, minutes);

        }
    }
}
