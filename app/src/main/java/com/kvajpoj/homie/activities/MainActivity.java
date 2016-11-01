package com.kvajpoj.homie.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.kvajpoj.homie.R;
import com.kvajpoj.homie.adapter.RecyclerViewAdapter;
import com.kvajpoj.homie.model.Node;
import com.kvajpoj.homie.model.Settings;
import com.kvajpoj.homie.service.MqttService;
import com.kvajpoj.homie.service.MqttServiceDelegate;
import com.kvajpoj.homie.touch.OnStartDragListener;
import com.kvajpoj.homie.touch.SimpleItemTouchHelperCallback;

import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;


public class MainActivity extends AppCompatActivity implements
        MqttServiceDelegate.MessageHandler,
        MqttServiceDelegate.StatusHandler,
        OnStartDragListener,
        RecyclerViewAdapter.OnItemClickListener {

    private static final String TAG = "MainActivity";
    private MqttServiceDelegate.StatusReceiver statusReceiver;
    private MqttServiceDelegate.MessageReceiver msgReceiver;

    private Logger LOG;
    private ItemTouchHelper mItemTouchHelper;
    //private StaggeredGridLayoutManager staggeredGridLayoutManagerVertical;
    private GridLayoutManager gridLayoutManager;
    private RecyclerViewAdapter myRecyclerViewAdapter;
    private Handler mTimerHandler;

    @Bind(R.id.colapse) Button colapse;
    @Bind(R.id.cntnt)   FrameLayout cntnt;
    @Bind(R.id.fab) FloatingActionButton fabButton;
    @Bind(R.id.myrecyclerview) RecyclerView myRecyclerView;
    @Bind(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setIcon(R.drawable.homie);

        LOG = Logger.getLogger(MainActivity.class);

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));


        //Init Receivers
        bindStatusReceiver();
        bindMessageReceiver();

        //Start service if not started
        LOG.debug("Starting service");
        Settings set = Settings.getInstance();
        set.reloadSettings(this);
        MqttServiceDelegate.startService(this, set.getServerUrl(), set.getServerPort(), set.getUsername(), set.getPassword());

        // Get a Realm instance for this thread
        Realm realm = Realm.getDefaultInstance();


        // get data to be displayed
        final RealmResults<Node> iotNodes = realm.where(Node.class).findAllSorted("position", Sort.DESCENDING);

        // create data adapter
        myRecyclerViewAdapter = new RecyclerViewAdapter(this, iotNodes, false, this);
        myRecyclerViewAdapter.setOnItemClickListener(this);

        // create layout manager for data
        //staggeredGridLayoutManagerVertical = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL);
        gridLayoutManager = new GridLayoutManager(this, 2);

        // assign adapter and layout manager to recycler view
        myRecyclerView.setAdapter(myRecyclerViewAdapter);
        //myRecyclerView.setLayoutManager(staggeredGridLayoutManagerVertical);
        myRecyclerView.setLayoutManager(gridLayoutManager);
        //myRecyclerView.setItemAnimator(null);



        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                switch (myRecyclerViewAdapter.getItem(position).getType()) {
                    case Node.MQTT_SENSOR:
                        return 1;
                    case Node.MQTT_SWITCH:
                        return 2;
                    case Node.WEBCAM:
                        return 2;

                }
                return -1;
            }
        });

        // touch helper
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(myRecyclerViewAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(myRecyclerView);


        mTimerHandler = new Handler();


    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            // check if we have any web cams to update
            for(int i = 0; i < myRecyclerViewAdapter.getRealmResults().size(); i++)
            {
                Node n = myRecyclerViewAdapter.getItem(i);
                if(n != null && n.getType() == Node.WEBCAM)
                {
                    View nodeView = gridLayoutManager.findViewByPosition(i);
                    if(nodeView != null)
                    {
                        RecyclerViewAdapter.ItemHolder holder = (RecyclerViewAdapter.ItemHolder) myRecyclerView.getChildViewHolder(nodeView);
                        if(holder != null && holder.getCurrentNode().getId().equals(n.getId()))
                        {
                            holder.loadImage();
                        }
                    }
                }
            }
            mTimerHandler.postDelayed(this, 2000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            //EditText editText = (EditText) findViewById(R.id.edit_message);
            //String message = editText.getText().toString();
            //intent.putExtra(EXTRA_MESSAGE, message);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        LOG.debug("OnResume");
        mTimerHandler.postDelayed(runnable, 2000);
        super.onResume();
    }


    @Override
    protected void onStop() {
        LOG.debug("OnStop");
        mTimerHandler.removeCallbacks(runnable);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LOG.debug("onDestroy");

        MqttServiceDelegate.stopService(this);

        unbindMessageReceiver();
        unbindStatusReceiver();



        super.onDestroy();
    }

    // event handlers


    /*@OnClick(R.id.publishButton)
    public void sayHi(Button btn) {

        LOG.debug("onPublish");

        MqttServiceDelegate.publish(
                MainActivity.this,
                "hello",
                publishEditView.getText().toString().getBytes());

    }

    @OnClick(R.id.btnDisconnect)
    public void disconnect(Button btn) {

        LOG.debug("onDisconnect");
        MqttServiceDelegate.stopService(this);



    }

    @OnClick(R.id.btnConnect)
    public void connect(Button btn) {

        LOG.debug("onConnect");
        MqttServiceDelegate.startService(this);



    }
*/
    private EditText nodeName = null;
    private EditText nodeTopic = null;
    private EditText webcamUsername = null;
    private EditText webcamPassword = null;
    private EditText webcamURL = null;
    private int nodeType = Node.MQTT_SENSOR;
    private View positiveAction = null;
    private RadioButton nodeSensor = null;
    private RadioButton nodeSwitch = null;
    private RadioButton nodeWebcam = null;
    private RadioGroup radios = null;
    private LinearLayout itemSensor = null;
    private LinearLayout itemWebcam = null;



    @SuppressWarnings("ConstantConditions")
    public void showNodeDialog(Node node) {

        String title = "Create new node";
        if (node != null) {
            title = "Edit " + node.getName();
        }


        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(title)
                .customView(R.layout.new_node_dialog, true)
                .positiveText("Save")
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        //showToast("Password: " + passwordInput.getText().toString());
                        LOG.debug("Creating new node!");

                        try {
                            Realm realm = Realm.getDefaultInstance();

                            Node n = new Node();
                            n.setName(nodeName.getText().toString());
                            n.setTopic(nodeTopic.getText().toString());
                            n.setWebcamPassword(webcamPassword.getText().toString());
                            n.setWebcamURL(webcamURL.getText().toString());
                            n.setWebcamUsername(webcamUsername.getText().toString());

                            if (nodeSensor.isChecked() == true) n.setType(Node.MQTT_SENSOR);
                            if (nodeSwitch.isChecked() == true) n.setType(Node.MQTT_SWITCH);
                            if (nodeWebcam.isChecked() == true) n.setType(Node.WEBCAM);

                            Number newPostion = 1;

                            RealmResults<Node> nodes = realm.where(Node.class).findAll();
                            if (nodes.size() > 0) {
                                newPostion = nodes.max("position").intValue();
                            } else {
                                newPostion = 0;
                            }

                            int pos = newPostion.intValue() + 1;
                            n.setPosition(pos);

                            realm.beginTransaction();
                            realm.copyToRealm(n);
                            realm.commitTransaction();
                            realm.close();

                            myRecyclerViewAdapter.notifyDataSetChanged();


                        } catch (Exception ex) {
                            LOG.error(ex.toString());
                        }
                    }
                }).build();

        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);

        itemSensor = (LinearLayout) dialog.getCustomView().findViewById(R.id.item_sensor);
        itemWebcam = (LinearLayout) dialog.getCustomView().findViewById(R.id.item_webcam);


        radios = (RadioGroup) dialog.getCustomView().findViewById(R.id.radioType);
        radios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                LOG.debug("Radio checked " + checkedId);

                switch (checkedId){

                    case R.id.node_sensor:
                        itemSensor.setVisibility(View.VISIBLE);
                        itemWebcam.setVisibility(View.GONE);

                        break;
                    case R.id.node_webcam:
                        itemSensor.setVisibility(View.GONE);
                        itemWebcam.setVisibility(View.VISIBLE);
                        break;
                    case R.id.node_switch:
                        itemSensor.setVisibility(View.GONE);
                        itemWebcam.setVisibility(View.GONE);
                        break;
                }
            }
        });


        nodeSensor = (RadioButton) dialog.getCustomView().findViewById(R.id.node_sensor);

        webcamPassword =  (EditText) dialog.getCustomView().findViewById(R.id.node_webcam_password);
        webcamUsername =  (EditText) dialog.getCustomView().findViewById(R.id.node_webcam_username);
        webcamURL =  (EditText) dialog.getCustomView().findViewById(R.id.node_webcam_url);


        nodeSwitch = (RadioButton) dialog.getCustomView().findViewById(R.id.node_switch);
        nodeWebcam = (RadioButton) dialog.getCustomView().findViewById(R.id.node_webcam);


        nodeName = (EditText) dialog.getCustomView().findViewById(R.id.node_name);
        nodeName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        nodeTopic = (EditText) dialog.getCustomView().findViewById(R.id.node_topic);
        nodeTopic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });




        // Toggling the show password CheckBox will mask or unmask the password input EditText
        //CheckBox checkbox = (CheckBox) dialog.getCustomView().findViewById(R.id.showPassword);
        //checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        //    @Override
        //    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //        passwordInput.setInputType(!isChecked ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
        //        passwordInput.setTransformationMethod(!isChecked ? PasswordTransformationMethod.getInstance() : null);
        //    }
        //});

        //int widgetColor = ThemeSingleton.get().widgetColor;
        //MDTintHelper.setTint(checkbox,
        //        widgetColor == 0 ? ContextCompat.getColor(this, R.color.material_teal_a400) : widgetColor);

        //MDTintHelper.setTint(passwordInput,
        //        widgetColor == 0 ? ContextCompat.getColor(this, R.color.material_teal_a400) : widgetColor);

        dialog.show();
        positiveAction.setEnabled(false); // disabled by default
    }


    @OnClick(R.id.fab)
    public void onClick(FloatingActionButton fab) {
        //Snackbar.make(fab, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show();
        //Intent intent = new Intent(this, NewNodeActivity.class);
        //EditText editText = (EditText) findViewById(R.id.edit_message);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        //startActivity(intent);
        showNodeDialog(null);


    }

    private void bindMessageReceiver() {
        msgReceiver = new MqttServiceDelegate.MessageReceiver();
        msgReceiver.registerHandler(this);
        registerReceiver(msgReceiver,
                new IntentFilter(MqttService.MQTT_MSG_RECEIVED_INTENT));
    }

    private void unbindMessageReceiver() {
        if (msgReceiver != null) {
            msgReceiver.unregisterHandler(this);
            unregisterReceiver(msgReceiver);
            msgReceiver = null;
        }
    }

    private void bindStatusReceiver() {
        statusReceiver = new MqttServiceDelegate.StatusReceiver();
        statusReceiver.registerHandler(this);
        registerReceiver(statusReceiver, new IntentFilter(MqttService.MQTT_STATUS_INTENT));
    }

    private void unbindStatusReceiver() {
        if (statusReceiver != null) {
            statusReceiver.unregisterHandler(this);
            unregisterReceiver(statusReceiver);
            statusReceiver = null;
        }
    }

    private String getCurrentTimestamp() {
        return new Timestamp(new Date().getTime()).toString();
    }

    @Override
    public void handleMessage(String topic, byte[] payload) {
        String message = new String(payload);

        LOG.debug("handleMessage: topic=" + topic + ", message=" + message);

        //if (timestampView != null) timestampView.setText("When: " + getCurrentTimestamp());
        //if (topicView != null) topicView.setText("Topic: " + topic);
        //if (messageView != null) messageView.setText("Message: " + message);
    }

    @Override
    public void handleStatus(MqttService.MQTTConnectionStatus status, String reason) {
        LOG.debug("handleStatus: status=" + status + ", reason=" + reason);
        //if (statusView != null)
        //    statusView.setText("Status: " + status.toString() + " (" + reason + ")");
    }

    @Override
    public void onItemClick(RecyclerViewAdapter.ItemHolder item, int position) {
        LOG.debug("item clicked");
    }

    @Override
    public void onItemEditClick(RecyclerViewAdapter.ItemHolder item, int position) {
        LOG.debug("item edit clicked " + item.getItemName());

    }

    @Override
    public void onDeleteClick(final RecyclerViewAdapter.ItemHolder item, int position) {
        LOG.debug("item delete clicked " + item.getItemName());
        new MaterialDialog.Builder(this)
                .title(android.R.string.dialog_alert_title)
                .content("delete this node?")
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        try {

                            RealmResults<Node> nodes = myRecyclerViewAdapter.getRealmResults();
                            LOG.debug(item.getId());
                            Node n = nodes.where().equalTo("id", item.getCurrentNode().getId()).findFirst();
                            //if (n != null) n.removeFromRealm();
                            if (n != null) n.deleteFromRealm();
                            realm.commitTransaction();
                            myRecyclerViewAdapter.notifyDataSetChanged();
                        }
                        catch (Exception e){
                            realm.cancelTransaction();
                            LOG.error(e.getMessage());
                        }
                        finally {
                            realm.close();
                        }


                    }
                })
                .show();
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onStopDrag(RecyclerView.ViewHolder viewHolder) {

    }


    @OnClick(R.id.colapse)
    public void onClick() {
        if (cntnt.getVisibility() == View.VISIBLE) cntnt.setVisibility(View.GONE);
        else cntnt.setVisibility(View.VISIBLE);

    }
}
