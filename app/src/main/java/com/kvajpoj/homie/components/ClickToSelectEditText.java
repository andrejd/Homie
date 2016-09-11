package com.kvajpoj.homie.components;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;

import com.kvajpoj.homie.adapter.OptionItemAdapter;
import com.kvajpoj.homie.model.OptionItem;

/**
 * Created by andrej on 3.3.2016.
 */
public class ClickToSelectEditText <T> extends TextInputEditText {

    CharSequence mHint;

    OnItemSelectedListener<T> onItemSelectedListener;
    OptionItemAdapter mSpinnerAdapter;

    public ClickToSelectEditText(Context context) {
        super(context);

        mHint = getHint();
    }

    public ClickToSelectEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHint = getHint();
    }

    public ClickToSelectEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHint = getHint();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setFocusable(false);
        setClickable(true);
        setLongClickable(false);
    }

    public void setAdapter(OptionItemAdapter adapter) {
        mSpinnerAdapter = adapter;

        configureOnClickListener();
    }

    private void configureOnClickListener() {

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(mHint);
                builder.setAdapter(mSpinnerAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int selectedIndex) {
                        if (onItemSelectedListener != null) {
                            onItemSelectedListener.onItemSelectedListener((T) mSpinnerAdapter.getItem(selectedIndex), selectedIndex);
                        }
                    }
                });
                builder.setPositiveButton(android.R.string.cancel, null);
                builder.create().show();
            }


        });
    }

    public void setOnItemSelectedListener(OnItemSelectedListener<OptionItem> onItemSelectedListener) {
        this.onItemSelectedListener = (OnItemSelectedListener<T>) onItemSelectedListener;
    }

    public interface OnItemSelectedListener<T> {
        void onItemSelectedListener(T item, int selectedIndex);
    }
}