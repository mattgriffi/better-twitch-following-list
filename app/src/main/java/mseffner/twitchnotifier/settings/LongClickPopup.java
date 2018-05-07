package mseffner.twitchnotifier.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupWindow;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.ListChangeMadeEvent;

public class LongClickPopup extends PopupWindow implements PopupWindow.OnDismissListener {

    private CheckBox channelPinCheckBox;
    private CheckBox gameFavoriteCheckBox;
    private ListEntry listEntry;
    private boolean channelPinned;
    private boolean gameFavorited;

    static public LongClickPopup getPopup(Context context, ListEntry listEntry) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.popup_long_click, null);
        return new LongClickPopup(view, listEntry);
    }

    private LongClickPopup(View view, ListEntry listEntry) {
        // Width and height must be specified for popup to be visible on older API versions
        super(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        setOnDismissListener(this);
        // Background must be set for popup to be dismissable on older API versions
        setBackgroundDrawable(new ColorDrawable());

        Resources res = view.getContext().getResources();
        channelPinCheckBox = view.findViewById(R.id.channel_pin);
        gameFavoriteCheckBox = view.findViewById(R.id.game_favorite);
        channelPinCheckBox.setText(res.getString(R.string.pin_channel, listEntry.displayName));
        gameFavoriteCheckBox.setText(res.getString(R.string.favorite_game, listEntry.gameName));

        this.listEntry = listEntry;
        channelPinned = listEntry.pinned;
        gameFavorited = listEntry.gameFavorited;
        channelPinCheckBox.setChecked(channelPinned);
        gameFavoriteCheckBox.setChecked(gameFavorited);

        if ("".equals(listEntry.gameName))
            gameFavoriteCheckBox.setVisibility(View.GONE);
        else
            gameFavoriteCheckBox.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDismiss() {
        boolean pinned = channelPinCheckBox.isChecked();
        boolean favorited = gameFavoriteCheckBox.isChecked();
        ThreadManager.post(() -> {
            boolean changeMade = false;
            if (pinned ^ channelPinned) {
                Database.toggleChannelPin(listEntry.id, pinned);
                changeMade = true;
            }
            if (favorited ^ gameFavorited) {
                Database.toggleGameFavorite(listEntry.gameName, favorited);
                changeMade = true;
            }
            if (changeMade)
                EventBus.getDefault().post(new ListChangeMadeEvent());
        });
    }
}
