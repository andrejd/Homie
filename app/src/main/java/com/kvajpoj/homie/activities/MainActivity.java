package com.kvajpoj.homie.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.kvajpoj.homie.R;
import com.kvajpoj.homie.adapter.RecyclerViewAdapter;
import com.kvajpoj.homie.common.DrawableHelper;
import com.kvajpoj.homie.common.SwipeImageTouchListener;
import com.kvajpoj.homie.common.Utils;
import com.kvajpoj.homie.model.Homie;
import com.kvajpoj.homie.model.Node;
import com.kvajpoj.homie.model.Settings;
import com.kvajpoj.homie.service.MqttService;
import com.kvajpoj.homie.service.MqttServiceDelegate;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity implements
        MqttServiceDelegate.MessageHandler,
        MqttServiceDelegate.StatusHandler,
        RecyclerViewAdapter.OnItemClickListener {

    @BindView(R.id.btnStatus)
    ImageView mStatusView;
    @BindView(R.id.cntnt)
    FrameLayout cntnt;
    @BindView(R.id.fab)
    FloatingActionButton fabButton;
    @BindView(R.id.myrecyclerview)
    RecyclerView myRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tvStatus)
    TextView statusLabel;
    @BindView(R.id.app_bar_layout)
    AppBarLayout appBarLayout;


    private MqttServiceDelegate.StatusReceiver statusReceiver;
    private MqttServiceDelegate.MessageReceiver msgReceiver;
    private Logger LOG;
    private GridLayoutManager gridLayoutManager;
    private RecyclerViewAdapter myRecyclerViewAdapter;
    private EditText etWebcamUsername = null;
    private EditText etNodeName = null;
    private EditText etNodeUnit = null;
    private EditText etNodeTopic = null;
    private EditText etNodePublish = null;
    private EditText etWebcamPassword = null;
    private EditText etWebcamURL = null;
    private View positiveAction = null;
    private View neutralAction = null;
    private View negativeAction = null;
    private RadioButton rbNodeTypeMqtt = null;
    private RadioButton rbNodeTypeWebcam = null;
    private RadioGroup rgRadios = null;
    private LinearLayout llMqtt = null;
    private LinearLayout llWebcam = null;
    private LinearLayout llNodeTypeSelector = null;
    private LinearLayout llDialogConfirmContent = null;
    private LinearLayout llDialogMqttHelpContent = null;
    private LinearLayout llDialogContent = null;
    private MaterialDialog mdDialog = null;
    private EditText nodeTopicPublishValue = null;

    @Override
    protected void onStart() {
        //Toast.makeText(this, "Homie: START", Toast.LENGTH_SHORT).show();
        super.onStart();
    }


    @Override
    protected void onPostResume() {
       super.onPostResume();
       //Toast.makeText(this, "Homie: POST RESUME", Toast.LENGTH_SHORT).show();
        // check if we have any web cams to update
        for(int i = 0; i < myRecyclerViewAdapter.getData().size(); i++) {
            Node n = myRecyclerViewAdapter.getItem(i);
            if(n != null && n.getType() == Node.WEBCAM) {
                View nodeView = gridLayoutManager.findViewByPosition(i);
                if(nodeView != null) {
                    RecyclerViewAdapter.ItemHolder holder = (RecyclerViewAdapter.ItemHolder) myRecyclerView.getChildViewHolder(nodeView);
                    if(holder != null && holder.getCurrentNode().getId().equals(n.getId())) {
                        holder.updateNodeData();
                    }
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Toast.makeText(this, "Homie: CREATE", Toast.LENGTH_SHORT).show();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setIcon(R.drawable.ic_homies);

        LOG = Logger.getLogger(MainActivity.class);

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));

        appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                Log.d("STATE", state.name());

                if(state == State.COLLAPSED) {
                    fabButton.animate().translationY(300).start();
                }
                else if (state == State.EXPANDED) {
                     fabButton.animate()
                             .translationY(0)
                             .setDuration(300)
                             .setInterpolator(null).start();
                }
            }
        });

        // Get a Realm instance for this thread
        final Realm realm = Realm.getDefaultInstance();


        // get data to be displayed
        //final RealmResults<Node> iotNodes = realm.where(Node.class).findAllSorted("position", Sort.ASCENDING);
        //final RealmResults<Node> iotNodes = realm.where(Node.class).findAllSorted("position");
        final RealmResults<Node> iotNodes;
        iotNodes = realm.where(Node.class).sort("position").findAll();

        // create data adapter
        myRecyclerViewAdapter = new RecyclerViewAdapter(this, iotNodes);
        myRecyclerViewAdapter.setOnItemClickListener(this);
        //myRecyclerViewAdapter.enableDragDropSupport(myRecyclerView);

        // create layout manager for data
        //staggeredGridLayoutManagerVertical = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL);
        gridLayoutManager = new GridLayoutManager(this, 4);

        // assign adapter and layout manager to recycler view
        myRecyclerView.setAdapter(myRecyclerViewAdapter);
        //myRecyclerView.setLayoutManager(staggeredGridLayoutManagerVertical);
        myRecyclerView.setLayoutManager(gridLayoutManager);
        //myRecyclerView.setItemAnimator(null);


        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {

            @Override
            public int getSpanSize(int position) {
            Node n = null;

            for (int i = 0; i < myRecyclerViewAdapter.getItemCount(); i++) {
                Node tmp = myRecyclerViewAdapter.getItem(i);
                if(tmp.getPosition() == position+1){
                    n = tmp;
                    break;
                }
            }

            if(n == null)
                n = myRecyclerViewAdapter.getItem(position);

            position +=1;
            switch (n.getType()) {
                case Node.MQTT_SENSOR:
                    LOG.debug("ORDR " + n.getName() + " SENSOR " + position + " is " + n.getType() + " " + n.getPosition());
                    return 2;
                case Node.MQTT_SWITCH:
                    LOG.debug("ORDR " + n.getName() + " SWITCH " + position + " is " + n.getType() + " " + n.getPosition());
                    return 1;
                case Node.WEBCAM:
                    LOG.debug("ORDR " + n.getName() + " WEBCAM " + position + " is " + n.getType() + " " + n.getPosition());
                    return 2;
                case Node.MQTT_CUSTOM_NODE:
                    LOG.debug("ORDR " + n.getName() + " CUSTOM NODE " + position + " is " + n.getType() + " " + n.getPosition());
                    return 1;

            }
            LOG.debug("ORDR " + n.getName() + " KRNEKEJ " + position + " is " + n.getType() + " " + n.getPosition());
            return 1;
            }
        });
    }


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
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {

        //Toast.makeText(this, "Homie: RESUME", Toast.LENGTH_SHORT).show();

        //Init Receivers
        bindStatusReceiver();
        bindMessageReceiver();

        Settings set = Settings.getInstance();
        set.reloadSettings(this);

        statusLabel.setText("Initializing ...");
        mStatusView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

        ArrayList<String> topics=new ArrayList<>();

        String hbt = set.getHomieBaseTopic();
        if(!hbt.isEmpty() )
            topics.add(hbt + "/#");
        // add other topics from any custom nodes
       /* Realm realm = Realm.getDefaultInstance();

        try {
            OrderedRealmCollection<Node> nodes = myRecyclerViewAdapter.getData();
            Node n = nodes.where().equalTo("id", node.getId()).findFirst();
            realm.beginTransaction();
            if (n != null) n.deleteFromRealm();
            realm.commitTransaction();
            myRecyclerViewAdapter.notifyDataSetChanged();
        }
        catch (Exception e) {
            realm.cancelTransaction();
            LOG.error(e.getMessage());
        }
        finally {
            realm.close();
            dialog.dismiss();
            mdDialog = null;
        }*/



        MqttServiceDelegate.startService(this, set.getServerUrl(), set.getServerPort(), set.getUsername(), set.getPassword(), topics);

        super.onResume();
    }


    @Override
    protected void onStop() {

//        /Toast.makeText(this, "Homie: STOP", Toast.LENGTH_SHORT).show();

        MqttServiceDelegate.stopService(this);
        if(mdDialog != null && mdDialog.isShowing()) {
            mdDialog.dismiss();
            mdDialog = null;
        }
        unbindMessageReceiver();
        unbindStatusReceiver();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LOG.debug("onDestroy");
        //Toast.makeText(this, "Homie: DESTROY", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    @SuppressWarnings("ConstantConditions")
    public void showNodeDialog(final Node node) {

        final String titleNewNode = "Create new node";
        final String titleEditNode;
        if(node != null)
            titleEditNode = node.getType() == Node.MQTT_CUSTOM_NODE ? node.getName() + "\n(Mqtt node)" : node.getName() + "\n(Web camera)";
        else
            titleEditNode = "";
        final String titleHelp = "Help";
        final String titleDelete = "Remove node";


        MaterialDialog dialog = new MaterialDialog.Builder(this)
        //.backgroundColorRes(R.color.colorPrimary)
        //.titleColorRes(R.color.colorWhite)
        .title(node == null ? titleNewNode : titleEditNode)
        .customView(R.layout.new_node_dialog, true)
        .negativeText(android.R.string.cancel)
        .positiveText("Save")
        //.positiveColorRes(R.color.colorWhite)
        //.negativeColorRes(R.color.colorWhite)
        .neutralText(node == null? " HELP" : " DELETE NODE")
        //.neutralColorRes(node == null? R.color.colorWhite : R.color.colorAccent)
        .autoDismiss(false)
        .dismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mdDialog = null;
            }
        })
        .onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (llDialogContent.getVisibility() == View.VISIBLE) {
                if(node != null){
                    llDialogContent.setVisibility(View.INVISIBLE);
                    positiveAction.setVisibility(View.GONE);
                    llDialogConfirmContent.setVisibility(View.VISIBLE);
                    llDialogMqttHelpContent.setVisibility(View.GONE);
                    dialog.setTitle(titleDelete);
                    dialog.setActionButton(DialogAction.NEGATIVE, "CANCEL");
                }
                else {
                    llDialogContent.setVisibility(View.INVISIBLE);
                    positiveAction.setVisibility(View.GONE);
                    llDialogMqttHelpContent.setVisibility(View.VISIBLE);
                    llDialogConfirmContent.setVisibility(View.GONE);
                    neutralAction.setVisibility(View.GONE);
                    dialog.setTitle(titleHelp);
                    dialog.setActionButton(DialogAction.NEGATIVE, "GO BACK");

                }
            }
            else {
                Realm realm = Realm.getDefaultInstance();

                try {
                    OrderedRealmCollection<Node> nodes = myRecyclerViewAdapter.getData();
                    Node n = nodes.where().equalTo("id", node.getId()).findFirst();
                    realm.beginTransaction();
                    if (n != null) n.deleteFromRealm();
                    realm.commitTransaction();
                    myRecyclerViewAdapter.notifyDataSetChanged();
                }
                catch (Exception e) {
                    realm.cancelTransaction();
                    LOG.error(e.getMessage());
                }
                finally {
                    realm.close();
                    dialog.dismiss();
                    mdDialog = null;
                }
            }
            }
        })
        .onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            // if positive action is not visible, we are in delete mode
            if (positiveAction.getVisibility() == View.GONE) {
                if(dialog.getTitleView().getText().equals(titleHelp)){
                    llDialogMqttHelpContent.setVisibility(View.INVISIBLE);
                    positiveAction.setVisibility(View.VISIBLE);
                    llDialogContent.setVisibility(View.VISIBLE);
                    neutralAction.setVisibility(View.VISIBLE);
                    dialog.setTitle(titleNewNode);
                    dialog.setActionButton(DialogAction.NEGATIVE, "CANCEL");

                }
                else if (dialog.getTitleView().getText().equals(titleDelete)) {
                    llDialogConfirmContent.setVisibility(View.INVISIBLE);
                    positiveAction.setVisibility(View.VISIBLE);
                    llDialogContent.setVisibility(View.VISIBLE);
                    dialog.setTitle(titleEditNode);
                    dialog.setActionButton(DialogAction.NEGATIVE, "CANCEL");
                }

            }
            else {
                dialog.dismiss();
                mdDialog = null;
            }
            }
        })
        .onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            try {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                Node n;

                if(node != null) { n = node; }
                else {
                    n = new Node();
                    if (rbNodeTypeMqtt.isChecked()) n.setType(Node.MQTT_CUSTOM_NODE);
                    if (rbNodeTypeWebcam.isChecked()) n.setType(Node.WEBCAM);
                    n.setPosition(getPosition(realm));
                    n = realm.copyToRealm(n);
                }

                n.setName(etNodeName.getText().toString());

                if(n.getType() == Node.WEBCAM) {
                    n.setWebcamURL(etWebcamURL.getText().toString());
                    n.setWebcamUsername(etWebcamUsername.getText().toString());
                    n.setWebcamPassword(etWebcamPassword.getText().toString());
                }

                if(n.getType() == Node.MQTT_CUSTOM_NODE) {
                    n.setUnit(etNodeUnit.getText().toString());
                    n.setTopic(etNodeTopic.getText().toString());
                    n.setProperties(etNodePublish.getText().toString());
                }
                realm.commitTransaction();
                realm.close();
                myRecyclerViewAdapter.notifyDataSetChanged();
            }
            catch (Exception ex) {
                LOG.error(ex.toString());
            }
            finally {
                dialog.dismiss();
                mdDialog = null;
            }
            }
        }).build();


        // dialog
        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        negativeAction = dialog.getActionButton(DialogAction.NEGATIVE);
        neutralAction = dialog.getActionButton(DialogAction.NEUTRAL);

        // layouts
        llMqtt = dialog.getCustomView().findViewById(R.id.item_mqtt);
        llWebcam = dialog.getCustomView().findViewById(R.id.item_webcam);
        llNodeTypeSelector = dialog.getCustomView().findViewById(R.id.nodeTypeSelector);
        llDialogConfirmContent = dialog.getCustomView().findViewById(R.id.dialogConfirmContent);
        llDialogMqttHelpContent = dialog.getCustomView().findViewById(R.id.dialogMQTTHelpContent);
        llDialogContent = dialog.getCustomView().findViewById(R.id.dialogContent);

        // node type selectors
        rgRadios = dialog.getCustomView().findViewById(R.id.radioType);
        rbNodeTypeMqtt = dialog.getCustomView().findViewById(R.id.node_mqtt);
        rbNodeTypeWebcam = dialog.getCustomView().findViewById(R.id.node_webcam);

        // common
        etNodeName = dialog.getCustomView().findViewById(R.id.node_name);

        // mqtt
        etNodeUnit = dialog.getCustomView().findViewById(R.id.node_unit);
        etNodePublish = dialog.getCustomView().findViewById(R.id.node_publish_value);
        etNodeTopic = dialog.getCustomView().findViewById(R.id.node_topic);

        // webcam
        etWebcamPassword = dialog.getCustomView().findViewById(R.id.node_webcam_password);
        etWebcamUsername = dialog.getCustomView().findViewById(R.id.node_webcam_username);
        etWebcamURL = dialog.getCustomView().findViewById(R.id.node_webcam_url);

        rgRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.node_mqtt:
                    llWebcam.setVisibility(View.INVISIBLE);
                    llMqtt.setVisibility(View.VISIBLE);
                    neutralAction.setVisibility(View.VISIBLE);
                    break;
                case R.id.node_webcam:
                    llMqtt.setVisibility(View.INVISIBLE);
                    llWebcam.setVisibility(View.VISIBLE);
                    neutralAction.setVisibility(View.INVISIBLE);
                    break;
            }
            }
        });

        if (node != null) {
            llNodeTypeSelector.setVisibility(View.GONE);
            etNodeName.setText(node.getName());

            if(node.getType() == Node.WEBCAM) {
                // select correct ui layout
                rbNodeTypeWebcam.setSelected(true);
                llMqtt.setVisibility(View.INVISIBLE);
                llWebcam.setVisibility(View.VISIBLE);
                // set widget properties
                etWebcamURL.setText(node.getWebcamURL());
                etWebcamPassword.setText(node.getWebcamPassword());
                etWebcamUsername.setText(node.getWebcamUsername());
            }

            if(node.getType() == Node.MQTT_CUSTOM_NODE) {
                // set widget properties
                etNodeUnit.setText(node.getUnit());
                etNodePublish.setText(node.getProperties());
                etNodeTopic.setText(node.getTopic());
            }
        }
        else {
            //neutralAction.setVisibility(View.INVISIBLE);

        }

        etNodeName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }
        });


        //dialog.getTitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        dialog.getTitleView().setPadding(10,0,0,0);


        dialog.show();
        positiveAction.setEnabled(!etNodeName.getText().toString().isEmpty()); // disabled by default
        mdDialog = dialog;
    }

    @OnClick(R.id.fab)
    public void onClick(FloatingActionButton fab) {
        showNodeDialog(null);
    }

    private void bindMessageReceiver() {
        msgReceiver = new MqttServiceDelegate.MessageReceiver();
        msgReceiver.registerHandler(this);
        registerReceiver(msgReceiver, new IntentFilter(MqttService.MQTT_MSG_RECEIVED_INTENT));
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

    private boolean updateHomie(Homie h) {
        if (h.getNodes().size() > 0) {
            for (int i = 0; i < h.getNodes().size(); i++) updateNode(h.getNodes().get(i));
        }
        return true;
    }

    private boolean updateNode(Node n) {

        for (int i = 0; i < myRecyclerViewAdapter.getData().size(); i++) {

            Node nn = myRecyclerViewAdapter.getItem(i);
            if (nn != null && nn.getId().equals(n.getId())) {
                View nodeView = gridLayoutManager.findViewByPosition(i);
                if (nodeView != null) {
                    RecyclerViewAdapter.ItemHolder holder = (RecyclerViewAdapter.ItemHolder) myRecyclerView.getChildViewHolder(nodeView);
                    if (holder != null) holder.updateNodeData();
                }
                break;
            }
        }
        return true;
    }

    @Override
    public void handleMessage(String topic, byte[] payload) {
        String message = new String(payload);

        LOG.info("handleMessage: topic=" + topic + ", message=" + message);

        try {
            // first we get base topic and device id
            Settings set = Settings.getInstance();
            //set.reloadSettings(this);
            String hbt = set.getHomieBaseTopic();
            String tmp = Homie.getBaseTopic(topic);
            if (tmp.equals(hbt)) {
                tmp = Homie.getDeviceId(topic);
                if (!tmp.isEmpty()) {
                    Realm realm = Realm.getDefaultInstance();
                    Homie h = realm.where(Homie.class).equalTo("deviceId", tmp).findFirst();

                    if (h == null) {
                        h = new Homie();
                        h.setDeviceId(tmp);
                        realm.beginTransaction();
                        Homie realmHomie = realm.copyToRealm(h);
                        realm.commitTransaction();
                        h = realmHomie;

                    }

                    //------------------------------------------------------------------------------
                    // find  and handle device property
                    String deviceProperty = Homie.getDeviceProperty(topic);

                    if (!deviceProperty.isEmpty()) {

                        if (deviceProperty.equals("$name")) {

                            LOG.info("We got name " + message + "; current name is: " + h.getName());
                            if (h.getName().isEmpty()) {
                                realm.beginTransaction();
                                h.setName(message);
                                realm.commitTransaction();
                            }
                            return;
                        } else if (deviceProperty.equals("$online")) {
                            if (h.getOnline() != message.equals("true")) {
                                realm.beginTransaction();
                                h.setOnline(message.equals("true"));
                                realm.commitTransaction();
                                updateHomie(h);
                            }
                            return;
                        }


                        // this is custom property; it is not part of the convention but is more
                        // of an convenience to send out additional properties like battery, cpu state,
                        // remaining memory ....
                        else if (deviceProperty.equals("$battery")) {

                            if (Homie.isDevicePropertySubTopic("voltage", topic)) {
                                if (!h.getBatteryVoltage().contains(message)) {
                                    realm.beginTransaction();
                                    h.setBatteryVoltage(message);
                                    realm.commitTransaction();
                                    updateHomie(h);
                                }
                                return;
                            } else if (Homie.isDevicePropertySubTopic("percentage", topic)) {
                                if (!h.getBatteryPercentage().contains(message + "%")) {
                                    realm.beginTransaction();
                                    h.setBatteryPercentage(message + "%");
                                    realm.commitTransaction();
                                    updateHomie(h);
                                }
                                return;
                            } else {
                                return;
                            }
                        } // end of battery property
                        else {
                            return;
                        }
                    } // end of device property
                    //------------------------------------------------------------------------------

                    //------------------------------------------------------------------------------
                    // Find and handle device node
                    //------------------------------------------------------------------------------
                    String deviceNode = Homie.getNode(topic);

                    if (!deviceNode.isEmpty()) {

                        // search for node in database or create new node
                        Node n = h.getNodes().where().contains("name", h.getDeviceId() + "-" + deviceNode).findFirst();

                        if (n == null) {
                            realm.beginTransaction();
                            n = new Node();
                            n.setName(h.getDeviceId() + "-" + deviceNode);
                            n.setPosition(getPosition(realm));
                            n.setHomie(h);
                            Node realmNode = realm.copyToRealm(n);
                            n = realmNode;
                            h.getNodes().add(n);
                            realm.commitTransaction();
                            myRecyclerViewAdapter.notifyDataSetChanged();
                        }

                        // save device topic base path to node name
                        if (!n.getTopic().equals(Homie.getNodeBaseTopic(topic))) {
                            realm.beginTransaction();
                            n.setTopic(Homie.getNodeBaseTopic(topic));
                            realm.commitTransaction();
                        }

                        String nodeProperty = Homie.getNodeProperty(topic);
                        if (!nodeProperty.isEmpty()) {

                            // handle type property
                            if (nodeProperty.equals("$type")) {
                                if (message.equals("switch")) {
                                    if (n.getType() != Node.MQTT_SWITCH) {
                                        realm.beginTransaction();
                                        n.setType(Node.MQTT_SWITCH);
                                        realm.commitTransaction();
                                    }
                                    return;
                                } else if (message.equals("sensor")) {
                                    if (n.getType() != Node.MQTT_SENSOR) {
                                        realm.beginTransaction();
                                        n.setType(Node.MQTT_SENSOR);
                                        realm.commitTransaction();
                                    }
                                    return;
                                } else {
                                    // all unknown nodes are set to sensor
                                    realm.beginTransaction();
                                    n.setType(Node.MQTT_SENSOR);
                                    realm.commitTransaction();
                                    return;
                                }
                            }

                            // handle node properties property
                            if (nodeProperty.equals("$properties")) {
                                if (!n.getProperties().equals(message)) {
                                    realm.beginTransaction();
                                    n.setProperties(message);
                                    realm.commitTransaction();
                                }
                                return;
                            }

                            // handle node's advertised subtopics, like voltage, temperature

                            // if node is switch, it has by convention 'on' subtopic which signalizes
                            // state of the switch

                            if (Homie.isDevicePropertySubTopic("on", topic) && n.getProperties().contains("on")) {
                                // check if this message is not echo from changing switch state
                                // homie/686f6d6965/light/on/set → true
                                if(topic.split("/").length == 5 && topic.split("/")[4].equals("set")){
                                    return;
                                }

                                if (!n.getValue().equals(message)) {
                                    realm.beginTransaction();
                                    n.setValue(message);
                                    realm.commitTransaction();
                                    updateNode(n);
                                }
                                return;
                            } else if (n.getProperties().contains(nodeProperty) && message.replace(".", "").replace(",", "").matches("^[+-]?\\d+$")) {
                                LOG.info("Node: Got value: " + nodeProperty);
                                if (!n.getValue().equals(message)) {
                                    realm.beginTransaction();
                                    n.setValue(message);
                                    realm.commitTransaction();
                                    updateNode(n);
                                }
                                return;
                            } else if (n.getProperties().contains(nodeProperty)) {
                                LOG.info("Node: Got unit: " + nodeProperty);
                                if (!n.getUnit().equals(message)) {
                                    realm.beginTransaction();
                                    n.setUnit(message);
                                    realm.commitTransaction();
                                    updateNode(n);
                                }
                                return;
                            } else {
                                LOG.info("Node: Got unknown: " + topic);
                            }

                            //DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                            //n.setUpdated(dateFormat.format(new Date()));
                            //realm.commitTransaction();

                        } else {
                            LOG.debug("we got no node: " + topic);
                        }
                    } // end of is node

                } // end of device id
                else {
                    //device id not found
                }
            } // end of base topic
            else {
                //base topic is not homie
            }
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
    } // end of function


    private int getPosition(Realm realm) {
        Number newPostion = 1;

        RealmResults<Node> nodes = realm.where(Node.class).findAll();
        if (nodes.size() > 0) {
            newPostion = nodes.max("position").intValue();
        } else {
            newPostion = 0;
        }

        return newPostion.intValue() + 1;
    }

    @Override
    public void handleStatus(MqttService.MQTTConnectionStatus status, String reason) {

        Settings set = Settings.getInstance();
        set.reloadSettings(this);

        LOG.debug("handleStatus: status=" + status + ", reason=" + reason + "; URL: " + set.getServerUrl() + ":" + set.getServerPort());

        if (status == MqttService.MQTTConnectionStatus.CONNECTING) {
            statusLabel.setText("Connecting to " + set.getServerUrl());// + ":" + set.getServerPort() + " ...");
            DrawableHelper.withContext(this).withColor(R.color.colorAccent).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        } else if (status == MqttService.MQTTConnectionStatus.CONNECTED) {
            statusLabel.setText("Connected to " + set.getServerUrl());// + ":" + set.getServerPort());
            DrawableHelper.withContext(this).withColor(R.color.colorGreen).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        } else if (status == MqttService.MQTTConnectionStatus.INITIAL) {
            statusLabel.setText("Initializing ...");
            DrawableHelper.withContext(this).withColor(R.color.colorAccent).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        } else if (status == MqttService.MQTTConnectionStatus.NOTCONNECTED_DATADISABLED) {
            statusLabel.setText("Data is disabled!");
            DrawableHelper.withContext(this).withColor(R.color.colorPrimaryDark).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        } else if (status == MqttService.MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON) {
            statusLabel.setText("Unable to connect! Check Mqtt server address.");
            DrawableHelper.withContext(this).withColor(R.color.colorPrimaryDark).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        } else if (status == MqttService.MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET) {
            statusLabel.setText("Waiting for internet!");
            DrawableHelper.withContext(this).withColor(R.color.colorPrimaryDark).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        } else if (status == MqttService.MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT) {
            statusLabel.setText("Disconnected");
            DrawableHelper.withContext(this).withColor(R.color.colorPrimaryDark).withDrawable(R.drawable.ic_lens_black_24dp).tint().applyTo(mStatusView);
        }
    }

    @Override
    public void onItemClick(RecyclerViewAdapter.ItemHolder item, int position) {
        final Node n = item.getCurrentNode();
        if (n != null) {
            if (n.getType() == Node.WEBCAM) {
                final Dialog nagDialog = new Dialog(this,android.R.style.Theme_Translucent);
                nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                nagDialog.setCancelable(true);
                nagDialog.setContentView(R.layout.full_screen_image);
                Button btnClose = (Button)nagDialog.findViewById(R.id.btnIvClose);
                final ImageView ivPreview = (ImageView)nagDialog.findViewById(R.id.iv_preview_image);
                final ProgressBar pbLoader = (ProgressBar) nagDialog.findViewById(R.id.pbLoader);

                final Utils.LoadImageCallback imageCallback =  new Utils.LoadImageCallback() {
                    @Override
                    public void onLoadImageError(Exception e) {
                        LOG.error("Load Image error: " + e.toString());
                        ivPreview.setPadding(400,0,400,0);
                        ivPreview.setTranslationY(0);
                        ivPreview.setTranslationX(0);
                        pbLoader.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onLoadImageSuccess() {
                        ivPreview.setPadding(0,0,0,0);
                        ivPreview.setTranslationY(0);
                        ivPreview.setTranslationX(0);
                        pbLoader.setVisibility(View.INVISIBLE);
                    }
                };

                // go ahead and load image
                Utils.LoadImage(this, ivPreview, n, imageCallback);

                btnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                    nagDialog.dismiss();
                    }
                });
                nagDialog.show();

                View swipeParent = nagDialog.findViewById(R.id.placeholder);
                SwipeImageTouchListener sitl = new SwipeImageTouchListener( ivPreview, new SwipeImageTouchListener.ISwipeImageCallback() {
                    @Override
                    public void onSwipeImageUp( SwipeImageTouchListener sender, View swipeView ) {
                        nagDialog.dismiss();
                    }

                    @Override
                    public void onSwipeImageDown( SwipeImageTouchListener sender, View swipeView ) {
                        nagDialog.dismiss();
                    }

                    @Override
                    public void onSwipeImageLeft(SwipeImageTouchListener sender, View swipeView) {
                        Utils.ClearImage(ivPreview);
                        ivPreview.setVisibility(View.VISIBLE);
                        pbLoader.setVisibility(View.VISIBLE);

                        Node next = Realm.getDefaultInstance()
                                .where(Node.class)
                                .equalTo("type", Node.WEBCAM)
                                .greaterThan("position", sender.getPosition())
                                .findAllSorted("position", Sort.ASCENDING)
                                .first();

                        if(next != null) {
                            LOG.info("Load next image " + next.getName() + " on position " + next.getPosition());
                            sender.setPosition(next.getPosition());
                            setupLeftRight(sender);
                            Utils.LoadImage(getApplicationContext(), ivPreview, next, imageCallback);
                        }
                    }

                    @Override
                    public void onSwipeImageRight(SwipeImageTouchListener sender, View swipeView) {
                        Utils.ClearImage(ivPreview);
                        ivPreview.setVisibility(View.VISIBLE);
                        pbLoader.setVisibility(View.VISIBLE);

                        Node prev = Realm.getDefaultInstance()
                                .where(Node.class)
                                .equalTo("type", Node.WEBCAM)
                                .lessThan("position", sender.getPosition())
                                .findAllSorted("position", Sort.DESCENDING)
                                .first();

                        if(prev != null) {
                            LOG.info("Load prev image " + prev.getName() + " on position " + prev.getPosition());
                            sender.setPosition(prev.getPosition());
                            setupLeftRight(sender);
                            Utils.LoadImage(getApplicationContext(), ivPreview, prev, imageCallback);
                        }
                    }
                });

                LOG.info("Load initial image " + n.getName() + " on position " + n.getPosition());
                sitl.setPosition(n.getPosition());
                setupLeftRight(sitl);
                swipeParent.setOnTouchListener(sitl);
            }
            else {
                LOG.debug("item clicked, with type: " + n.getTopic() + " Type: " + n.getType());
            }
        }
    }

    private void setupLeftRight(SwipeImageTouchListener swiper) {
        Node prev = Realm.getDefaultInstance().where(Node.class).equalTo("type", Node.WEBCAM).lessThan("position", swiper.getPosition()).findFirst();
        swiper.SetRightEnabled(prev != null);
        Node next = Realm.getDefaultInstance().where(Node.class).equalTo("type", Node.WEBCAM).greaterThan("position", swiper.getPosition()).findFirst();
        swiper.SetLeftEnabled(next != null);
    }

    @Override
    public void onItemLongPress(RecyclerViewAdapter.ItemHolder item, int position) {
        Node n = item.getCurrentNode();
        if (n != null) {
            if (n.getType() == Node.MQTT_SWITCH) {

                LOG.debug("item long pressed! " + n.getTopic() + "/on/set " + n.getUnit());

                if (n.getValue().equals("true")) {
                    MqttServiceDelegate.publish(MainActivity.this, n.getTopic() + "/on/set", "false".getBytes());
                    LOG.debug("item long pressed! " + n.getTopic() + "/on/set -> false");
                }
                else if (n.getValue().equals("false")) {
                    MqttServiceDelegate.publish(MainActivity.this, n.getTopic() + "/on/set", "true".getBytes());
                    LOG.debug("item long pressed! " + n.getTopic() + "/on/set -> true");
                }
                else {
                    LOG.debug("item long pressed, with unknown value: " + n.getTopic() + " Value: " + n.getValue());
                }
            }
            else if (n.getType() == Node.WEBCAM) {
                updateNode(n);
            }
            else {
                LOG.debug("item long pressed, with type: " + n.getTopic() + " Type: " + n.getType());
            }
        }
    }

    @Override
    public void onItemEditClick(RecyclerViewAdapter.ItemHolder item, int position) {
        Node node = item.getCurrentNode();
        if (node.getType() == Node.MQTT_CUSTOM_NODE || node.getType() == Node.WEBCAM) {
            showNodeDialog(node);
        }
        else {
            //TODO show HomieNodeDialog(node);
        }
    }

    @OnClick({R.id.btnStatus, R.id.tvStatus, R.id.llStatusPanel})
    public void onClick() {
        if (cntnt.getVisibility() != View.VISIBLE) cntnt.setVisibility(View.VISIBLE);

    }

    static abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

        public enum State {
            EXPANDED,
            COLLAPSED,
            IDLE
        }

        private State mCurrentState = State.IDLE;

        @Override
        public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {

            Log.d("STATE", "Offset " + i);
            if (i == 0) {
                if (mCurrentState != State.EXPANDED) {
                    onStateChanged(appBarLayout, State.EXPANDED);
                }
                mCurrentState = State.EXPANDED;
            } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()) {
                if (mCurrentState != State.COLLAPSED) {
                    onStateChanged(appBarLayout, State.COLLAPSED);
                }
                mCurrentState = State.COLLAPSED;
            } else {
                if (mCurrentState != State.IDLE) {
                    onStateChanged(appBarLayout, State.IDLE);
                }
                mCurrentState = State.IDLE;
            }
        }

        public abstract void onStateChanged(AppBarLayout appBarLayout, State state);
    }


    @OnClick(R.id.btnClosePanel)
    public void onCloseClick() {
        if (cntnt.getVisibility() == View.VISIBLE) cntnt.setVisibility(View.GONE);
    }

}

