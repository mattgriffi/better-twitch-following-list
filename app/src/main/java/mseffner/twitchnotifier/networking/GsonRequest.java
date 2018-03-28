package mseffner.twitchnotifier.networking;

import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.util.Map;


public class GsonRequest<T> extends Request<T> {

    private static final int TIMEOUT_MS = 1000;
    private static final int RETRIES = 3;
    private static final int BACKOFF_MUL = 2;

    private final Gson gson = new Gson();
    private final Map<String, String> headers;
    private Class<T> clazz;
    private Response.Listener<T> listener;

    public GsonRequest(String url, Class<T> clazz, Map<String, String> headers,
                       @Nullable Response.Listener<T> listener, @Nullable Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.headers = headers;
        this.clazz = clazz;
        this.listener = listener;
        Log.i(GsonRequest.class.getSimpleName(), url);
        setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, RETRIES, BACKOFF_MUL));
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            // Get the json from the response
            String json = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            Log.i(GsonRequest.class.getSimpleName(), json);
            // Build object with gson and return it
            return Response.success(
                    gson.fromJson(json, clazz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(T response) {
        if (listener != null)
            listener.onResponse(response);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }
}
