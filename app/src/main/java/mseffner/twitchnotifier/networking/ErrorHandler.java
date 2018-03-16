package mseffner.twitchnotifier.networking;

import android.util.Log;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.ToastMaker;
import mseffner.twitchnotifier.events.NetworkErrorEvent;


public class ErrorHandler implements Response.ErrorListener {

    private static final String LOG_TAG = ErrorHandler.class.getSimpleName();
    private static ErrorHandler instance;
    private static boolean errorShown = false;

    private ErrorHandler() {}

    public static synchronized ErrorHandler getInstance() {
        if (instance == null)
            instance = new ErrorHandler();
        return instance;
    }

    public static void reset() {
        errorShown = false;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(LOG_TAG, null, error);
        if (error instanceof NoConnectionError)
            handleNoConnectionError((NoConnectionError) error);
        else if (error instanceof NetworkError)
            handleNetworkError((NetworkError) error);
        else if (error instanceof TimeoutError)
            handleTimeoutError((TimeoutError) error);
        else if (error instanceof ServerError)
            handleServerError((ServerError) error);
        else if (error instanceof ParseError)
            handleParseError((ParseError) error);
        errorShown = true;
    }

    private void handleParseError(ParseError error) {
        if (!errorShown)
            networkError();
    }

    private void handleNoConnectionError(NoConnectionError error) {
        if (!errorShown)
            networkError();
    }

    private void handleTimeoutError(TimeoutError error) {
        if (!errorShown)
            networkError();
    }

    private void handleNetworkError(NetworkError error) {
        if (!errorShown)
            networkError();
    }

    private void handleServerError(ServerError error) {
        if (errorShown) return;
        if (error.networkResponse.statusCode >= 500 && error.networkResponse.statusCode < 600)
            serverError();
        else if (error.networkResponse.statusCode == 400)
            ToastMaker.makeToastLong(ToastMaker.MESSAGE_INVALID_USERNAME);
        else if (error.networkResponse.statusCode == 429)
            ToastMaker.makeToastLong(ToastMaker.MESSAGE_RATE_LIMIT);
        else
            networkError();
    }

    private static void networkError() {
        ToastMaker.makeToastShort(ToastMaker.MESSAGE_NETWORK_ERROR);
        EventBus.getDefault().post(new NetworkErrorEvent());
    }

    private static void serverError() {
        ToastMaker.makeToastShort(ToastMaker.MESSAGE_SERVER_ERROR);
        EventBus.getDefault().post(new NetworkErrorEvent());
    }
}
