package com.kvajpoj.homie.model;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Define you model class by extending the RealmObject
public class Node extends RealmObject {

    public static final int MQTT_SENSOR = 98;
    public static final int MQTT_SWITCH = 99;
    public static final int WEBCAM = 100;

    public Node() {
        id = UUID.randomUUID().toString();
    }

    @PrimaryKey
    private String id;

    @Required // Name cannot be null
    private String name;

    private int type;
    private Date lastSeen;
    private String topic;

    //Webcam
    private String webcamUsername;
    private String webcamURL;

    public String getWebcamPassword() {
        return webcamPassword;
    }

    public void setWebcamPassword(String webcamPassword) {
        this.webcamPassword = webcamPassword;
    }

    public String getWebcamURL() {
        return webcamURL;
    }

    public void setWebcamURL(String webcamURL) {
        this.webcamURL = webcamURL;
    }

    public String getWebcamUsername() {
        return webcamUsername;
    }

    public void setWebcamUsername(String webcamUsername) {
        this.webcamUsername = webcamUsername;
    }

    private String webcamPassword;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private int position;

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

    public Date getLastSeen() {
        return lastSeen;
    }
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }

}