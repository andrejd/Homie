package com.kvajpoj.homie.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
//import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
//import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

//import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MqttService extends Service implements MqttCallback {

    private static final Logger LOG = Logger.getLogger(MqttService.class);
    //public static final String APP_ID = "com.dalelane.mqtt";

    // constants used to notify the Activity UI of received messages
    public static final String MQTT_MSG_RECEIVED_INTENT = "com.kvajpoj.services.MSGRECVD";
    public static final String MQTT_MSG_RECEIVED_TOPIC = "com.kvajpoj.services.MSGRECVD_TOPIC";
    public static final String MQTT_MSG_RECEIVED_MSG = "com.kvajpoj.services.MSGRECVD_MSGBODY";

    // constants used to notify the Service of messages to send
    public static final String MQTT_PUBLISH_MSG_INTENT = "com.kvajpoj.services.SENDMSG";
    public static final String MQTT_PUBLISH_MSG_TOPIC  = "com.kvajpoj.services.SENDMSG_TOPIC";
    public static final String MQTT_PUBLISH_MSG    = "com.kvajpoj.services.SENDMSG_MSG";

    // constants used to tell the Activity UI the connection status
    public static final String MQTT_STATUS_INTENT = "com.kvajpoj.services.STATUS";
    public static final String MQTT_STATUS_CODE    = "com.kvajpoj.services.STATUS_CODE";
    public static final String MQTT_STATUS_MSG    = "com.kvajpoj.services.STATUS_MSG";

    // constants used to tell service what to do
    public static final String MQTT_CONNECT_CLIENT_INTENT       = "com.kvajpoj.services.MQTT_CONNECT_CLIENT_INTENT";
    public static final String MQTT_DISCONNECT_CLIENT_INTENT    = "com.kvajpoj.services.MQTT_DISCONNECT_CLIENT_INTENT";



    // constant used internally to schedule the next ping event
    public static final String MQTT_PING_ACTION = "com.kvajpoj.services.PING";

    // MQTT constants
    public static final int MAX_MQTT_CLIENTID_LENGTH = 22;

    // constants used to define MQTT connection status
    public enum MQTTConnectionStatus {
        INITIAL,                            // initial status
        CONNECTING,                         // attempting to connect
        CONNECTED,                          // connected
        NOTCONNECTED_WAITINGFORINTERNET,    // can't connect because the phone does not have Internet access
        NOTCONNECTED_USERDISCONNECT,        // user has explicitly requested disconnection
        NOTCONNECTED_DATADISABLED,          // can't connect because the user has disabled data access
        NOTCONNECTED_UNKNOWNREASON          // failed to connect for some reason
    }

    // status of MQTT client connection
    private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;
    private Timestamp connectionStatusChangeTime;


    // taken from preferences
    //    host name of the server we're receiving push notifications from
    private String                  brokerHostName       = "";
    private List<String>            topicNames           = new ArrayList<>();
    private boolean         		cleanStart           = false;
    private String 					username			 = "guest";
    private char[]					password			 = "guest".toCharArray();
    private short                   keepAliveSeconds     = 20 * 60;
    private int                     brokerPortNumber     = 1883;
    private int[]                   qualitiesOfService   = {0};
    private String                  mqttClientId         = null;


    private volatile IMqttAsyncClient mqttClient = null;

    // receiver that notifies the Service when the phone gets data connection
    private NetworkConnectionIntentReceiver netConnReceiver;

    // receiver that wakes the Service up when it's time to ping the server
    private PingSender pingSender;
    private ExecutorService executor;

    // see http://developer.android.com/guide/topics/fundamentals.html#lcycles
    @Override
    public void onCreate() {
        super.onCreate();
        changeStatus(MQTTConnectionStatus.INITIAL);
        executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {

        if(intent == null) {
            LOG.debug("onStartCommand: intent=null, flags=" + flags + ", startId=" + startId);
        }
        else {
            LOG.debug("onStartCommand: intent="+intent+", flags="+flags+", startId="+ startId);

            if(intent.getExtras() != null) {
                brokerHostName = intent.getExtras().getString("serverUrl", "");
                brokerPortNumber = Integer.parseInt(intent.getExtras().getString("serverPort", "1883"));
                username = intent.getExtras().getString("username", "guest");
                if (username.isEmpty()) username = "guest";
                String pass = intent.getExtras().getString("password", "guest");
                if (pass.isEmpty()) pass = "guest";
                password = pass.toCharArray();
                topicNames = intent.getExtras().getStringArrayList("topics");

                if( topicNames != null ) {
                    for (String topic : topicNames) {
                        LOG.debug("onStartCommand: Subscribing to  topic: " + topic);
                    }
                }

            }
        }
        doStart(intent, startId);
        //return START_STICKY;
        return START_REDELIVER_INTENT;
    }

    private void doStart(final Intent intent, final int startId){

        // here we create client if needed
        initMqttClient();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        });
    }

    synchronized void handleStart(Intent intent, int startId) {

        // if client was not created, we stop service
        if (mqttClient == null) {
            // we were unable to define the MQTT client connection, so we stop
            //  immediately - there is nothing that we can do
            LOG.debug("handleStart: mqttClient == null");

            stopSelf();
            return;
        }

        if (netConnReceiver == null) {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            registerReceiver(netConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        if (pingSender == null) {
            pingSender = new PingSender();
            registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
        }

        if (connectionStatus == MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT) {
            // When calling startService in multiple activities, onStartCommand()
            // is called when activities are switched. Thus the service would connect
            // automatically even though the user might have requested the disconnect.
            return;
        }

        if (!isBackgroundDataEnabled()) // respect the user's request not to use data!
        {
            changeStatus(MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);
            broadcastServiceStatus("Not connected - background data disabled @ " + getConnectionChangeTimestamp());
            return;
        }

        if (!isConnected()) {

            changeStatus(MQTTConnectionStatus.CONNECTING);
            broadcastServiceStatus("Connecting to broker");

            if (isOnline()) {

                if (connectToBroker()) {
                    subscribeToTopic();
                }

            } else {
                changeStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
                broadcastServiceStatus("Waiting for network connection");
            }
        }



        //if(!handleStartAction(intent)){
        //    rebroadcastStatus();
        //}
        handleStartAction(intent);
    }

    private boolean handleStartAction(Intent intent){

        String action = intent.getAction();

        if(action == null){
            return false;
        }

        if(action.equalsIgnoreCase(MQTT_PUBLISH_MSG_INTENT)){
            LOG.debug("handleStartAction: action == MQTT_PUBLISH_MSG_INTENT");
            handlePublishMessageIntent(intent);
        }

        return true;
    }

    @Override
    public void onDestroy() {

        disconnectFromBroker();

        //changeStatus(MQTTConnectionStatus.INITIAL);
        // inform the app that the app has successfully disconnected
        //broadcastServiceStatus("Disconnected @ " + getConnectionChangeTimestamp());

        /*if (mBinder != null) {

            mBinder.close();
            mBinder = null;
        }*/
        super.onDestroy();
    }


    /************************************************************************/
        /*    METHODS - broadcasts and notifications                            */

    /**
     * ********************************************************************
     */

    // methods used to notify the Activity UI of something that has happened
    //  so that it can be updated to reflect status and the data received
    //  from the server
    private void broadcastServiceStatus(String statusDescription) {
        // inform the app (for times when the Activity UI is running /
        //   active) of the current MQTT connection status so that it
        //   can update the UI accordingly
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_STATUS_INTENT);
        broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
        broadcastIntent.putExtra(MQTT_STATUS_CODE, connectionStatus.ordinal());
        sendBroadcast(broadcastIntent);
    }

    private void broadcastReceivedMessage(String topic, byte[] message) {
        // pass a message received from the MQTT server on to the Activity UI
        //   (for times when it is running / active) so that it can be displayed
        //   in the app GUI
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
        sendBroadcast(broadcastIntent);
    }

    // methods used to notify the user of what has happened for times when
    //  the app Activity UI isn't running

    private void notifyUser(String alert, String title, String body) {
        /*NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher, alert,
                System.currentTimeMillis());
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = Color.MAGENTA;
        Intent notificationIntent = new Intent(this, MQTTNotifier.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(this, title, body, contentIntent);
        nm.notify(MQTT_NOTIFICATION_UPDATE, notification);*/
        LOG.debug("notifyUser: alert="+alert+", title="+title+", body="+body);
    }


    /************************************************************************/
        /*    METHODS - binding that allows access from the Actitivy            */
    /**
     * ********************************************************************
     */

    // trying to do local binding while minimizing leaks - code thanks to
    //   Geoff Bruckner - which I found at
    //   http://groups.google.com/group/cw-android/browse_thread/thread/d026cfa71e48039b/c3b41c728fedd0e7?show_docid=c3b41c728fedd0e7

    //private LocalBinder<MqttService> mBinder = new LocalBinder<MqttService>(this);

    @Override
    public IBinder onBind(Intent intent) {
        //return mBinder;
        return null;
    }

    /*public class LocalBinder<S> extends Binder {
        private WeakReference<S> mService;

        public LocalBinder(S service) {
            mService = new WeakReference<S>(service);
        }

        public S getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }
    }*/

    //
    // public methods that can be used by Activities that bind to the Service
    //

    //public MQTTConnectionStatus getConnectionStatus() {
    //    return connectionStatus;
    //}

    /*public void rebroadcastStatus() {
        String status = "";

        switch (connectionStatus) {
            case INITIAL:
                //App.log.warn("Please wait... " + brokerHostName);
                status = "Please wait";
                break;
            case CONNECTING:
                //App.log.warn("Connecting...");
                status = "Connecting...";
                break;
            case CONNECTED:
                //App.log.warn("Connected");
                status = "Connected";
                break;
            case NOTCONNECTED_UNKNOWNREASON:
                //App.log.warn("Disconnected");
                status = "Not connected - waiting for network connection";
                break;
            case NOTCONNECTED_USERDISCONNECT:
                //App.log.warn("Disconnected");
                status = "Disconnected";
                break;
            case NOTCONNECTED_DATADISABLED:
                //App.log.warn("Not connected - background data disabled");
                status = "Not connected - background data disabled";
                break;
            case NOTCONNECTED_WAITINGFORINTERNET:
                //App.log.warn("Unable to connect: " + brokerHostName);
                status = "Unable to connect";
                break;
        }

        //
        // inform the app that the Service has successfully connected
        broadcastServiceStatus(status);
    }*/

    /*public void disconnect() {
        disconnectFromBroker();

        // set status

        changeStatus(MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT);

        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");
    }*/


    /************************************************************************/
        /*    METHODS - MQTT methods inherited from MQTT classes                */

    /**
     * ********************************************************************
     */



    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {



    }


    /*
     * callback - method called when we no longer have a connection to the
     *  message broker server
     */
    @Override
    public void connectionLost(Throwable cause)  {
        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();


        //
        // have we lost our data connection?
        //

        if (!isOnline()) {
            changeStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);

            // inform the app that we are not connected any more
            broadcastServiceStatus("Connection lost - no network connection");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we are no longer able to receive messages
            notifyUser("Connection lost - no network connection",
                    "MQTT", "Connection lost - no network connection");

            //
            // wait until the phone has a network connection again, when we
            //  the network connection receiver will fire, and attempt another
            //  connection to the broker
        } else {
            //
            // we are still online
            //   the most likely reason for this connectionLost is that we've
            //   switched from wifi to cell, or vice versa
            //   so we try to reconnect immediately
            //

            changeStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

            // inform the app that we are not connected any more, and are
            //   attempting to reconnect
            broadcastServiceStatus("Connection lost - reconnecting...");

            // try to reconnect
            //if (connectToBroker()) {
            //    subscribeToTopic(topicName);
            //}
        }

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }


    /*
     *   callback - called when we receive a message from the server
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        LOG.debug("messageArrived: topic=" + topic + ", message=" + new String(message.getPayload()));
        broadcastReceivedMessage(topic, message.getPayload());

        // receiving this message will have kept the connection alive for us, so
        //  we take advantage of this to postpone the next scheduled ping
        scheduleNextPing();

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }


    /************************************************************************/
        /*    METHODS - wrappers for some of the MQTT methods that we use       */

    /**
     * ********************************************************************
     */

        /*
         * Create a client connection object that defines our connection to a
         *   message broker server
         */
    private void initMqttClient() {

        IMqttToken token;

        if(mqttClient != null) return;

        String mqttConnSpec = "tcp://" + brokerHostName + ":" + brokerPortNumber;
        LOG.debug("initMqttClient " + mqttConnSpec);

        try {

            mqttClient = new MqttAsyncClient(mqttConnSpec, getClientId(), new MemoryPersistence());
            mqttClient.setCallback(this);

        } catch (MqttException e) {
            // something went wrong!
            mqttClient = null;
            changeStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

            broadcastServiceStatus("Invalid connection parameters");
            notifyUser("Unable to connect", "MQTT", "Unable to connect");
        }
    }

    /*
     * (Re-)connect to the message broker
     */
    private boolean connectToBroker() {

        IMqttToken token;
        LOG.debug("connectToBroker");

        try {

            if(mqttClient.isConnected())  return false;


            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(cleanStart);
            options.setKeepAliveInterval(keepAliveSeconds);
            options.setUserName(username);
            options.setPassword(password);
            // try to connect
            token = mqttClient.connect(options);
            token.waitForCompletion(3500);
            mqttClient.setCallback(this);
            token.waitForCompletion(5000);

            // inform the app that the app has successfully connected
            changeStatus(MQTTConnectionStatus.CONNECTED);
            broadcastServiceStatus("Connected to broker");

            // we are connected


            // we need to wake up the phone's CPU frequently enough so that the
            //  keep alive messages can be sent
            // we schedule the first one of these now
            scheduleNextPing();

            return true;

        } catch (MqttException e) {

            // something went wrong!
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    LOG.error(e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    LOG.error(e.getMessage());
                    break;
                default:
                    LOG.error(e.getMessage());
            }

            changeStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
            broadcastServiceStatus("Unable to connect");

            notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");

            // if something has failed, we wait for one keep-alive period before
            //   trying again
            // in a real implementation, you would probably want to keep count
            //  of how many times you attempt this, and stop trying after a
            //  certain number, or length of time - rather than keep trying
            //  forever.
            // a failure is often an intermittent network issue, however, so
            //  some limited retry is a good idea
            scheduleNextPing();

            return false;
        }
    }

    /*
     * Send a request to the message broker to be sent messages published with
     *  the specified topic name. Wildcards are allowed.
     */
    private void subscribeToTopic() {

        IMqttToken token;
        LOG.debug("subscribeToTopics");

        boolean subscribed = false;

        if (isConnected()) {

            try {
                String[] topics = new String[topicNames.size()];
                topics = topicNames.toArray(topics);

                token = mqttClient.subscribe(topics, qualitiesOfService);
                token.waitForCompletion();

                subscribed = true;

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        else{
            LOG.error("Unable to subscribe as we are not connected");
        }


        if (!subscribed) {

            broadcastServiceStatus("Unable to subscribe");
            notifyUser("Unable to subscribe", "MQTT", "Unable to subscribe");
        }
    }

    /*
     * Terminates a connection to the message broker.
     */
    private void disconnectFromBroker() {
        // if we've been waiting for an Internet connection, this can be
        //  cancelled - we don't need to be told when we're connected now
        IMqttToken token;

        LOG.debug("disconnectFromBroker");

        // if we've been waiting for an Internet connection, this can be
        //  cancelled - we don't need to be told when we're connected now
        try
        {
            if (netConnReceiver != null)
            {
                unregisterReceiver(netConnReceiver);
                netConnReceiver = null;
            }

            if (pingSender != null)
            {
                unregisterReceiver(pingSender);
                pingSender = null;
            }
        }
        catch (Exception eee) {
            // probably because we hadn't registered it
            LOG.error("unregister failed", eee);
        }


        try {
            if (mqttClient != null && mqttClient.isConnected()){
                token = mqttClient.disconnect();
                token.waitForCompletion(2000);
            }
        }
        catch (MqttException e) {
            LOG.error("disconnect failed - mqtt exception", e);
            e.printStackTrace();
        }
        finally {

            changeStatus(MQTTConnectionStatus.INITIAL);
            // inform the app that the app has successfully disconnected
            broadcastServiceStatus("Disconnected @ "+getConnectionChangeTimestamp());
            mqttClient = null;



        }

        // we can now remove the ongoing notification that warns users that
        //  there was a long-running ongoing service running
        //NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //nm.cancelAll();
    }

    private String getClientId()
    {
        // generate a unique client id if we haven't done so before, otherwise
        //   re-use the one we already have

        if (mqttClientId == null)
        {
            mqttClientId = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            // truncate - MQTT spec doesn't allow client ids longer than 23 chars
            if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
                mqttClientId = mqttClientId.substring(0, MAX_MQTT_CLIENTID_LENGTH);
            }
        }
        LOG.debug("ClientID " + mqttClientId);
        return mqttClientId;
    }

    private void changeStatus(MQTTConnectionStatus newStatus){
        LOG.debug("changeStatus -> "+newStatus.toString());
        connectionStatus = newStatus;
        connectionStatusChangeTime = new Timestamp(new Date().getTime());
    }

    private void handlePublishMessageIntent(Intent intent){
        IMqttToken token;

        LOG.debug("handlePublishMessageIntent: intent="+intent);

        boolean isOnline = isOnline();
        boolean isConnected = isConnected();

        if(!isOnline || !isConnected){
            LOG.error("handlePublishMessageIntent: isOnline()="+isOnline+", isConnected()="+isConnected);
            return;
        }

        byte[] payload = intent.getByteArrayExtra(MQTT_PUBLISH_MSG);
        String topic = intent.getStringExtra(MQTT_PUBLISH_MSG_TOPIC);

        try
        {
            LOG.debug("Publishing to topic " + topic);
            token = mqttClient.publish(topic, new MqttMessage(payload));
            token.waitForCompletion(2000);
        }
        catch(MqttException e)
        {
            LOG.error(e.getMessage());

        }
    }

    /*
     * Checks if the MQTT client thinks it has an active connection
     */
    private boolean isConnected() {
        return ((mqttClient != null) && (mqttClient.isConnected()));
    }

    /*
     * Called in response to a change in network connection - after losing a
     *  connection to the server, this allows us to wait until we have a usable
     *  data connection again
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

        private final Logger LOG = Logger.getLogger(NetworkConnectionIntentReceiver.class);

        @Override
        public void onReceive(Context ctx, Intent intent)
        {

            if(this.isInitialStickyBroadcast()) return;


            IMqttToken token;
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();



            if (isOnline()) {
                LOG.warn("onReceive: isOnline()=" + isOnline() + ", isConnected()=" + isConnected());
                //here we reconnect
                doStart(null, -1);
            }
            else{

                if (mqttClient != null && mqttClient.isConnected()){
                    LOG.warn("onReceive: isOnline()=" + isOnline() + ", isConnected()=" + isConnected());

                    try {
                        token = mqttClient.disconnect();
                        token.waitForCompletion(2000);
                    }
                    catch (MqttException e) {
                        LOG.error("disconnect failed - mqtt exception", e);
                        e.printStackTrace();
                    }
                    finally {
                        mqttClient = null;
                        changeStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
                        broadcastServiceStatus("Waiting for network connection");
                    }
                }
            }


            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }


    /*
     * Schedule the next time that you want the phone to wake up and ping the
     *  message broker server
     */
    private void scheduleNextPing() {
        // When the phone is off, the CPU may be stopped. This means that our
        //   code may stop running.
        // When connecting to the message broker, we specify a 'keep alive'
        //   period - a period after which, if the client has not contacted
        //   the server, even if just with a ping, the connection is considered
        //   broken.
        // To make sure the CPU is woken at least once during each keep alive
        //   period, we schedule a wake up to manually ping the server
        //   thereby keeping the long-running connection open
        // Normally when using this Java MQTT client library, this ping would be
        //   handled for us.
        // Note that this may be called multiple times before the next scheduled
        //   ping has fired. This is good - the previously scheduled one will be
        //   cancelled in favour of this one.
        // This means if something else happens during the keep alive period,
        //   (e.g. we receive an MQTT message), then we start a new keep alive
        //   period, postponing the next ping.

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // in case it takes us a little while to do this, we try and do it
        //  shortly before the keep alive period expires
        // it means we're pinging slightly more frequently than necessary
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);
        //TODO: Try setInexactRepeating(): See http://developer.android.com/training/efficient-downloads/regular_updates.html#OptimizedPolling
        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);

        LOG.debug("Scheduled ping for: " + wakeUpTime.getTime());

    }


    /*
     * Used to implement a keep-alive protocol at this Service level - it sends
     *  a PING message to the server, then schedules another ping after an
     *  interval defined by keepAliveSeconds
     */
    public class PingSender extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Note that we don't need a wake lock for this method (even though
            //  it's important that the phone doesn't switch off while we're
            //  doing this).
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            //  long as the alarm receiver's onReceive() method is executing.
            //  This guarantees that the phone will not sleep until you have
            //  finished handling the broadcast."
            // This is good enough for our needs.

            if(isOnline() && !isConnected())
            {
                LOG.warn("onReceive: isOnline()="+isOnline()+", isConnected()="+isConnected());
                doStart(null, -1);
            }
            else if(!isOnline()){
                LOG.debug("Waiting for network to come online again");
            }
            else
            {
                try
                {
                    final String TOPIC_PING = "PING";

                    try
                    {
                        mqttClient.publish(TOPIC_PING, new byte[]{0},1,false);
                    }
                    catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException e)
                    {
                        e.printStackTrace();
                    }
                    catch (org.eclipse.paho.client.mqttv3.MqttException e)
                    {
                        throw new MqttException(e);
                    }
                }
                catch (MqttException e)
                {
                    IMqttToken token;
                    // if something goes wrong, it should result in connectionLost
                    //  being called, so we will handle it there
                    LOG.error("ping failed - MQTT exception", e);

                    // assume the client connection is broken - trash it
                    try {
                        token = mqttClient.disconnect();
                        token.waitForCompletion(2000);
                    }
                    catch (MqttException e2)
                    {
                        LOG.error("disconnect failed - mqtt exception", e2);
                    }
                    finally {
                        mqttClient = null;
                    }

                    // reconnect
                    LOG.warn("onReceive: MqttException="+e);
                    doStart(null, -1);
                }
            }

            // start the next keep alive period
            scheduleNextPing();

        }
    }


    /************************************************************************/
        /*   APP SPECIFIC - stuff that would vary for different uses of MQTT    */
    /**
     * ********************************************************************
     */

    //  apps that handle very small amounts of data - e.g. updates and
    //   notifications that don't need to be persisted if the app / phone
    //   is restarted etc. may find it acceptable to store this data in a
    //   variable in the Service
    //  that's what I'm doing in this sample: storing it in a local hashtable
    //  if you are handling larger amounts of data, and/or need the data to
    //   be persisted even if the app and/or phone is restarted, then
    //   you need to store the data somewhere safely
    //  see http://developer.android.com/guide/topics/data/data-storage.html
    //   for your storage options - the best choice depends on your needs

    // stored internally

