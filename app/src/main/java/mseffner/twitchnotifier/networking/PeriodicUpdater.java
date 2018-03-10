package mseffner.twitchnotifier.networking;


import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.data.ThreadManager;

public class PeriodicUpdater implements Runnable {

    public boolean okay;

    @Override
    public void run() {
        if (!okay) return;
        DataUpdateManager.updateStreamsData(new ErrorHandler() {});
        ThreadManager.postDelayed(this, 65 * 1000);
    }
}
