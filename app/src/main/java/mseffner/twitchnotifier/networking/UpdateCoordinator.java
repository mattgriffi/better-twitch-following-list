package mseffner.twitchnotifier.networking;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import mseffner.twitchnotifier.events.UpdateFinishedEvent;

public class UpdateCoordinator {

    private static int activeFollows = 0;
    private static int activeStreams = 0;
    private static int activeUsers = 0;
    private static int activeGames = 0;

    private UpdateCoordinator() {}

    public static synchronized void incrementFollows() {
        activeFollows++;
        log();
    }

    public static synchronized void incrementStreams() {
        activeStreams++;
        log();
    }

    public static synchronized void incrementUsers() {
        activeUsers++;
        log();
    }

    public static synchronized void incrementGames() {
        activeGames++;
        log();
    }

    public static synchronized void decrementFollows() {
        if (activeFollows <= 0) return;
        activeFollows--;
        log();

        if (activeFollows == 0)
            Updates.updateStreams();
    }

    public static synchronized void decrementStreams() {
        if (activeStreams <= 0) return;
        activeStreams--;
        log();

        if (activeStreams == 0) {
            Updates.updateUsers();
            Updates.updateGames();
        }
    }

    public static synchronized void decrementUsers() {
        if (activeUsers <= 0) return;
        decrementUsersNoUpdate();
        if (!updateInProgress())
            updateComplete();
    }

    public static synchronized void decrementUsersNoUpdate() {
        if (activeUsers <= 0) return;
        activeUsers--;
        log();
    }

    public static synchronized void decrementGames() {
        if (activeGames <= 0) return;
        activeGames--;
        log();

        if (!updateInProgress())
            updateComplete();
    }

    public static synchronized int getActiveFollows() {
        return activeFollows;
    }

    public static synchronized int getActiveStreams() {
        return activeStreams;
    }

    public static synchronized int getActiveUsers() {
        return activeUsers;
    }

    public static synchronized int getActiveGames() {
        return activeGames;
    }

    public static synchronized boolean updateInProgress() {
        return !(activeFollows == 0 && activeStreams == 0 && activeUsers == 0 && activeGames == 0);
    }

    public static synchronized void reset() {
        activeFollows = 0;
        activeStreams = 0;
        activeUsers = 0;
        activeGames = 0;
    }

    private static synchronized void updateComplete() {
        EventBus.getDefault().post(new UpdateFinishedEvent());
    }

    public static synchronized void log() {
        Log.e(UpdateCoordinator.class.getSimpleName(),
            "follows: " + UpdateCoordinator.getActiveFollows() + "  " +
            "streams: " + UpdateCoordinator.getActiveStreams() + "  " +
            "users: " + UpdateCoordinator.getActiveUsers() + "  " +
            "games: " + UpdateCoordinator.getActiveGames() + "  " +
            "updateInProgress: " + updateInProgress()
        );
    }
}
