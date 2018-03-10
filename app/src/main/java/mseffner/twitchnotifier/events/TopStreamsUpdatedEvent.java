package mseffner.twitchnotifier.events;


import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.data.ListEntry;

public class TopStreamsUpdatedEvent {

    public List<ListEntry> list = new ArrayList<>();

    public TopStreamsUpdatedEvent(List<ListEntry> list) {
        this.list = list;
    }
}
