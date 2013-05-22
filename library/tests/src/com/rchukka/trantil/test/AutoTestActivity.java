package com.rchukka.trantil.test;

import java.util.List;

import com.rchukka.trantil.test.common.TestActivityUnit;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TwoLineListItem;

@SuppressWarnings("rawtypes")
public class AutoTestActivity extends ListActivity {

    static final String         TAG            = "MainActivity";
    private ArrayAdapter<Class> adapter;
    private List<Class>         testActivities = null;
    private LayoutInflater      inflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        inflater = (LayoutInflater) getApplicationContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        testActivities = TestActivityUnit.getTestActivities(this
                .getApplicationContext());

        adapter = new ArrayAdapter<Class>(this,
                android.R.layout.simple_list_item_2, testActivities) {
            @SuppressWarnings("unchecked")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TwoLineListItem row;
                if (convertView == null) {
                    row = (TwoLineListItem) inflater.inflate(
                            android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                Class act = testActivities.get(position);
                TestActivityUnit.Activity an = (TestActivityUnit.Activity) act
                        .getAnnotation(TestActivityUnit.Activity.class);

                row.getText1().setText(an.name());
                row.getText2().setText(an.desc());

                return row;
            }
        };

        setListAdapter(adapter);

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(AutoTestActivity.this,
                        testActivities.get(position));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}