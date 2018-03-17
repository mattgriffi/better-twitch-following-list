package mseffner.twitchnotifier.events;


import java.util.List;

import mseffner.twitchnotifier.data.ListEntry;

public class PreStreamUpdateEvent {

    public List<ListEntry> list;

    public PreStreamUpdateEvent(List<ListEntry> list) {
        this.list = list;
    }
}
