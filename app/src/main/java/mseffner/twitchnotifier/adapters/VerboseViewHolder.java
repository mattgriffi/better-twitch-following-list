package mseffner.twitchnotifier.adapters;


import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ListEntry;

public class VerboseViewHolder extends CompactViewHolder {

    private ImageView channelLogo;
    private TextView streamTitle;

    public VerboseViewHolder(View itemView, Vibrator vibrator) {
        super(itemView, vibrator);
        channelLogo = itemView.findViewById(R.id.channel_logo);
        streamTitle = itemView.findViewById(R.id.title);
    }

    @Override
    public void bind(ListEntry listEntry, boolean allowLongClick, int vibrateTime) {
        super.bind(listEntry, allowLongClick, vibrateTime);
        // Set up Glide to load the profile image
        GlideApp.with(itemView.getContext())
                .load(listEntry.profileImageUrl)
                .placeholder(R.drawable.default_logo_300x300)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(channelLogo);
    }

    @Override
    protected void bindOfflineStream() {
        super.bindOfflineStream();
        streamTitle.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void bindOnlineStream(final ListEntry listEntry) {
        super.bindOnlineStream(listEntry);
        streamTitle.setVisibility(View.VISIBLE);
        streamTitle.setText(listEntry.title);
    }
}