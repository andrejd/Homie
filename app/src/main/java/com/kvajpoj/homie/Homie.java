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

/**
 * Created by Andrej on 29.8.2015.
 */
public class Homie extends Application {

    @Override
    public void onCreate() {

        RealmConfiguration config = new RealmConfiguration.Builder(getApplicationContext())
                .deleteRealmIfMigrationNeeded()
                .name("homie.realm")
                .build();
        Realm.setDefaultConfiguration(config);

        LogConfigurator logConfigurator = new LogConfigurator();
        logConfigurator.setFileName(Environment.getExternalStorageDirectory()
                + File.separator + "MyApp" + File.separator + "logs"
                + File.separator + "log4j.txt");
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
        logConfigurator.setMaxFileSize(1024 * 1024 * 5);
        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();

        Logger LOG;
        LOG = Logger.getLogger(Homie.class);

        LOG.debug(logConfigurator.getFileName());

        super.onCreate();
    }


}
