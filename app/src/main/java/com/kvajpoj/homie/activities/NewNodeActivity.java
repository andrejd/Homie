package com.kvajpoj.homie.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;

import com.kvajpoj.homie.R;
import com.kvajpoj.homie.adapter.OptionItemAdapter;
import com.kvajpoj.homie.components.ClickToSelectEditText;
import com.kvajpoj.homie.model.Node;
import com.kvajpoj.homie.model.OptionItem;

import org.apache.log4j.Logger;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmResults;


public class NewNodeActivity extends AppCompatActivity {

    // UI references.
    @Bind(R.id.name)        TextInputEditText mNameView;
    @Bind(R.id.topic)       TextInputEditText mTopic;
    @Bind(R.id.topicItem)   LinearLayout mTopicItem;

    @Bind(R.id.urlItem)     LinearLayout mUrlItem;
    @Bind(R.id.url)         TextInputEditText mUrl;


    @Bind(R.id.btnSave)     Button mBtnSaveView;
    @Bind(R.id.node_icon)   ImageView nodeIcon;
    //@Bind(R.id.node_form)   LinearLayout nodeForm;
    //@Bind(R.id.login_form)  ScrollView loginForm;

    private Logger LOG;


    private static final String ERROR_MSG = "Very very very long error message to get scrolling or multiline animation when the error button is clicked";
    private static final String[] ITEMS = {"Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6"};


    private ListAdapter adapter;
    ClickToSelectEditText<String> nodeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        LOG = Logger.getLogger(NewNodeActivity.class);
        LOG.info("Creating NewNodeActivity!");

        setContentView(R.layout.activity_new_node);
        setupActionBar();
        ButterKnife.bind(this);


        final OptionItemAdapter adapter = new OptionItemAdapter(
                NewNodeActivity.this, R.layout.option_item);

        adapter.add(new OptionItem(Node.MQTT_SENSOR, "Mqtt temperature sensor", "Sensor's value will be displayed in °C", R.drawable.ic_infrared_sensor_icon_24dp));
        adapter.add(new OptionItem(Node.MQTT_SENSOR, "Mqtt pressure sensor", "Sensor's value will be displayed in millibars", R.drawable.ic_presure_24dp));
        adapter.add(new OptionItem(Node.WEBCAM, "Webcam image", "Image will be read from web enabled web camera", R.drawable.ic_now_wallpaper_24dp));
        adapter.add(new OptionItem(Node.MQTT_SWITCH, "Mqtt light switch", "Switch can be set ON or OFF", R.drawable.ic_public_24dp));


        nodeType = (ClickToSelectEditText<String>) findViewById(R.id.node_type);

        nodeType.setAdapter(adapter);

        nodeType.setOnItemSelectedListener(new ClickToSelectEditText.OnItemSelectedListener<OptionItem>() {
            @Override
            public void onItemSelectedListener(OptionItem item, int selectedIndex) {

                nodeType.setText(item.getName());
                nodeIcon.setImageDrawable(getDrawable(item.getIcon()));

                mBtnSaveView.requestFocus();

                switch (item.getType()) {
                    case 2:
                        mUrlItem.setVisibility(View.VISIBLE);
                        mUrl.setText("");
                        mTopicItem.setVisibility(View.GONE);
                        break;
                    default:
                        mUrlItem.setVisibility(View.GONE);
                        mTopic.setText("");
                        mTopicItem.setVisibility(View.VISIBLE);
                }
            }
        });

    }


    private void setupActionBar() {

        LOG.debug("Setting up action bar!");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    private boolean isTopicValid(String topic) {
        //TODO: Replace this with your own logic
        return topic.length() > 4;
    }


    @OnClick(R.id.btnSave)
    public void onClick(Button btn) {

        LOG.debug("Creating new node!");

        try {
            Realm realm = Realm.getDefaultInstance();

            Node n = new Node();

            n.setName(mNameView.getText().toString());

            switch (nodeType.getText().toString()) {
                case "Webcam":
                    n.setType(Node.WEBCAM);
                    break;
                case "Mqtt Switch":
                    n.setType(Node.MQTT_SWITCH);
                    break;
                default:
                    n.setType(Node.MQTT_SENSOR);
                    break;
            }

            Number newPostion = 1;

            RealmResults<Node> nodes = realm.where(Node.class).findAll();
            if (nodes.size() > 0) {
                newPostion = nodes.max("position").intValue();
            } else {
                newPostion = 0;
            }

            int pos = newPostion.intValue() + 1;
            n.setPosition(pos);

            LOG.debug("Max position " + newPostion + ", new position " + pos);

            realm.beginTransaction();
            realm.copyToRealm(n);
            realm.commitTransaction();
            realm.close();


        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
    }

}


