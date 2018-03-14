package mseffner.twitchnotifier.networking;


import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;
import mseffner.twitchnotifier.events.TopStreamsUpdatedEvent;
import mseffner.twitchnotifier.settings.SettingsManager;

public class PeriodicUpdater implements Runnable {

    @Override
    public void run() {
        if (SettingsManager.rateLimitReset()) {
            ChannelDb.deleteAllStreams();
            if (SettingsManager.getFollowsNeedUpdate())
                DataUpdateManager.updateFollowsData(new ErrorHandler() {});
            else
                DataUpdateManager.updateStreamsData(new ErrorHandler() {});
            DataUpdateManager.updateTopStreamsData(new ErrorHandler() {});
        } else {
            EventBus.getDefault().post(new StreamsUpdatedEvent());
            EventBus.getDefault().post(new TopStreamsUpdatedEvent());
        }
        ThreadManager.postDelayed(this, 70 * 1000);
    }

    public void start() {
        run();
    }

    public void stop() {
        ThreadManager.removeCallbacks(this);
    }
}
