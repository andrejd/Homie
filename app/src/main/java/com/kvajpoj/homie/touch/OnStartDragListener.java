package com.kvajpoj.homie.touch;

import android.support.v7.widget.RecyclerView;

/**
 * Created by Andrej on 16.9.2015.
 */
public interface OnStartDragListener {
    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onStartDrag(RecyclerView.ViewHolder viewHolder);

    /**
     * Called when a view is stopped dragging.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onStopDrag(RecyclerView.ViewHolder viewHolder);
}