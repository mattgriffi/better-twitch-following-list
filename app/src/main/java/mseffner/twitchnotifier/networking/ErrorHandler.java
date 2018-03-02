package mseffner.twitchnotifier.networking;

import android.util.Log;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import mseffner.twitchnotifier.ToastMaker;


public abstract class ErrorHandler implements Response.ErrorListener {

    private static final String LOG_TAG = ErrorHandler.class.getSimpleName();

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(LOG_TAG, null, error);

        boolean handled = customHandling(error);
        if (handled) return;

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
    }

    protected void handleParseError(ParseError error) {
        ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
    }

    protected void handleNoConnectionError(NoConnectionError error) {
        ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
    }

    protected void handleTimeoutError(TimeoutError error) {
        ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
    }

    protected void handleNetworkError(NetworkError error) {
        ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
    }

    protected void handleServerError(ServerError error) {
        if (error.networkResponse.statusCode >= 500 && error.networkResponse.statusCode < 600)
            ToastMaker.makeToastLong(ToastMaker.MESSAGE_SERVER_ERROR);
        else
            ToastMaker.makeToastLong(ToastMaker.MESSAGE_NETWORK_ERROR);
    }

    protected boolean customHandling(VolleyError error) {
        return false;
    }
}
