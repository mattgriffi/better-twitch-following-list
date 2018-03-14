package mseffner.twitchnotifier.adapters;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.settings.SettingsManager;


public class CompactViewHolder extends RecyclerView.ViewHolder {

    protected Vibrator vibrator;
    protected View itemView;

    protected TextView channelName;
    protected TextView currentGame;
    protected TextView offlineText;
    protected LinearLayout streamInfo;
    protected TextView viewerCount;
    protected TextView uptime;
    protected TextView vodcastTag;
    protected ImageView pinIcon;

    public CompactViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        channelName = itemView.findViewById(R.id.display_name);
        currentGame = itemView.findViewById(R.id.game_name);
        offlineText = itemView.findViewById(R.id.offline_text);
        streamInfo = itemView.findViewById(R.id.live_stream_info);
        vodcastTag = itemView.findViewById(R.id.rerun_tag);
        viewerCount = itemView.findViewById(R.id.viewer_count);
        uptime = itemView.findViewById(R.id.uptime);
        pinIcon = itemView.findViewById(R.id.pin_icon);
        vibrator = (Vibrator) itemView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void bind(ListEntry listEntry, boolean allowLongClick, int vibrateTime) {

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

        // LongClickListener to toggle pin (does not apply to top streams list)
        if (allowLongClick) {
            itemView.setOnLongClickListener(view -> {
                if (vibrator != null)
                    vibrator.vibrate(vibrateTime);
                ChannelDb.toggleChannelPin(listEntry.id);
                listEntry.togglePinned();
                updatePinIcon(listEntry, pinIcon);
                return true;
            });
        }

        // Determine whether to treat stream as online or offline, and finish binding there
        if (listEntry.type == ChannelContract.StreamEntry.STREAM_TYPE_OFFLINE ||
                (listEntry.type == ChannelContract.ChannelEntry.STREAM_TYPE_RERUN &&
                        SettingsManager.getRerunSetting() == SettingsManager.RERUN_OFFLINE)) {
            bindOfflineStream();
        } else {
            bindOnlineStream(listEntry);
        }
    }

    protected void bindOfflineStream() {

        offlineText.setVisibility(View.VISIBLE);

        currentGame.setVisibility(View.INVISIBLE);
        streamInfo.setVisibility(View.INVISIBLE);
        vodcastTag.setVisibility(View.INVISIBLE);

        itemView.setOnClickListener(null);
    }

    @SuppressLint("SetTextI18n")
    protected void bindOnlineStream(final ListEntry listEntry) {

        // OnClickListener to open the stream
        itemView.setOnClickListener(view -> {
            Uri channelPage = Uri.parse(listEntry.streamUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, channelPage);
            // Start intent to open stream
            if (intent.resolveActivity(view.getContext().getPackageManager()) != null)
                view.getContext().startActivity(intent);
        });

        offlineText.setVisibility(View.INVISIBLE);

        currentGame.setVisibility(View.VISIBLE);
        streamInfo.setVisibility(View.VISIBLE);

        currentGame.setText(listEntry.gameName);
        viewerCount.setText(Integer.toString(listEntry.viewerCount));
        uptime.setText(getUptime(listEntry.startedAt));

        // Display vodcast tag depending on setting
        if (listEntry.type == ChannelContract.ChannelEntry.STREAM_TYPE_RERUN &&
                SettingsManager.getRerunSetting() == SettingsManager.RERUN_ONLINE_TAG) {
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
