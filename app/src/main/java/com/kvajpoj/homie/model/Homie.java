package com.kvajpoj.homie.model;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by andrej on 02/11/2016.
 */

public class Homie extends RealmObject {

    /*
        devices/686f6d6965/$online → true
        devices/686f6d6965/$name → Bedroom temperature sensor
        devices/686f6d6965/$localip → 192.168.0.10
        devices/686f6d6965/$signal → 72
        devices/686f6d6965/$fwname → 1.0.0
        devices/686f6d6965/$fwversion → 1.0.0
        devices/686f6d6965/$nodes → temperature:temperature,humidity:humidity
    */




    @PrimaryKey
    private String deviceId;

    private String name = "";
    private boolean online = false;
    private String localIp = "";
    private int signal = 0;
    private int uptime = 0;
    private String fwName = "";
    private String fwVersion = "";
    private RealmList<Node> nodes;
    private String batteryVoltage = "";
    private String batteryPercentage = "";

    public String getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(String batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public String getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(String batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isOnline() {
        return online;
    }
    public void setOnline(boolean online) {
        this.online = online;
    }
    public boolean getOnline() {
        return this.online;
    }
    public String getLocalIp() {
        return localIp;
    }
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }
    public int getSignal() {
        return signal;
    }
    public void setSignal(int signal) {
        this.signal = signal;
    }
    public int getUptime() {
        return uptime;
    }
    public void setUptime(int uptime) {
        this.uptime = uptime;
    }
    public String getFwName() {
        return fwName;
    }
    public void setFwName(String fwName) {
        this.fwName = fwName;
    }
    public String getFwVersion() {
        return fwVersion;
    }
    public void setFwVersion(String fwVersion) {
        this.fwVersion = fwVersion;
    }
    public RealmList<Node> getNodes() {
        return nodes;
    }
    public void setNodes(RealmList<Node> nodes) {
        this.nodes = nodes;
    }


    public static String getBaseTopic(String msg){
        String[] parts = msg.split("/");
        if ( parts.length > 0) return parts[0];
        return "";
    }

    public static String getDeviceId(String msg){
        String[] parts = msg.split("/");
        if ( parts.length > 1) return parts[1];
        return "";
    }

    public static String getDeviceProperty(String msg){
        // homie/686f6d6965/$online → true
        String[] parts = msg.split("/");
        if ( parts.length > 2 && parts[2].indexOf("$") == 0) return parts[2];
        return "";
    }

    public static String getNode(String msg){
        //homie/686f6d6965/temperature/$type → temperature
        String[] parts = msg.split("/");
        if ( parts.length > 2 && !parts[2].contains("$")) return parts[2];
        return "";
    }
    public static String getNodeProperty(String msg){
        //homie/686f6d6965/temperature/$type → temperature
        String[] parts = msg.split("/");
        if ( parts.length > 3 && !parts[2].contains("$")) return parts[3];
        return "";
    }

    public static boolean isDevicePropertySubTopic(String property, String topic){
        //homie/686f6d6965/temperature/$type → temperature
        String[] parts = topic.split("/");
        return ( parts.length > 3 && parts[3].contains(property) );
    }

    public static String getNodeBaseTopic(String msg){
        //homie/686f6d6965/temperature/$type → temperature
        String[] parts = msg.split("/");
        if ( parts.length >= 3 && !parts[2].contains("$")) return parts[0] + "/" + parts[1] + "/" + parts[2];
        return "";
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
