package mseffner.twitchnotifier.data;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;


public class ChannelAdapter extends RecyclerView.Adapter {

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        public ChannelViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
