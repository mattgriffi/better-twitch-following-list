package mseffner.twitchnotifier.data;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import mseffner.twitchnotifier.R;

class VerboseViewHolder extends CompactViewHolder {

    private ImageView channelLogo;
    private TextView streamTitle;

    VerboseViewHolder(View itemView) {
        super(itemView);
        channelLogo = itemView.findViewById(R.id.channel_logo);
        streamTitle = itemView.findViewById(R.id.title);
    }

    @Override
    protected void bind(ListEntry listEntry, boolean allowLongClick, int vibrateTime) {
        super.bind(listEntry, allowLongClick, vibrateTime);
        // Set up Picasso to load the channel logo
        Picasso.with(itemView.getContext())
                .load(listEntry.profileImageUrl)
                .placeholder(R.drawable.default_logo_300x300)
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