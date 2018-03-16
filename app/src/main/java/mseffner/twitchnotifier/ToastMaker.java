package mseffner.twitchnotifier;


import android.content.Context;
import android.widget.Toast;

/**
 * This class makes it easier to display Toast messages.
 */
public class ToastMaker {

    public static final String MESSAGE_INVALID_USERNAME = "Invalid username";
    public static final String MESSAGE_NETWORK_ERROR = "A network error has occurred";
    public static final String MESSAGE_SERVER_ERROR = "A Twitch server error has occurred";
    public static final String MESSAGE_TOO_MANY_FOLLOWS = "Cannot have more than 2500 follows due to Twitch API limits";
    public static final String MESSAGE_RATE_LIMIT = "Twitch API limit hit, data may be incomplete for now";

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
