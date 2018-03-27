package mseffner.twitchnotifier.networking;


import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.data.ChannelDb;
import mseffner.twitchnotifier.data.DataUpdateManager;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.PreStreamUpdateEvent;
import mseffner.twitchnotifier.events.StreamsUpdatedEvent;
import mseffner.twitchnotifier.events.TopStreamsUpdatedEvent;
import mseffner.twitchnotifier.settings.SettingsManager;

public class PeriodicUpdater implements Runnable {

    private boolean needPreStreamData;

    @Override
    public void run() {
        RequestTracker.log();
        // If we are ready for a new update
        if (SettingsManager.rateLimitReset()) {
            SettingsManager.setLastUpdated();
            /* Fetch the follows before deleting streams data when first starting so the follows
            list doesn't show everything as offline before the new update completes. */
            if (needPreStreamData) {
                PreStreamUpdateEvent stickyEvent = EventBus.getDefault().getStickyEvent(PreStreamUpdateEvent.class);
                if (stickyEvent != null)
                    EventBus.getDefault().removeStickyEvent(stickyEvent);
                EventBus.getDefault().postSticky(new PreStreamUpdateEvent(ChannelDb.getAllFollows()));
            }
            // Prepare for the update
            ChannelDb.deleteAllStreams();
            ErrorHandler.reset();
            RequestTracker.reset();
            // Update either the follows or just streams
            if (SettingsManager.getFollowsNeedUpdate())
                DataUpdateManager.updateFollowsData();
            else
                DataUpdateManager.updateStreamsData();
            // Update top streams
            DataUpdateManager.updateTopStreamsData();
        } else {  // If we shouldn't update yet, refresh with old data
            EventBus.getDefault().post(new StreamsUpdatedEvent());
            EventBus.getDefault().post(new TopStreamsUpdatedEvent());
        }
        needPreStreamData = false;
        ThreadManager.postDelayed(this, 15 * 1000);
    }

    public void start() {
        needPreStreamData = true;
        run();
    }

    public void stop() {
        ThreadManager.removeCallbacks(this);
        PreStreamUpdateEvent stickyEvent = EventBus.getDefault().getStickyEvent(PreStreamUpdateEvent.class);
        if (stickyEvent != null) {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
        }
    }
}
