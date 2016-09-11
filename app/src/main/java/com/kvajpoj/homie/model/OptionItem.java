package com.kvajpoj.homie.model;

/**
 * Created by andrej on 13.3.2016.
 */
public class OptionItem {

    int type;
    String name;
    String desc;
    int icon;

    public OptionItem(int type, String name, String desc, int icon) {
        this.type = type;
        this.name = name;
        this.desc = desc;
        this.icon = icon;
    }


    public OptionItem(int type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

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

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


}
