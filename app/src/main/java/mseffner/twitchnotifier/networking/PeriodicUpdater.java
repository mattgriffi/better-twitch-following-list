package mseffner.twitchnotifier.networking;


import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.data.ThreadManager;

public class PeriodicUpdater implements Runnable {

    @Override
    public void run() {
        DataUpdateManager.updateStreamsData(new ErrorHandler() {});
        ThreadManager.postDelayed(this, 10 * 1000);
    }

    public void start() {
        run();
    }

    public void stop() {
        ThreadManager.removeCallbacks(this);
    }
}
