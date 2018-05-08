package mseffner.twitchnotifier.adapters;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ChannelContract;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.settings.LongClickPopup;
import mseffner.twitchnotifier.settings.SettingsManager;


public class MinimalViewHolder extends RecyclerView.ViewHolder {

    private Vibrator vibrator;
    private View itemView;

    private Drawable darkPinnedBackground = new ColorDrawable(Color.argb(125, 75, 54, 124));
    private Drawable lightPinnedBackground = new ColorDrawable(Color.argb(50, 177, 157, 216));
    private Drawable transparentBackground = new ColorDrawable(Color.TRANSPARENT);

    protected TextView channelName;
    protected TextView currentGame;
    protected TextView offlineText;
    protected TextView rerunTag;
    protected ImageView pinIcon;

    public MinimalViewHolder(View itemView, Vibrator vibrator) {
        super(itemView);
        this.itemView = itemView;
        channelName = itemView.findViewById(R.id.display_name);
        currentGame = itemView.findViewById(R.id.game_name);
        offlineText = itemView.findViewById(R.id.offline_text);
        rerunTag = itemView.findViewById(R.id.rerun_tag);
        pinIcon = itemView.findViewById(R.id.pin_icon);
        this.vibrator = vibrator;
    }

    public void bind(ListEntry listEntry, boolean allowLongClick, int vibrateTime) {

        // Set the display name
        channelName.setText(listEntry.displayName);

        // Set the pin
        if (allowLongClick) {
            setPinDisplay(listEntry);
            setGameDisplay(listEntry);
        } else
            pinIcon.setVisibility(View.GONE);

        // LongClickListener to toggle pin (does not apply to top streams list)
        if (allowLongClick)
            itemView.setOnLongClickListener(view -> {
                if (vibrator != null)
                    vibrator.vibrate(vibrateTime);
                PopupWindow popupWindow = LongClickPopup.getPopup(itemView.getContext(), listEntry);
                popupWindow.showAtLocation(itemView, Gravity.CENTER, 0, 0);
                return true;
            });

        // Determine whether to treat stream as online or offline, and finish binding there
        boolean offline = listEntry.type == ChannelContract.StreamEntry.STREAM_TYPE_OFFLINE;
        boolean rerun = listEntry.type == ChannelContract.StreamEntry.STREAM_TYPE_RERUN;
        boolean rerunOffline = SettingsManager.getRerunSetting() == SettingsManager.RERUN_OFFLINE;
        if (offline || (rerun && rerunOffline))
            bindOfflineStream();
        else
            bindOnlineStream(listEntry);
    }

    private void setPinDisplay(ListEntry listEntry) {
        if (listEntry.pinned) {
            pinIcon.setVisibility(View.VISIBLE);
            if (SettingsManager.getDarkModeSetting())
                ViewCompat.setBackground(itemView, darkPinnedBackground);
            else
                ViewCompat.setBackground(itemView, lightPinnedBackground);
        } else {
            pinIcon.setVisibility(View.INVISIBLE);
            ViewCompat.setBackground(itemView, transparentBackground);
        }
    }

    private void setGameDisplay(ListEntry listEntry) {
        if (listEntry.gameFavorited)
            currentGame.setPaintFlags(currentGame.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        else
            currentGame.setPaintFlags(currentGame.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
    }

    protected void bindOfflineStream() {
        offlineText.setVisibility(View.VISIBLE);
        currentGame.setText("");
        rerunTag.setVisibility(View.GONE);
        itemView.setOnClickListener(null);
    }

    protected void bindOnlineStream(final ListEntry listEntry) {

        // OnClickListener to open the stream
        itemView.setOnClickListener(view -> {
            Uri channelPage = Uri.parse(listEntry.streamUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, channelPage);
            // Start intent to open stream
            if (intent.resolveActivity(view.getContext().getPackageManager()) != null)
                view.getContext().startActivity(intent);
        });

        offlineText.setVisibility(View.GONE);
        currentGame.setText(listEntry.gameName);

        // Display vodcast tag depending on setting
        boolean rerun = listEntry.type == ChannelContract.StreamEntry.STREAM_TYPE_RERUN;
        boolean rerunTagged = SettingsManager.getRerunSetting() == SettingsManager.RERUN_ONLINE_TAG;
        if (rerun && rerunTagged)
            rerunTag.setVisibility(View.VISIBLE);
        else
            rerunTag.setVisibility(View.GONE);
    }
}
