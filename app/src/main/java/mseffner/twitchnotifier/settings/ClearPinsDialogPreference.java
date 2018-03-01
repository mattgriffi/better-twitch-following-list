package mseffner.twitchnotifier.settings;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import mseffner.twitchnotifier.data.ChannelDb;

public class ClearPinsDialogPreference extends DialogPreference {

    public ClearPinsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // This isn't actually a "preference", so it should not save anything
        setPersistent(false);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            new DeletePinsAsyncTask().execute();
        }
    }

    private class DeletePinsAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... strings) {
            ChannelDb.removeAllPins();
            return null;
        }
    }
}
