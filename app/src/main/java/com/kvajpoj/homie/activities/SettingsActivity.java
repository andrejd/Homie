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

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        EditTextPreference mServerAddress;
        EditTextPreference mUsername;
        EditTextPreference mPassword;
        EditTextPreference mServerPort;

        private Logger LOG;
        private SharedPreferences sharedPref;
        private String MqttDefaultPort = "1883";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            LOG = Logger.getLogger(SettingsActivity.class);

            mServerAddress = (EditTextPreference) findPreference("pMqttServerAddress");
            mServerAddress.setOnPreferenceChangeListener(this);
            String address = sharedPref.getString("pMqttServerAddress", getString(R.string.prefServerPrompt));
            if (address.isEmpty()) address = getString(R.string.prefServerPrompt);
            mServerAddress.setSummary(address);

            mServerPort = (EditTextPreference) findPreference("pMqttServerPort");
            mServerPort.setOnPreferenceChangeListener(this);
            String port = sharedPref.getString("pMqttServerPort", MqttDefaultPort);
            if(port.isEmpty()) port = MqttDefaultPort;
            mServerPort.setSummary(port);

            mUsername = (EditTextPreference) findPreference("pMqttUsername");
            mUsername.setOnPreferenceChangeListener(this);
            String username = sharedPref.getString("pMqttUsername", getString(R.string.prefUsernamePrompt));
            if(username.isEmpty()) username = getString(R.string.prefUsernamePrompt);
            mUsername.setSummary(username);

            mPassword = (EditTextPreference) findPreference("pMqttPassword");
            mPassword.setOnPreferenceChangeListener(this);
            String password = sharedPref.getString("pMqttPassword", getString(R.string.prefPasswordPrompt));
            if(password.isEmpty()) password = getString(R.string.prefPasswordPrompt);
            else password = password.replaceAll("(?s).", "*");
            mPassword.setSummary(password);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if ( preference.getKey().equals("pMqttServerAddress") ){
                String server = (String) newValue;
                if(server.isEmpty()) server = getString(R.string.prefServerPrompt);
                preference.setSummary(server);
            }

            if ( preference.getKey().equals("pMqttServerPort") ){
                String port = (String) newValue;
                if(port.isEmpty()) port = MqttDefaultPort;
                preference.setSummary(port);
            }

            if ( preference.getKey().equals("pMqttPassword") ){
                String password = (String) newValue;
                if(password.isEmpty()) password = getString(R.string.prefPasswordPrompt);
                else password = password.replaceAll("(?s).", "*");
                preference.setSummary(password);
            }

            if ( preference.getKey().equals("pMqttUsername") ){
                String username = (String) newValue;
                if(username.isEmpty()) username = getString(R.string.prefUsernamePrompt);
                preference.setSummary(username);
            }

            ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();

            return true;
        }

    }

}

