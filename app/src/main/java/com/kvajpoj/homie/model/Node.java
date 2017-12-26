package com.kvajpoj.homie.model;

import com.daimajia.swipe.SwipeLayout;
import com.kvajpoj.homie.adapter.RecyclerViewAdapter;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Define you model class by extending the RealmObject
public class Node extends RealmObject {

    public static final int MQTT_TYPE_UNKNOWN = 0;
    public static final int MQTT_SENSOR = 98;
    public static final int MQTT_SWITCH = 99;
    public static final int WEBCAM = 100;
    public static final int MQTT_CUSTOM_NODE = 101;

    @PrimaryKey private String id;
    @Required private String name = "";
    private Homie homie;
    private int type = MQTT_TYPE_UNKNOWN;
    private int position;

    private String subtopic = "";
    private String topic = "";
    private String value = "";
    private String unit = "";
    private String properties = "";
    private String webcamPassword = "";
    private String webcamUsername = "";
    private String webcamURL = "";

    @Ignore
    private long lastUpdateTime = 0;

    public Node() {
        id = UUID.randomUUID().toString();
    }

    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public String getSubtopic() { return subtopic; }
    public void setSubtopic(String subtopic) { this.subtopic = subtopic; }

    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getWebcamPassword() { return webcamPassword; }
    public void setWebcamPassword(String webcamPassword) { this.webcamPassword = webcamPassword; }

    public String getWebcamURL() { return webcamURL; }
    public void setWebcamURL(String webcamURL) { this.webcamURL = webcamURL; }

    public String getWebcamUsername() { return webcamUsername; }
    public void setWebcamUsername(String webcamUsername) { this.webcamUsername = webcamUsername; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Homie getHomie() { return homie; }
    public void setHomie(Homie homie) { this.homie = homie; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
    public String getTopic() {
       return topic;
    }

}