package com.kvajpoj.homie.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Andrej on 1.11.2016.
 */

public class Settings {

    private String username;
    private String password;
    private String serverPort;
    private String serverUrl;

    private static Settings instance = null;
    private SharedPreferences sharedPref;

    protected Settings() {
        // Exists only to defeat instantiation.
    }

    public static Settings getInstance() {
        if(instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public Boolean reloadSettings(Context context) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        String address = sharedPref.getString("pMqttServerAddress", "");
        setServerUrl(address);

        String port = sharedPref.getString( "pMqttServerPort", "1883" );
        if(port.isEmpty()) port = "1883";
        setServerPort(port);

        String username = sharedPref.getString( "pMqttUsername", "" );
        setUsername(username);

        String password = sharedPref.getString( "pMqttPassword", "" );
        setPassword(password);
        return true;
    }

    public String getPassword() { return password; }
    private void setPassword(String password) { this.password = password; }

    public String getServerPort() { return serverPort; }
    private void setServerPort(String serverPort) { this.serverPort = serverPort; }

    public String getServerUrl() { return serverUrl; }
    private void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getUsername() { return username; }
    private void setUsername(String username) { this.username = username; }
}
