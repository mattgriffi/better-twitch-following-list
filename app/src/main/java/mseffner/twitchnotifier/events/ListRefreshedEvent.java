package mseffner.twitchnotifier.events;


import java.util.List;

import mseffner.twitchnotifier.data.ListEntry;

public class ListRefreshedEvent {

    public List<ListEntry> list;

    public ListRefreshedEvent(List<ListEntry> list) {
        this.list = list;
    }
}
