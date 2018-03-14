package mseffner.twitchnotifier.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ListEntry;
import mseffner.twitchnotifier.settings.SettingsManager;


public class ChannelAdapter extends RecyclerView.Adapter<CompactViewHolder> {

    private static final int VIBRATE_TIME = 3;

    private final List<ListEntry> channelList;
    private Boolean allowLongClick;

    public ChannelAdapter(@NonNull List<ListEntry> channelList, Boolean allowLongClick) {
        this.channelList = channelList;
        this.allowLongClick = allowLongClick;
    }

    public void setData(@NonNull List<ListEntry> list) {
        channelList.clear();
        channelList.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public CompactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (SettingsManager.getCompactSetting()) {
            View view = inflater.inflate(R.layout.layout_list_item_compact, parent, false);
            return new CompactViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.layout_list_item_verbose, parent, false);
            return new VerboseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(CompactViewHolder holder, int position) {
        holder.bind(channelList.get(position), allowLongClick, VIBRATE_TIME);
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }
}
