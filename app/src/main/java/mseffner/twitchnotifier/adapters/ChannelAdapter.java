package mseffner.twitchnotifier.adapters;

import android.content.Context;
import android.os.Vibrator;
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
    private Vibrator vibrator;
    private final List<ListEntry> channelList;
    private Boolean allowLongClick;

    public ChannelAdapter(@NonNull List<ListEntry> channelList, Boolean allowLongClick, Context context) {
        this.channelList = channelList;
        this.allowLongClick = allowLongClick;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setHasStableIds(true);
    }

    public void setData(@NonNull List<ListEntry> list) {
        channelList.clear();
        channelList.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return channelList.get(position).id;
    }

    @Override
    public CompactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (SettingsManager.getCompactSetting()) {
            View view = inflater.inflate(R.layout.layout_list_item_compact, parent, false);
            return new CompactViewHolder(view, vibrator);
        } else {
            View view = inflater.inflate(R.layout.layout_list_item_verbose, parent, false);
            return new VerboseViewHolder(view, vibrator);
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
