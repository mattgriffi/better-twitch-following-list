package mseffner.twitchnotifier.adapters;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import mseffner.twitchnotifier.R;
import mseffner.twitchnotifier.fragments.FollowingListFragment;
import mseffner.twitchnotifier.fragments.SettingsFragment;
import mseffner.twitchnotifier.fragments.TopListFragment;

public class ListPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public ListPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    @Override
    public android.app.Fragment getItem(int position) {
        switch (position) {
            case 0: return new FollowingListFragment();
            case 1: return new TopListFragment();
            default: return new SettingsFragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: return context.getResources().getString(R.string.following_tab);
            case 1: return context.getResources().getString(R.string.top_tab);
            default: return context.getResources().getString(R.string.settings);
        }
    }
}
