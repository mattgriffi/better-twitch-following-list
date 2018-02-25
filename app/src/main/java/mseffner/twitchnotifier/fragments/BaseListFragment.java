package mseffner.twitchnotifier.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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

import mseffner.twitchnotifier.R;

public abstract class BaseListFragment extends Fragment {

    protected RecyclerView recyclerView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected FloatingActionButton scrollTopButton;
    protected FloatingActionButton refreshButton;
    protected LinearLayout startMessage;
    protected Context context;

    protected abstract void refreshList();
    protected abstract void cancelAsyncTasks();

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
        startMessage = rootView.findViewById(R.id.get_started_message);

        Context context = recyclerView.getContext();

        // Start the refresh animation (will be stopped when child classes finish their stuff)
        swipeRefreshLayout.setRefreshing(true);

        // Set up the layout manager
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        // Set up the swipe refresh
        swipeRefreshLayout.setColorSchemeResources(R.color.colorWhiteAlpha);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorPrimaryAlpha);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });


        // Set up floating action button animations
        final Animation scrollScaleUp = getScaleAnimation(scrollTopButton, R.anim.scale_up);
        final Animation refreshScaleUp = getScaleAnimation(refreshButton, R.anim.scale_up);
        final Animation scrollScaleDown = getScaleAnimation(scrollTopButton, R.anim.scale_down);
        final Animation refreshScaleDown = getScaleAnimation(refreshButton, R.anim.scale_down);

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

        // Make the scroll button actually do something
        scrollTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutManager.smoothScrollToPosition(recyclerView, null, 0);
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshList();
            }
        });

        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelAsyncTasks();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        // This is needed to support API versions below 23
        // because the support library is a mess
        super.onAttach(activity);
        this.context = activity.getApplicationContext();
    }

    private Animation getScaleAnimation(FloatingActionButton floatingActionButton, int animResource) {
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

    private class FABAnimationListener implements Animation.AnimationListener {

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
