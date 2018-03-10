package mseffner.twitchnotifier.events;


import java.util.List;

import mseffner.twitchnotifier.data.ListEntry;

public class ListUpdatedEvent {

    public List<ListEntry> list;

    public ListUpdatedEvent(List<ListEntry> list) {
        this.list = list;
    }
}
