package mseffner.twitchnotifier.networking;

import android.util.Log;

public class RequestTracker {

    private static int activeFollows = 0;
    private static int activeStreams = 0;
    private static int activeUsers = 0;
    private static int activeGames = 0;

    private RequestTracker() {}

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
    }

    public static synchronized void decrementStreams() {
        if (activeStreams <= 0) return;
        activeStreams--;
        log();
    }

    public static synchronized void decrementUsers() {
        if (activeUsers <= 0) return;
        activeUsers--;
        log();
    }

    public static synchronized void decrementGames() {
        if (activeGames <= 0) return;
        activeGames--;
        log();
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

    public static synchronized void log() {
        Log.e(RequestTracker.class.getSimpleName(),
            "follows: " + RequestTracker.getActiveFollows() + "  " +
            "streams: " + RequestTracker.getActiveStreams() + "  " +
            "users: " + RequestTracker.getActiveUsers() + "  " +
            "games: " + RequestTracker.getActiveGames() + "  " +
            "updateInProgress: " + updateInProgress()
        );
    }
}
