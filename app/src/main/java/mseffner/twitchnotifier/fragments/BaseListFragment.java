package mseffner.twitchnotifier.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.data.ChannelAdapter;
import mseffner.twitchnotifier.data.ListEntry;

public abstract class BaseListFragment extends Fragment {

    private static final String LAYOUT_MANAGER_STATE_KEY = "state";

    protected RecyclerView recyclerView;
    protected ChannelAdapter channelAdapter;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected FloatingActionButton scrollTopButton;
    protected FloatingActionButton refreshButton;
    protected LinearLayout startMessage;

    protected boolean needToRestoreState = false;
    protected Parcelable layoutManagerState;

    protected abstract void refreshList();
    protected abstract void cancelAsyncTasks();
    protected abstract boolean getLongClickSetting();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        // Get the views
        recyclerView = rootView.findViewById(R.id.list_recyclerview);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
        scrollTopButton = rootView.findViewById(R.id.scroll_top_fab);
        refreshButton = rootView.findViewById(R.id.refresh_fab);
        startMessage = rootView.findViewById(R.id.start_message);

        Context context = recyclerView.getContext();

        // Check to see if layout manager state needs to be restored
        needToRestoreState = savedInstanceState != null;
        if (needToRestoreState)
            layoutManagerState = savedInstanceState.getParcelable(LAYOUT_MANAGER_STATE_KEY);

        // Set up the recyclerView
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        channelAdapter = new ChannelAdapter(new ArrayList<>(), getLongClickSetting());
        recyclerView.setAdapter(channelAdapter);

        // Set up the swipe refresh
        swipeRefreshLayout.setColorSchemeResources(R.color.colorWhiteAlpha);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorPrimaryAlpha);
        swipeRefreshLayout.setOnRefreshListener(this::refreshList);

        // Set up floating action button animations
        final Animation scrollScaleUp = getScaleAnimation(context, scrollTopButton, R.anim.scale_up);
        final Animation refreshScaleUp = getScaleAnimation(context, refreshButton, R.anim.scale_up);
        final Animation scrollScaleDown = getScaleAnimation(context, scrollTopButton, R.anim.scale_down);
        final Animation refreshScaleDown = getScaleAnimation(context, refreshButton, R.anim.scale_down);

        // Animate the floating action buttons
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // Both buttons will behave the same, so only need to check the state of one
                if (layoutManager.findFirstVisibleItemPosition() == 0 &&
                        scrollTopButton.getVisibility() == View.VISIBLE &&
                        scrollTopButton.getAnimation() == null) {
                    scrollTopButton.startAnimation(scrollScaleDown);
                    refreshButton.startAnimation(refreshScaleDown);
                } else if (layoutManager.findFirstVisibleItemPosition() != 0 &&
                        scrollTopButton.getAnimation() == null &&
                        scrollTopButton.getVisibility() == View.INVISIBLE &&
                        scrollTopButton.getAnimation() == null){
                    scrollTopButton.startAnimation(scrollScaleUp);
                    refreshButton.startAnimation(refreshScaleUp);
                }
            }
        });

        // Add click listeners to buttons
        scrollTopButton.setOnClickListener(view -> layoutManager.smoothScrollToPosition(recyclerView, null, 0));
        refreshButton.setOnClickListener(view -> refreshList());

        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelAsyncTasks();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LAYOUT_MANAGER_STATE_KEY, recyclerView.getLayoutManager().onSaveInstanceState());
    }

    protected void updateAdapter(@NonNull List<ListEntry> list) {
        channelAdapter.setData(list);
        // If we are updating for the first time after restoring state, restore
        // linear layout state to save scroll position
        if (needToRestoreState) {
            recyclerView.getLayoutManager().onRestoreInstanceState(layoutManagerState);
            needToRestoreState = false;
        }
    }

    private Animation getScaleAnimation(Context context, FloatingActionButton floatingActionButton, int animResource) {
        Animation anim;
        if (animResource == R.anim.scale_up) {
            anim = AnimationUtils.loadAnimation(context, R.anim.scale_up);
            anim.setAnimationListener(new FABAnimationListener(floatingActionButton, true));
        } else {
            anim = AnimationUtils.loadAnimation(context, R.anim.scale_down);
            anim.setAnimationListener(new FABAnimationListener(floatingActionButton, false));
        }
        return anim;
    }

    private static class FABAnimationListener implements Animation.AnimationListener {

        private FloatingActionButton fab;
        private boolean up;

        FABAnimationListener(FloatingActionButton floatingActionButton, boolean up) {
            fab = floatingActionButton;
            this.up = up;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (up)
                fab.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (!up)
                fab.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}
