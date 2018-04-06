package mseffner.twitchnotifier.networking;


import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.data.Database;
import mseffner.twitchnotifier.data.ThreadManager;
import mseffner.twitchnotifier.events.PreStreamUpdateEvent;
import mseffner.twitchnotifier.events.UpdateFinishedEvent;
import mseffner.twitchnotifier.events.UpdateStartedEvent;
import mseffner.twitchnotifier.settings.SettingsManager;

public class PeriodicUpdater implements Runnable {

    private boolean needPreStreamData;

    @Override
    public void run() {
        UpdateCoordinator.log();
        // If we are ready for a new update
        if (SettingsManager.rateLimitReset()) {
            EventBus.getDefault().post(new UpdateStartedEvent());
            SettingsManager.setLastUpdated();
            /* Fetch the follows before deleting streams data when first starting so the follows
            list doesn't show everything as offline before the new update completes. */
            if (needPreStreamData) {
                PreStreamUpdateEvent stickyEvent = EventBus.getDefault().getStickyEvent(PreStreamUpdateEvent.class);
                if (stickyEvent != null)
                    EventBus.getDefault().removeStickyEvent(stickyEvent);
                EventBus.getDefault().postSticky(new PreStreamUpdateEvent(Database.getAllFollows()));
            }
            // Prepare for the update
            Database.deleteAllStreams();
            ErrorHandler.reset();
            UpdateCoordinator.reset();
            // Update either the follows or just streams
            if (SettingsManager.getFollowsNeedUpdate())
                Updates.updateFollows();
            else
                Updates.updateStreams();
        } else {  // If we shouldn't update yet, refresh with old data
            EventBus.getDefault().post(new UpdateFinishedEvent());
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
