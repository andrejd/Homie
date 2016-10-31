package com.kvajpoj.homie.adapter;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.daimajia.swipe.SwipeLayout;
import com.kvajpoj.homie.R;
import com.kvajpoj.homie.model.Node;
import com.kvajpoj.homie.touch.ItemTouchHelperAdapter;
import com.kvajpoj.homie.touch.ItemTouchHelperViewHolder;
import com.kvajpoj.homie.touch.OnStartDragListener;


import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.Sort;

public class RecyclerViewAdapter
        extends RealmRecyclerViewAdapter<Node, RecyclerViewAdapter.ItemHolder>
        implements ItemTouchHelperAdapter {

    private OnItemClickListener onItemClickListener;
    private final OnStartDragListener mDragStartListener;
    private Logger LOG;
    private int origPosition = -1;
    private int endPosition = -1;

    public RecyclerViewAdapter(Context context,
                               RealmResults<Node> realmResults,
                               boolean automaticUpdate,
                               OnStartDragListener mDragStartListener) {

        super(context, realmResults, automaticUpdate);
        this.mDragStartListener = mDragStartListener;

        LOG = Logger.getLogger(RecyclerViewAdapter.class);


    }

    public RealmResults getRealmResults()
    {
        return this.getData().sort("position");
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        SwipeLayout nodeView = (SwipeLayout) inflater.inflate(R.layout.grid_item, parent, false);

        nodeView.setShowMode(SwipeLayout.ShowMode.LayDown);



        return new ItemHolder(nodeView, this);
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {

        LOG.debug("Binding item in position " + position);
        Node n = getItem(position);
        if (n != null) {
            holder.setCurrentNode(n);
            holder.loadImage();
        }

        if (mDragStartListener != null) {

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
        } else {
            holder.handle.setOnTouchListener(null);
        }
    }




    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    private void RepositionNode() {
        LOG.debug("onItemClear! endPosition " + endPosition + " origPosition " + origPosition );

        if (origPosition == -1 || endPosition == -1 || (origPosition == endPosition)) return;

        try{
            Realm realm = Realm.getDefaultInstance();

            List<Node> list = new ArrayList<>();
            list.addAll(getRealmResults());

            LOG.debug("Swaping position " + origPosition + " to position " + endPosition);

            Node orig = list.remove(origPosition);
            list.add(endPosition, orig);


            //--------------------------
            realm.beginTransaction();

            int position = 0;
            for (int i = 0; i < list.size(); i++) {
                Node c = list.get(i);
                position = list.size() - i;

                if (c.getPosition() != position) {
                    Log.i("SAVE", "changing position for " + c.getName());
                    c.setPosition(position);
                }
            }
            realm.commitTransaction();
            realm.close();
            //---------------------

            //realmResults.sort("position", Sort.DESCENDING);

            if (mDragStartListener != null)
                mDragStartListener.onStopDrag(null);

            origPosition = -1;
            endPosition = -1;
        }
        catch (Exception e){
            LOG.error(e.toString());
        }
        finally {

        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        LOG.debug("Item moved from " + fromPosition + " to position " + toPosition);
        if (origPosition == -1) origPosition = fromPosition;
        endPosition = toPosition;
        RepositionNode();

        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        LOG.debug("Dismissed from position " + position);
    }

    public interface OnItemClickListener {
        public void onItemClick(ItemHolder item, int position);
        public void onItemEditClick(ItemHolder item, int position);
        public void onDeleteClick(ItemHolder item, int position);
    }

    public class ItemHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder,View.OnClickListener, View.OnLongClickListener {

        private RecyclerViewAdapter parent;
        private SwipeLayout nodeView;

        private TextView textItemName;
        private TextView textItemValue;
        public ImageView handle;
        public ImageView edit;
        public ImageView delete;
        private FrameLayout nodeHolder;
        public ImageView snapshot;

        public Node getCurrentNode() {
            return currentNode;
        }

        public void setCurrentNode(Node currentNode) {
            this.currentNode = currentNode;
        }

        private Node currentNode;


        public SwipeLayout getNodeView() {
            return nodeView;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        private int type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        private String id;

        public ItemHolder(SwipeLayout nView, RecyclerViewAdapter parent) {
            super(nView);

            nodeView = nView;
            nodeView.setOnClickListener(this);
            nodeView.setClickToClose(true);



            nodeHolder =  (FrameLayout) nodeView.findViewById(R.id.nodeHolder);
            nodeHolder.setOnLongClickListener(this);


            this.parent = parent;
            textItemName = (TextView) nodeView.findViewById(R.id.name);
            handle = (ImageView) nodeView.findViewById(R.id.handle);
            edit = (ImageView) nodeView.findViewById(R.id.edit);
            delete = (ImageView) nodeView.findViewById(R.id.delete);
            snapshot = (ImageView) nodeView.findViewById(R.id.snapshot);

            if(edit != null){
                edit.setOnClickListener(this);
            }
            if(delete != null){
                delete.setOnClickListener(this);
            }

            if(textItemName != null && currentNode != null){
                textItemName.setText(currentNode.getName());
            }


        }

        public boolean loadImage(){

            if(currentNode != null && currentNode.getType() == Node.WEBCAM){

                String url = currentNode.getWebcamURL();
                String pass = currentNode.getWebcamPassword();
                String unam = currentNode.getWebcamUsername();

                if(!url.contains("http://")){
                    url = "http://" + url;
                }

                if(URLUtil.isValidUrl(url))
                {
                    //"http://kvajpoj.com:4500/image.jpg?t=" + (System.currentTimeMillis()/1000
                    if(pass == null) pass = "";
                    if(unam == null) unam = "";

                    GlideUrl glideUrl = new GlideUrl(url,
                                        new LazyHeaders.Builder()
                                            .addHeader("Authorization", "Basic " + Base64.encodeToString((unam + ":" + pass).getBytes(), Base64.NO_WRAP))
                                        .build());

                    Glide.with(context)
                            .load(glideUrl)
                            .dontAnimate()

                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .error(R.drawable.ic_lock_outline_24dp)
                            .placeholder(snapshot.getDrawable())
                            .into(snapshot);

                    return true;
                }
                else
                {
                    LOG.debug("Current Webcam URL is not valid! " + currentNode.getName() + " " +url);
                    return false;
                }
            }
            else
            {
                Glide.clear(snapshot);
                snapshot.setImageDrawable(null);
                return false;
            }
        }

        public void setItemName(CharSequence name) {
            textItemName.setText(name);
        }

        public CharSequence getItemName() {
            return textItemName.getText();
        }

        @Override
        public void onClick(View v) {
            LOG.debug("Item click!");
            final OnItemClickListener listener = parent.getOnItemClickListener();
            if (listener != null) {

                if(v == delete){
                    listener.onDeleteClick(this, getPosition());
                    return;
                }
                if(v == edit){
                    listener.onItemEditClick(this, getPosition());
                    return;
                }


                listener.onItemClick(this, getPosition());
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
            nodeView.open(true);
            return true;
        }


    }
}

