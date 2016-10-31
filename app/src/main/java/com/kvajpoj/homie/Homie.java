package com.kvajpoj.homie;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;
import io.realm.Realm;
import io.realm.RealmConfiguration;


public class Homie extends Application {

    @Override
    public void onCreate() {

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("homie.realm")
                .build();

        Realm.setDefaultConfiguration(config);

        LogConfigurator logConfigurator = new LogConfigurator();

        // check if external storage is connected, e.g. sd card
        //logConfigurator.setFileName(Environment.getExternalStorageDirectory()
        //        + File.separator + "MyApp" + File.separator + "logs"
        //        + File.separator + "log4j.txt");
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
        logConfigurator.setUseFileAppender(false);
        //logConfigurator.setMaxFileSize(1024 * 1024 * 5);
        //logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();

        //Logger LOG;
        //LOG = Logger.getLogger(Homie.class);

        //LOG.debug(logConfigurator.getFileName());

        super.onCreate();
    }


}
