package mseffner.twitchnotifier;


import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

/**
 * This class makes it easier to display Toast messages.
 */
public class ToastMaker {

    public static String MESSAGE_INVALID_USERNAME;
    public static String MESSAGE_NETWORK_ERROR;
    public static String MESSAGE_SERVER_ERROR;
    public static String MESSAGE_TOO_MANY_FOLLOWS;
    public static String MESSAGE_RATE_LIMIT;
    public static String MESSAGE_USERNAME_CHANGE;

    private static Context appContext;

    public static void initialize(Context context) {
        // It must be application context in order to use default Toast style
        appContext = context.getApplicationContext();
        Resources res = appContext.getResources();
        MESSAGE_INVALID_USERNAME = res.getString(R.string.message_invalid_username);
        MESSAGE_NETWORK_ERROR = res.getString(R.string.message_network_error);
        MESSAGE_SERVER_ERROR = res.getString(R.string.message_server_error);
        MESSAGE_TOO_MANY_FOLLOWS = res.getString(R.string.message_too_many_follows);
        MESSAGE_RATE_LIMIT = res.getString(R.string.message_rate_limit);
        MESSAGE_USERNAME_CHANGE = res.getString(R.string.message_username_change);
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
