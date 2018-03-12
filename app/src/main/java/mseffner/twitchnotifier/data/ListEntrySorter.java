package mseffner.twitchnotifier.data;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mseffner.twitchnotifier.data.ChannelContract.StreamEntry;
import mseffner.twitchnotifier.settings.SettingsManager;

public class ListEntrySorter {

    public static void sort(List<ListEntry> list) {
        Collections.sort(list, new DisplayNameLexicographicComparator());
        Collections.sort(list, new ViewerCountComparator());
        doFinalSort(list);
        Collections.sort(list, new PinnedComparator());
        Collections.sort(list, new TypeComparator());
    }

    private static void doFinalSort(List<ListEntry> list) {
        int sortBy = SettingsManager.getSortBySetting();
        boolean ascending = SettingsManager.getSortAscendingSetting();

        switch (sortBy) {
            case SettingsManager.SORT_BY_VIEWER_COUNT:
                if (ascending)
                    Collections.sort(list, Collections.reverseOrder(new ViewerCountComparator()));
                else
                    Collections.sort(list, new ViewerCountComparator());
                break;
            case SettingsManager.SORT_BY_NAME:
                if (ascending)
                    Collections.sort(list, new DisplayNameLexicographicComparator());
                else
                    Collections.sort(list, Collections.reverseOrder(new DisplayNameLexicographicComparator()));
                break;
            case SettingsManager.SORT_BY_GAME:
                if (ascending)
                    Collections.sort(list, new GameNameLexicographicComparator());
                else
                    Collections.sort(list, Collections.reverseOrder(new GameNameLexicographicComparator()));
                break;
            case SettingsManager.SORT_BY_UPTIME:
                if (ascending)
                    Collections.sort(list, Collections.reverseOrder(new UptimeComparator()));
                else
                    Collections.sort(list, new UptimeComparator());
                break;
        }
    }

    private static class TypeComparator implements Comparator<ListEntry> {
        @Override
        public int compare(ListEntry a, ListEntry b) {
            boolean rerunOnline = SettingsManager.getRerunSetting() != SettingsManager.RERUN_OFFLINE;
            boolean aLive = a.type == StreamEntry.STREAM_TYPE_LIVE || (rerunOnline && a.type == StreamEntry.STREAM_TYPE_RERUN);
            boolean bLive = b.type == StreamEntry.STREAM_TYPE_LIVE || (rerunOnline && b.type == StreamEntry.STREAM_TYPE_RERUN);
            if (aLive && !bLive)
                return -1;
            else if (!aLive && bLive)
                return 1;
            return 0;
        }
    }

    private static class ViewerCountComparator implements Comparator<ListEntry> {
        @Override
        public int compare(ListEntry a, ListEntry b) {
            return b.viewerCount - a.viewerCount;
        }
    }

    private static class PinnedComparator implements Comparator<ListEntry> {
        @Override
        public int compare(ListEntry a, ListEntry b) {
            if (a.pinned && !b.pinned)
                return -1;
            else if (!a.pinned && b.pinned)
                return 1;
            return 0;
        }
    }

    private static class DisplayNameLexicographicComparator implements Comparator<ListEntry> {
        @Override
        public int compare(ListEntry a, ListEntry b) {
            return a.displayName.compareToIgnoreCase(b.displayName);
        }
    }

    private static class GameNameLexicographicComparator implements Comparator<ListEntry> {
        @Override
        public int compare(ListEntry a, ListEntry b) {
            return a.gameName.compareToIgnoreCase(b.gameName);
        }
    }

    private static class UptimeComparator implements  Comparator<ListEntry> {
        @Override
        public int compare(ListEntry a, ListEntry b) {
            if (b.startedAt - a.startedAt > 0)
                return 1;
            else if (b.startedAt - a.startedAt < 0)
                return -1;
            return 0;
        }
    }
}
