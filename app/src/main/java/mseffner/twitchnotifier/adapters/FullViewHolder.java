package mseffner.twitchnotifier.adapters;


import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ListEntry;

public class FullViewHolder extends CompactViewHolder {

    private ImageView profileImage;
    private TextView title;

    public FullViewHolder(View itemView, Vibrator vibrator) {
        super(itemView, vibrator);
        profileImage = itemView.findViewById(R.id.channel_logo);
        title = itemView.findViewById(R.id.title);
    }

    @Override
    public void bind(ListEntry listEntry, boolean allowLongClick, int vibrateTime) {
        super.bind(listEntry, allowLongClick, vibrateTime);
        // Set up Glide to load the profile image
        GlideApp.with(profileImage.getContext())
                .load(listEntry.profileImageUrl)
                .placeholder(R.drawable.default_logo_300x300)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(profileImage);
    }

    @Override
    protected void bindOfflineStream() {
        super.bindOfflineStream();
        title.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void bindOnlineStream(final ListEntry listEntry) {
        super.bindOnlineStream(listEntry);
        title.setVisibility(View.VISIBLE);
        title.setText(listEntry.title);
    }
}