package mseffner.twitchnotifier.data;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * This class handles the boilerplate of starting and stopping the handler thread.
 */
public class ThreadManager {

    private static final String NAME = "background_thread";

    private static boolean initialized;
    private static HandlerThread handlerThread;
    private static Handler handler;

    public static void initialize() {
        initialized = true;
        if (handlerThread == null) {
            handlerThread = new HandlerThread(NAME);
            handlerThread.start();
        }
        handler = new Handler(handlerThread.getLooper());
    }

    public static void destroy() {
        initialized = false;
        // Since HandlerThread.quitSafely is only API level 18, quit the
        // thread safely by posting a poison pill to the end of the queue
        post(() -> {
            if (!initialized) {
                handlerThread.quit();
                handlerThread = null;
                handler = null;
            }
        });
    }

    public static void postMainThread(Runnable r) {
        // Check to see if we're already on the main thread
        if (Looper.myLooper() == Looper.getMainLooper())
            r.run();
        else
            new Handler(Looper.getMainLooper()).post(r);
    }

    public static void post(Runnable r) {
        handler.post(r);
    }

    public static void postDelayed(Runnable r, long ms) {
        handler.postDelayed(r, ms);
    }
}
