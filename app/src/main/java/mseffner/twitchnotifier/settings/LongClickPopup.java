package mseffner.twitchnotifier.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
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

    public LongClickPopup(Context context, ListEntry listEntry) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.popup_long_click, null);
        setContentView(view);
        setFocusable(true);
        setOnDismissListener(this);

        channelPinCheckBox = view.findViewById(R.id.channel_pin);
        gameFavoriteCheckBox = view.findViewById(R.id.game_favorite);

        this.listEntry = listEntry;
        channelPinned = listEntry.pinned;
        gameFavorited = listEntry.gameFavorited;
        channelPinCheckBox.setChecked(channelPinned);
        gameFavoriteCheckBox.setChecked(gameFavorited);
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
