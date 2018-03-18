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


public class ChannelAdapter extends RecyclerView.Adapter<MinimalViewHolder> {

    private static final int VIBRATE_TIME = 5;
    private Vibrator vibrator;
    private List<ListEntry> list;
    private Boolean allowLongClick;

    public ChannelAdapter(@NonNull List<ListEntry> list, Boolean allowLongClick, Context context) {
        this.list = list;
        this.allowLongClick = allowLongClick;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setHasStableIds(true);
    }

    public void setData(@NonNull List<ListEntry> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).id;
    }

    @NonNull
    @Override
    public MinimalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (SettingsManager.getListModeSetting() == SettingsManager.LIST_MODE_COMPACT) {
            View view = inflater.inflate(R.layout.layout_list_item_compact, parent, false);
            return new CompactViewHolder(view, vibrator);
        } else if (SettingsManager.getListModeSetting() == SettingsManager.LIST_MODE_MINIMAL) {
            View view = inflater.inflate(R.layout.layout_list_item_minimal, parent, false);
            return new MinimalViewHolder(view, vibrator);
        } else {
            View view = inflater.inflate(R.layout.layout_list_item_verbose, parent, false);
            return new FullViewHolder(view, vibrator);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MinimalViewHolder holder, int position) {
        holder.bind(list.get(position), allowLongClick, VIBRATE_TIME);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
