package com.kvajpoj.homie.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.daimajia.swipe.SwipeLayout;
import com.kvajpoj.homie.R;
import com.kvajpoj.homie.common.DrawableHelper;
import com.kvajpoj.homie.common.Utils;
import com.kvajpoj.homie.components.WebcamImageView;
import com.kvajpoj.homie.model.Node;
import com.kvajpoj.homie.touch.ItemTouchHelperAdapter;
import com.kvajpoj.homie.touch.ItemTouchHelperViewHolder;
import com.kvajpoj.homie.touch.OnStartDragListener;
import com.kvajpoj.homie.touch.SimpleItemTouchHelperCallback;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class RecyclerViewAdapter extends RealmRecyclerViewAdapter<Node, RecyclerViewAdapter.ItemHolder>
                                 implements ItemTouchHelperAdapter, OnStartDragListener
{

    private OnItemClickListener onItemClickListener;
    private OnStartDragListener mDragStartListener;
    private Logger LOG;
    private ItemTouchHelper mItemTouchHelper;


    public RecyclerViewAdapter(Context context, RealmResults<Node> realmResults) {
        super(context, realmResults, false);
        LOG = Logger.getLogger(RecyclerViewAdapter.class);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(this);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
        mDragStartListener = this;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SwipeLayout nodeView = (SwipeLayout) inflater.inflate(R.layout.grid_item, parent, false);
        nodeView.setShowMode(SwipeLayout.ShowMode.LayDown);
        return new ItemHolder(nodeView, this);
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {

        Node n = getItem(position);
        if (n != null) {
            holder.setCurrentNode(n);
            holder.updateNodeData();
            // TODO add check if drag/drop is enables
            holder.handle.setOnTouchListener(null);
        }

        holder.handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            Log.i("DragStatus", event.toString());
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                if (mDragStartListener != null)
                    mDragStartListener.onStartDrag(holder);
            }
            return false;
            }
        });

    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    private void RepositionNode(int original, int target) {

        // List to hold moved/relocated nodes; editing list items always ends drag and screen redraw
        // when node is repositioned. We get items in new array and then edit those items - array has
        // no callbacks defined

        final List<Node> list;

        LOG.debug("onItemClear! endPosition " + target + " origPosition " + original );
        if (original == -1 || target == -1 || (original == target)) return;

        try{
            list = new ArrayList<>();
            list.addAll(getData());

            Node orig = list.remove(original);
            list.add(target, orig);

            Realm.getDefaultInstance().executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setPosition(i+1);
                    }
                }
            });
        }
        catch (Exception e){
            LOG.error(e.toString());
        }
        finally {
            LOG.info("Reposition succeeded!");
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        LOG.debug("Item moved from " + fromPosition + " to position " + toPosition);

        RepositionNode(fromPosition, toPosition);

        if (mDragStartListener != null)
            mDragStartListener.onStopDrag(null);

        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        //LOG.debug("Dismissed from position " + position);
    }



    @Override // from on start drag listner
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onStopDrag(RecyclerView.ViewHolder viewHolder) {

    }

    public interface OnItemClickListener {
        void onItemClick(ItemHolder item, int position);
        void onItemEditClick(ItemHolder item, int position);
        void onItemLongPress(ItemHolder item, int position);
    }

    public class ItemHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder,
                       View.OnClickListener,
                       View.OnLongClickListener,
                       SwipeLayout.SwipeListener
    {

        private RecyclerViewAdapter parent;
        private SwipeLayout nodeView;
        private TextView textItemName;
        private TextView textItemValue;
        private TextView twItemUpdated;
        private TextView textItemUnit;
        private WebcamImageView handle;
        private ImageView edit;
        private ImageView imgOffline;
        private FrameLayout nodeHolder;
        private Node currentNode;
        private String id;
        private ImageView snapshot;
        private ImageView imgEdit;
        private LinearLayout battStatDisplay;
        private TextView battStatText;

        public Node getCurrentNode() { return currentNode; }
        public void setCurrentNode(Node currentNode) { this.currentNode = currentNode;  }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public ItemHolder(SwipeLayout nView, RecyclerViewAdapter parent) {
            super(nView);

            nodeView = nView;
            this.parent = parent;

            nodeHolder = nodeView.findViewById(R.id.nodeHolder);
            textItemName = nodeView.findViewById(R.id.name);
            textItemValue = nodeView.findViewById(R.id.value);
            textItemUnit = nodeView.findViewById(R.id.unit);
            twItemUpdated = nodeView.findViewById(R.id.itemupdated);
            handle = nodeView.findViewById(R.id.handle);
            edit = nodeView.findViewById(R.id.edit);
            snapshot = nodeView.findViewById(R.id.snapshot);
            imgOffline = nodeView.findViewById(R.id.imgOffline);
            imgEdit = nodeView.findViewById(R.id.edit);

            battStatText = nodeView.findViewById(R.id.txtBatteryPercentage);
            battStatDisplay = nodeView.findViewById(R.id.batteryDisplay);

            edit.setOnClickListener(this);

            nodeView.setOnClickListener(this);
            nodeView.setClickToClose(true);
            nodeView.addSwipeListener(this);

            nodeHolder.setOnLongClickListener(this);
            nodeHolder.setOnClickListener(this);
            battStatDisplay.setOnClickListener(this);
        }

        public boolean updateNodeData() {

            LOG.info("Updating node  " + currentNode.getName());

            if (textItemName != null && currentNode != null) {
                textItemName.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                if(currentNode.getHomie() != null)
                    textItemName.setText( currentNode.getHomie().getName() );
                else
                    textItemName.setText( currentNode.getName() );
            }

            if (textItemValue != null && currentNode != null) {
                textItemValue.setText(currentNode.getValue() );
            }

            if (textItemUnit != null && currentNode != null) {
                textItemUnit.setText(currentNode.getUnit());
            }

            // online
            if (twItemUpdated != null && currentNode != null) {
                if (currentNode.getHomie() != null && currentNode.getHomie().isOnline() == false) {
                    imgOffline.setVisibility(View.VISIBLE);
                }
                else {
                    imgOffline.setVisibility(View.GONE);
                }
            }

            // battery
            if(currentNode != null && currentNode.getHomie() != null && !currentNode.getHomie().getBatteryPercentage().isEmpty()) {
                battStatText.setText(currentNode.getHomie().getBatteryPercentage());
                battStatDisplay.setVisibility(View.VISIBLE);
            }
            else {
                battStatDisplay.setVisibility(View.GONE);
            }


            //icons
            if (currentNode != null ){

                switch (currentNode.getType()){
                    case Node.MQTT_SWITCH:
                    case Node.MQTT_SENSOR:
                        imgEdit.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_homie_logo));
                        DrawableHelper.withContext(context).withColor(R.color.colorWhite).withDrawable(R.drawable.ic_homie_logo).tint().applyTo(imgEdit);
                        break;
                    default:
                        imgEdit.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_info_outline_black_24dp));


                }
            }


            if (currentNode != null && currentNode.getType() == Node.MQTT_SWITCH) {



                if(currentNode.getValue().toLowerCase().contains("true") ||
                        currentNode.getValue().toLowerCase().contains("1") ||
                        currentNode.getValue().toLowerCase().contains("on")) {

                    textItemValue.setText("ON");
                    textItemValue.setTextColor(Color.WHITE);
                    textItemName.setTextColor(Color.WHITE);
                    nodeHolder.setBackground(context.getResources().getDrawable(R.drawable.node_background_dark));

                }
                else if(currentNode.getValue().toLowerCase().contains("false") ||
                        currentNode.getValue().toLowerCase().contains("0") ||
                        currentNode.getValue().toLowerCase().contains("off")) {

                    textItemValue.setText("OFF");
                    textItemValue.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                    textItemName.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                    nodeHolder.setBackground(context.getResources().getDrawable(R.drawable.node_background));
                }
                else{
                    textItemValue.setText(currentNode.getValue().toUpperCase());
                    textItemValue.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                    textItemName.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                    nodeHolder.setBackground(context.getResources().getDrawable(R.drawable.node_background));
                }
            }


            if (currentNode != null && currentNode.getType() == Node.WEBCAM) {

                // wait at least 2 seconds before updating again
                if ( currentNode.getLastUpdateTime() + 2000 > System.currentTimeMillis() ) {
                    return false;
                }
                currentNode.setLastUpdateTime( System.currentTimeMillis() );
                textItemName.setTextColor(Color.WHITE);
                Utils.LoadImage(context, snapshot, currentNode, new Utils.LoadImageCallback() {
                    @Override
                    public void onLoadImageError(Exception e) {
                        snapshot.setPadding(500, 100, 500, 100);
                        textItemName.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                    }

                    @Override
                    public void onLoadImageSuccess() {
                        snapshot.setPadding(0, 0, 0, 0);
                    }
                }, R.drawable.ic_broken_image_gray_24dp );
                return true;
            }
            else {
                Utils.ClearImage(snapshot);
                return false;
            }
        }

        private long mCloseTime = 0;



        @Override
        public void onClick(View v) {
            LOG.debug("Item click!");
            final OnItemClickListener listener = parent.getOnItemClickListener();
            if (listener != null) {

                //if (v == delete) {
                //listener.onDeleteClick(this, getPosition());
                //    return;
                //}
                if (v == edit) {
                    listener.onItemEditClick(this, getPosition());
                    nodeView.close();
                    return;
                }

                if(nodeView.getOpenStatus() == SwipeLayout.Status.Close) {
                    if(mCloseTime + 100 < System.currentTimeMillis())
                        listener.onItemClick(this, getPosition());
                }


            }
        }

        @Override
        public void onItemSelected() {
            nodeView.setAlpha((float) 0.5);
        }

        @Override
        public void onItemClear() {
            nodeView.setAlpha((float) 1);
        }


        @Override
        public boolean onLongClick(View v) {
            //nodeView.open(true);
            final OnItemClickListener listener = parent.getOnItemClickListener();
            if (listener != null) {
                listener.onItemLongPress(this, getPosition());
                return true;
            }
            return true;
        }

        @Override
        public void onStartOpen(SwipeLayout layout) {
            //LOG.info("Swipe: START OPEN");
        }

        @Override
        public void onOpen(SwipeLayout layout) {
            //LOG.info("Swipe: ON OPEN");
        }

        @Override
        public void onStartClose(SwipeLayout layout) {
            //LOG.info("Swipe: START CLOSE");
        }

        @Override
        public void onClose(SwipeLayout layout) {
            //LOG.info("Swipe: CLOSE");
            mCloseTime = System.currentTimeMillis();
        }

        @Override
        public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {
            //LOG.info("Swipe: UPDATE");
        }

        @Override
        public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
            //LOG.info("Swipe: HAND RELEASE");

        }
    }

}

