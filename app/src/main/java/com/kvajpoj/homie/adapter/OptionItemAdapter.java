package com.kvajpoj.homie.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kvajpoj.homie.R;
import com.kvajpoj.homie.model.OptionItem;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by andrej on 13.3.2016.
 */
public class OptionItemAdapter extends ArrayAdapter<OptionItem> {

    private Logger LOG;

    public OptionItemAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        LOG = Logger.getLogger(OptionItemAdapter.class);
    }

    public OptionItemAdapter(Context context, int resource, List<OptionItem> items) {
        super(context, resource, items);
        LOG = Logger.getLogger(OptionItemAdapter.class);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.option_item, null);
        }

        OptionItem p = getItem(position);

        if (p != null) {
            TextView name = (TextView) v.findViewById(R.id.item_name);
            TextView desc = (TextView) v.findViewById(R.id.item_desc);
            ImageView icon = (ImageView) v.findViewById(R.id.item_icon);

            if (name != null) {
                name.setText(p.getName());
            }

            if (desc != null) {
                desc.setText(p.getDesc());
            }

            if(icon != null){
                icon.setImageDrawable(getContext().getDrawable(p.getIcon()));
            }


        }

        return v;
    }

}
