package mseffner.twitchnotifier.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ThreadManager;

public class ClearPinsDialogPreference extends DialogPreference {

    public ClearPinsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            ThreadManager.post(Database::removeAllPins);
        }
    }
}
