package com.rchukka.trantil.test.common;

import com.rchukka.trantil.content.DataStore;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.Table;
import com.rchukka.trantil.test.R;
import com.rchukka.trantil.test.unit.Xml2CVHandlerTest;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

@TestActivityUnit.Activity(name = "Performance Activity",
        desc = "Performance test", order = 1)
public class PerformanceActivity extends FragmentActivity {

    private Button         bookxmlperf;
    private Button         filexmlperf;
    private TextView       log;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.performanceactivity);
        bookxmlperf = (Button) findViewById(R.id.bookxmlperf);
        filexmlperf = (Button) findViewById(R.id.filexmlperf);
        log = (TextView) findViewById(R.id.log_view);

        bookxmlperf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Xml2CVHandlerTest xvt = new Xml2CVHandlerTest();
                long time = 0;
                try {
                    time = System.currentTimeMillis();
                    xvt._testBooksXml("books-big.xml", getApplicationContext());
                    time = System.currentTimeMillis() - time;
                } catch (Exception e) {
                    addToLog(Log.getStackTraceString(e));
                }
                
                addToLog("Book Test Finished:" + time + "ms");
            }
        });
        
        filexmlperf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Xml2CVHandlerTest xvt = new Xml2CVHandlerTest();
                long time = 0;
                try {
                    time = System.currentTimeMillis();
                    xvt._testFolderXml("folder-big.xml", getApplicationContext());
                    time = System.currentTimeMillis() - time;
                } catch (Exception e) {
                    addToLog(Log.getStackTraceString(e));
                }
                
                addToLog("Folder Test Finished:" + time + "ms");
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void addToLog(String msg) {
        log.append(msg + "\n");
    }
}
