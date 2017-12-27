package com.kvajpoj.homie.activities;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.kvajpoj.homie.R;
import com.kvajpoj.homie.model.Homie;
import com.kvajpoj.homie.model.Node;

import org.apache.log4j.Logger;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

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

        MaterialEditTextPreference mServerAddress;
        MaterialEditTextPreference mUsername;
        MaterialEditTextPreference mPassword;
        MaterialEditTextPreference mServerPort;
        MaterialEditTextPreference mAutoSearchNodes;
        Preference mHomieDeleteNodes;

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

            mAutoSearchNodes = (MaterialEditTextPreference)findPreference(getString(R.string.pHomieBaseTopic));
            mAutoSearchNodes.setOnPreferenceChangeListener(this);
            String homieTopic = sharedPref.getString(getString(R.string.pHomieBaseTopic), getString(R.string.prefHomieAutoSearch));
            if (homieTopic.isEmpty()) homieTopic = getString(R.string.prefHomieAutoSearch);
            mAutoSearchNodes.setSummary(homieTopic);
            //mAutoSearchNodes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            //    @Override
            //    public boolean onPreferenceClick(Preference preference) {
                    //code for what you want it to do
            //        LOG.debug("Auto search");
            //        return true;
            //    }
            //});

            mServerAddress = (MaterialEditTextPreference) findPreference("pMqttServerAddress");
            mServerAddress.setOnPreferenceChangeListener(this);
            String address = sharedPref.getString("pMqttServerAddress", getString(R.string.prefServerPrompt));
            if (address.isEmpty()) address = getString(R.string.prefServerPrompt);
            mServerAddress.setSummary(address);

            mServerPort = (MaterialEditTextPreference) findPreference("pMqttServerPort");
            mServerPort.setOnPreferenceChangeListener(this);
            String port = sharedPref.getString("pMqttServerPort", MqttDefaultPort);
            if(port.isEmpty()) port = MqttDefaultPort;
            mServerPort.setSummary(port);

            mUsername = (MaterialEditTextPreference) findPreference("pMqttUsername");
            mUsername.setOnPreferenceChangeListener(this);
            String username = sharedPref.getString("pMqttUsername", getString(R.string.prefUsernamePrompt));
            if(username.isEmpty()) username = getString(R.string.prefUsernamePrompt);
            mUsername.setSummary(username);

            mPassword = (MaterialEditTextPreference) findPreference("pMqttPassword");
            mPassword.setOnPreferenceChangeListener(this);
            String password = sharedPref.getString("pMqttPassword", getString(R.string.prefPasswordPrompt));
            if(password.isEmpty()) password = getString(R.string.prefPasswordPrompt);
            else password = password.replaceAll("(?s).", "*");
            mPassword.setSummary(password);

            mHomieDeleteNodes = getPreferenceManager().findPreference("pHomieDeleteNodes");
            if (mHomieDeleteNodes != null) {

                Realm realm = Realm.getDefaultInstance();
                RealmResults<Node> nodes = realm.where(Node.class).notEqualTo("type", Node.MQTT_CUSTOM_NODE).notEqualTo("type", Node.WEBCAM).findAll();
                mHomieDeleteNodes.setEnabled(nodes != null && nodes.size() > 0);

                mHomieDeleteNodes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {

                        new MaterialDialog.Builder(getActivity())
                                //.iconRes(R.mipmap.ic_launcher)
                                //.limitIconToDefaultSize()
                                .title("Confirm")
                                .content("Do you want to delete all Homie nodes from application?")
                                .positiveText("Delete")
                                .negativeText("Not now")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        deleteHomieNodes();
                                    }
                                })
                                .show();


                        return true;
                    }
                });
            }
        }

        private boolean deleteHomieNodes() {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            RealmResults<Node> nodes = realm.where(Node.class).notEqualTo("type", Node.MQTT_CUSTOM_NODE).notEqualTo("type", Node.WEBCAM).findAll();
            RealmResults<Homie> homies = realm.where(Homie.class).findAll();

            //int nodesNbr = nodes != null ? nodes.size() : 0;
            int homiesNbr = homies != null ? homies.size() : 0;

            if(nodes != null && nodes.size() > 0 && nodes.deleteAllFromRealm()) {
                //Toast.makeText(getActivity(), "Deleted " + nodesNbr + " nodes!",  Toast.LENGTH_SHORT).show();
                //mHomieDeleteNodes.setEnabled(false);
            }
            if(homies != null && homies.size() > 0 && homies.deleteAllFromRealm()) {
                Toast.makeText(getActivity(), "Deleted " + homiesNbr + " Homies!",  Toast.LENGTH_SHORT).show();
                mHomieDeleteNodes.setEnabled(false);
            }
            realm.commitTransaction();
            return true;
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

            if ( preference.getKey().equals(getString(R.string.pHomieBaseTopic)) ){
                String homieTopic = (String) newValue;
                if(homieTopic.isEmpty()) {
                    deleteHomieNodes();
                    homieTopic = getString(R.string.prefHomieAutoSearch);
                }
                preference.setSummary(homieTopic);
            }

            ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();

            return true;
        }

    }

}