//    private Hashtable<String, String> dataCache = new Hashtable<>();
//
//    private boolean addReceivedMessageToStore(String key, String value) {
//        String previousValue;
//
//        if (value.length() == 0) {
//            previousValue = dataCache.remove(key);
//        } else {
//            previousValue = dataCache.put(key, value);
//        }
//
//        // is this a new value? or am I receiving something I already knew?
//        //  we return true if this is something new
//        return ((previousValue == null) || (!previousValue.equals(value)));
//    }

    // provide a public interface, so Activities that bind to the Service can
    //  request access to previously received messages

//    public void rebroadcastReceivedMessages() {
//        Enumeration<String> e = dataCache.keys();
//        while (e.hasMoreElements()) {
//            String nextKey = e.nextElement();
//            String nextValue = dataCache.get(nextKey);
//
//            broadcastReceivedMessage(nextKey, nextValue);
//        }
//    }


    /************************************************************************/
        /*    METHODS - internal utility methods                                */

    /**
     * ********************************************************************
     */

//    private String generateClientId() {
//        // generate a unique client id if we haven't done so before, otherwise
//        //   re-use the one we already have
//
//        if (mqttClientId == null) {
//            // generate a unique client ID - I'm basing this on a combination of
//            //  the phone device id and the current timestamp
//            String timestamp = "" + (new Date()).getTime();
//            String android_id = Settings.System.getString(getContentResolver(),
//                    Settings.Secure.ANDROID_ID);
//            mqttClientId = timestamp + android_id;
//
//            // truncate - MQTT spec doesn't allow client ids longer than 23 chars
//            if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
//                mqttClientId = mqttClientId.substring(0, MAX_MQTT_CLIENTID_LENGTH);
//            }
//        }
//
//        return mqttClientId;
//    }

    private String getConnectionChangeTimestamp(){
        return connectionStatusChangeTime.toString();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null) &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected();
    }

    @SuppressWarnings("deprecation")
    private boolean isBackgroundDataEnabled(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        //Only on pre-ICS platforms, backgroundDataSettings API exists
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            return cm.getBackgroundDataSetting();
        }

        //On ICS platform and higher, define BackgroundDataSetting by checking if
        //phone is online
        return isOnline();
    }

}






























