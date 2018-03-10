package mseffner.twitchnotifier.events;


public class DarkModeChangedEvent {

    public boolean darkModeEnabled;

    public DarkModeChangedEvent(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }
}
