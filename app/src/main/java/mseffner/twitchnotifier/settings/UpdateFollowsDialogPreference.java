package mseffner.twitchnotifier.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;


public class UpdateFollowsDialogPreference extends DialogPreference {

    public UpdateFollowsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            SettingsManager.setFollowsNeedUpdate(true);
        }
    }
}
