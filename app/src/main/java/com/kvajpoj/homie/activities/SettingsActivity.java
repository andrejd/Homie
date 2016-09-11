package com.kvajpoj.homie.activities;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.kvajpoj.homie.R;


public class SettingsActivity extends AppCompatActivity {

    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initPreferences();
    }

    private void initPreferences() {

        mSettingsFragment = new SettingsFragment();
        //mSettingsFragment.setBuildTime(mBuildTime);

        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();

        mFragmentTransaction.replace(android.R.id.content, mSettingsFragment);
        mFragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        Preference mRefreshOnBackgroundPreference;
        Preference mVersionPreference;
        String mBuildTime = "";

        public void setBuildTime(String buildTime) {
            mBuildTime = buildTime;
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            //mRefreshOnBackgroundPreference = findPreference("prefRefreshOnBackground");
            //mRefreshOnBackgroundPreference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {


            return true;
        }

    }

}

