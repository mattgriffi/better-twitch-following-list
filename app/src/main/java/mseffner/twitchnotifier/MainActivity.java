package mseffner.twitchnotifier;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the theme based on whether dark mode is on
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        boolean darkMode = sharedPreferences.getBoolean(getString(R.string.pref_dark_mode), false);
        setTheme(darkMode ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        // Set the content view
        setContentView(R.layout.activity_main);

        // Set up ViewPager and TabLayout
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        ListPagerAdapter adapter = new ListPagerAdapter(this, getFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_dark_mode))) {
            // If dark mode has been toggled, recreate activity so new theme is applied
            recreate();
        }
    }
}
