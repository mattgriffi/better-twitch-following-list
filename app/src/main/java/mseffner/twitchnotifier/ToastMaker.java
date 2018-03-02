package mseffner.twitchnotifier;


import android.content.Context;
import android.widget.Toast;

/**
 * This class makes it easier to display Toast messages.
 */
public class ToastMaker {

    public static final String MESSAGE_INVALID_USERNAME = "Invalid username";
    public static final String MESSAGE_NETWORK_ERROR = "A network error has occurred";

    private static Context appContext;

    public static void initialize(Context context) {
        // It must be application context in order to use default Toast style
        appContext = context.getApplicationContext();
    }

    public static void destroy() {
        appContext = null;
    }

    public static void makeToastLong(String message) {
        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
    }

    public static void makeToastShort(String message) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
    }
}
