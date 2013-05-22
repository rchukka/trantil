package com.rchukka.trantil.test.datastore;

import com.rchukka.trantil.content.DataStore;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.Table;
import com.rchukka.trantil.test.R;
import com.rchukka.trantil.test.common.TestActivityUnit;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

@TestActivityUnit.Activity(name = "Data Store Test",
        desc = "Notification test", order = 1)
public class DataStoreNotification extends FragmentActivity {

    private Button         addTop;
    private Button         addBottom;
    private TextView       log;
    private Cursor         cTop;
    private Cursor         cBottom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notificationactivity);
        addTop = (Button) findViewById(R.id.add_top);
        addBottom = (Button) findViewById(R.id.add_bottom);
        log = (TextView) findViewById(R.id.log_view);

        DataStore.delete(Top.class, null, null);
        DataStore.delete(Bottom.class, null, null);

        addTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSomeRows(Top.class);
            }
        });

        addBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSomeRows(Bottom.class);
            }
        });

        cTop = DataStore.query(Top.class, null, null, null, null, true);
        cTop.registerContentObserver(new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                addToLog("Received change in top class");
            }
        });

        cBottom = DataStore.query(Bottom.class, null, null, null, null, true);
        cBottom.registerContentObserver(new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                addToLog("Received change in bottom class");
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        
        cTop.close();
        cBottom.close();
    }

    @SuppressWarnings("rawtypes")
    private void addSomeRows(Class klass) {
        ContentValues cv = new ContentValues();
        cv.put("mBookId", System.currentTimeMillis());
        cv.put("mTitle", "Title :" + klass.getSimpleName());
        DataStore.insert(klass, cv, true);
    }

    private void addToLog(String msg) {
        log.append(msg + "\n");
    }

    @Table(version = 0)
    public class Top {

        @Column(isKey = true) private long mBookId;
        @Column private String             mTitle;
    }

    @Table(version = 0)
    public class Bottom {

        @Column(isKey = true) private long mBookId;
        @Column private String             mTitle;
    }
    
}
