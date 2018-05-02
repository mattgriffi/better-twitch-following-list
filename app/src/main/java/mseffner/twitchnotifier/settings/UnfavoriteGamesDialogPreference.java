package mseffner.twitchnotifier.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ThreadManager;

public class UnfavoriteGamesDialogPreference extends DialogPreference {

    public UnfavoriteGamesDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            ThreadManager.post(Database::removeAllFavorites);
        }
    }
}
