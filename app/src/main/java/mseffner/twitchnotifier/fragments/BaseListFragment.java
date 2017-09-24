package mseffner.twitchnotifier.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import mseffner.twitchnotifier.R;

public abstract class BaseListFragment extends Fragment {

    protected RecyclerView recyclerView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected FloatingActionButton scrollTopButton;
    protected RelativeLayout startMessage;
    protected Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        // Get the views
        recyclerView = rootView.findViewById(R.id.list_recyclerview);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
        scrollTopButton = rootView.findViewById(R.id.scroll_top_fab);
        startMessage = rootView.findViewById(R.id.get_started_message);

        Context context = recyclerView.getContext();

        // Start the refresh animation (will be disabled when UpdateAdapterAsyncTask completes)
        swipeRefreshLayout.setRefreshing(true);

        // Set up scroll button animations
        final Animation animScaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up);
        final Animation animScaleDown = AnimationUtils.loadAnimation(context, R.anim.scale_down);
        animScaleUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                scrollTopButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        animScaleDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                scrollTopButton.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Set up the layout manager
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        // Set up the swipe refresh
        swipeRefreshLayout.setColorSchemeColors(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        // Animate the scroll button
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (layoutManager.findFirstVisibleItemPosition() == 0 &&
                        scrollTopButton.getVisibility() == View.VISIBLE &&
                        scrollTopButton.getAnimation() == null) {
                    scrollTopButton.startAnimation(animScaleDown);
                } else if (layoutManager.findFirstVisibleItemPosition() != 0 &&
                        scrollTopButton.getAnimation() == null &&
                        scrollTopButton.getVisibility() == View.INVISIBLE &&
                        scrollTopButton.getAnimation() == null){
                    scrollTopButton.startAnimation(animScaleUp);
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

    @Override
    public void onAttach(Activity activity) {
        // This is needed to support API versions below 23
        // because the support library is a mess
        super.onAttach(activity);
        this.context = activity.getApplicationContext();
    }

    protected abstract void refreshList();
    protected abstract void cancelAsyncTasks();
}
