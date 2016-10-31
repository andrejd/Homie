package com.kvajpoj.homie.activities;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.BaseAdapter;

import com.kvajpoj.homie.R;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.StringMatchFilter;


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

        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
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

    //@Override
    //protected void onPause() {

    //    super.onPause();
    //}

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        //Preference mRefreshOnBackgroundPreference;
        //Preference mVersionPreference;
        //String mBuildTime = "";
        EditTextPreference mServerAddress;
        EditTextPreference mUsername;
        EditTextPreference mPassword;
        EditTextPreference mServerPort;

        //public void setBuildTime(String buildTime) {
        //    mBuildTime = buildTime;
        //}

        private Logger LOG;
        private SharedPreferences sharedPref;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            LOG = Logger.getLogger(SettingsActivity.class);

            mServerAddress = (EditTextPreference) findPreference("pMqttServerAddress");
            mServerAddress.setOnPreferenceChangeListener(this);
            mServerAddress.setSummary(sharedPref.getString("pMqttServerAddress","Tap to set Mqtt server address"));

            mServerPort = (EditTextPreference) findPreference("pMqttServerPort");
            mServerPort.setOnPreferenceChangeListener(this);
            mServerPort.setSummary(sharedPref.getString("pMqttServerPort","1833"));

            mUsername = (EditTextPreference) findPreference("pMqttUsername");
            mUsername.setOnPreferenceChangeListener(this);
            mUsername.setSummary(sharedPref.getString("pMqttUsername","Tap to set username"));

            mPassword = (EditTextPreference) findPreference("pMqttPassword");
            mPassword.setOnPreferenceChangeListener(this);
            String password = sharedPref.getString("pMqttPassword", getString(R.string.prefPasswordPrompt));
            if(password.isEmpty()) password = getString(R.string.prefPasswordPrompt);
            else password = password.replaceAll("(?s).", "*");
            mPassword.setSummary(password);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (LOG.isDebugEnabled()) LOG.debug("preference changed " + preference.getKey());

            if ( preference.getKey().equals("pMqttPassword") ){
                String password = (String) newValue;//sharedPref.getString("pMqttPassword", getString(R.string.prefPasswordPrompt));
                LOG.debug(password);
                if(password.isEmpty()) {
                    password = getString(R.string.prefPasswordPrompt);
                }
                else {
                    password = password.replaceAll("(?s).", "*");
                }

                LOG.debug("Setting summary to " + password);
                preference.setSummary(password);
                //mPassword.setSummary(password);

            }

            ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();





            //preference.setSummary(sharedPref.getString(preference.getKey(), (String) preference.getSummary()));
            return true;
        }

    }

}

