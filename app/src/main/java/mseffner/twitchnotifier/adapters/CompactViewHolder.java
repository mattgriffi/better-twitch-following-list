package mseffner.twitchnotifier.adapters;


import android.annotation.SuppressLint;
import android.os.Vibrator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ListEntry;


public class CompactViewHolder extends MinimalViewHolder {

    protected LinearLayout onlineInfo;
    protected TextView viewerCount;
    protected TextView uptime;

    public CompactViewHolder(View itemView, Vibrator vibrator) {
        super(itemView, vibrator);
        onlineInfo = itemView.findViewById(R.id.online_info);
        viewerCount = itemView.findViewById(R.id.viewer_count);
        uptime = itemView.findViewById(R.id.uptime);
    }

    protected void bindOfflineStream() {
        super.bindOfflineStream();
        onlineInfo.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("SetTextI18n")
    protected void bindOnlineStream(final ListEntry listEntry) {
        super.bindOnlineStream(listEntry);
        onlineInfo.setVisibility(View.VISIBLE);
        currentGame.setText(listEntry.gameName);
        viewerCount.setText(Integer.toString(listEntry.viewerCount));
        uptime.setText(getUptime(listEntry.startedAt));
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
