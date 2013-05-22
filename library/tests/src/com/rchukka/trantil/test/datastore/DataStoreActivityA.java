package com.rchukka.trantil.test.datastore;

import com.rchukka.trantil.content.CursorColumnMap;
import com.rchukka.trantil.content.DataStore;
import com.rchukka.trantil.content.StoreCursorLoader;
import com.rchukka.trantil.test.R;
import com.rchukka.trantil.test.common.TestActivityUnit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TwoLineListItem;

@TestActivityUnit.Activity(name = "Data Store Test", desc = "Model Pattern A",
        order = 1)
public class DataStoreActivityA extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private LayoutInflater  inflater;
    private ListView        list;
    private CursorAdapter   adapter;
    private boolean         stopped = false;
    private CursorColumnMap cursorMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listitemactivity);
        inflater = getLayoutInflater();
        list = (ListView) findViewById(R.id.list);

        DataStore.delete(ModelPatternA.class, null, null);
        adapter = new ListAdapter(this, null, 0);
        list.setAdapter(adapter);
        getSupportLoaderManager().initLoader(1, savedInstanceState, this);

        new Thread() {
            @Override
            public void run() {
                int pie = 0;
                while (pie < 20) {
                    if (stopped) return;
                    addSomeRows();
                    DataStore.notifyChange(ModelPatternA.class);
                    SystemClock.sleep(500);
                    pie++;
                }
            }
        }.start();
    }

    @Override
    public void onStop() {
        stopped = true;
        super.onStop();
    }

    private void addSomeRows() {
        ContentValues cv = new ContentValues();
        cv.put(ModelPatternA.BOOK_ID, System.currentTimeMillis());
        cv.put(ModelPatternA.TITLE, "Title :" + this.getClass().getSimpleName());
        DataStore.insert(ModelPatternA.class, cv);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        StoreCursorLoader loader = new StoreCursorLoader(this,
                ModelPatternA.class);
        loader.setProjection(new String[] { "0 as _id", ModelPatternA.BOOK_ID,
                ModelPatternA.TITLE });
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        DataStore.registerForChanges(cursor, ModelPatternA.class);
        if (cursorMap == null) cursorMap = CursorColumnMap.buildMap(cursor);
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        adapter.swapCursor(null);
    }

    public class ListAdapter extends CursorAdapter {

        public ListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(android.R.layout.simple_list_item_2, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            TwoLineListItem v = (TwoLineListItem) view;
            v.getText1().setText(cursorMap.getString(c, ModelPatternA.TITLE, ""));
            v.getText2().setText(cursorMap.getInt(c, ModelPatternA.BOOK_ID, 0) + "");
        }
    }
}
