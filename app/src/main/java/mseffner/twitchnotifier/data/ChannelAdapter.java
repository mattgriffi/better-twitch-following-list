package mseffner.twitchnotifier.data;

import android.annotation.SuppressLint;
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

    private final List<ListEntry> channelList;
    private int rerunSetting;
    private Boolean allowLongClick;

    public ChannelAdapter(List<ListEntry> channelList, int rerunSetting, Boolean allowLongClick) {
        this.channelList = channelList;
        this.rerunSetting = rerunSetting;
        this.allowLongClick = allowLongClick;
    }

    public void updateVodcastSetting(int newSetting) {
        rerunSetting = newSetting;
    }

    public void clear() {
        if (channelList != null) {
            channelList.clear();
            notifyDataSetChanged();
        }
    }

    public void addAll(List<ListEntry> list) {
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

            final ListEntry listEntry = channelList.get(index);

            // Set the channel name and pin icon
            channelName.setText(listEntry.displayName);
            if (listEntry.pinned) {
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
                    .load(listEntry.profileImageUrl)
                    .placeholder(R.drawable.default_logo_300x300)
                    .into(channelLogo);

            // LongClickListener to toggle pin (does not apply to top streams list)
            if (allowLongClick) {
                itemView.setOnLongClickListener(view -> {
                    ChannelDb.toggleChannelPin(listEntry.id);
                    listEntry.togglePinned();
                    updatePinIcon(listEntry, pinIcon);
                    return true;
                });
            }

            // Determine whether to treat stream as online or offline, and finish binding there
            if (listEntry.type == ChannelContract.StreamEntry.STREAM_TYPE_OFFLINE ||
                    (listEntry.type == ChannelContract.ChannelEntry.STREAM_TYPE_RERUN &&
                    rerunSetting == SettingsManager.RERUN_OFFLINE)) {
                bindOfflineStream();
            } else {
                bindOnlineStream(listEntry);
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

        @SuppressLint("SetTextI18n")
        private void bindOnlineStream(final ListEntry listEntry) {

            // OnClickListener to open the stream
            itemView.setOnClickListener(view -> {
                Uri channelPage = Uri.parse(listEntry.streamUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, channelPage);
                // Start intent to open stream
                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                    view.getContext().startActivity(intent);
                } else {
                    Log.e(LOG_TAG, "No app can handle intent");
                }
            });

            offlineText.setVisibility(View.INVISIBLE);

            currentGame.setVisibility(View.VISIBLE);
            streamTitle.setVisibility(View.VISIBLE);
            streamInfo.setVisibility(View.VISIBLE);

            currentGame.setText(listEntry.gameName);
            streamTitle.setText(listEntry.title);
            viewerCount.setText(Integer.toString(listEntry.viewerCount));
            uptime.setText(getUptime(listEntry.startedAt));

            // Display vodcast tag depending on setting
            if (listEntry.type == ChannelContract.ChannelEntry.STREAM_TYPE_RERUN &&
                    rerunSetting == SettingsManager.RERUN_ONLINE_TAG) {
                vodcastTag.setVisibility(View.VISIBLE);
            } else {
                vodcastTag.setVisibility(View.INVISIBLE);
            }
        }

        private void updatePinIcon(ListEntry listEntry, View pinIconView) {
            // This is used to visually toggle the pin icon without refreshing the whole list
            if (listEntry.pinned) {
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
