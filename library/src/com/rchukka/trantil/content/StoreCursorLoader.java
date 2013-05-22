package com.rchukka.trantil.content;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

@SuppressWarnings("rawtypes")
public class StoreCursorLoader extends CursorLoader {

    final ForceLoadContentObserver mObserver;
    Class                          mCls;
    private CursorColumnMap        mCursorMap;
    private WeakReference<Cursor>  mCursor;

    public StoreCursorLoader(Context context, Class cls) {
        super(context);
        mCls = cls;
        mObserver = new ForceLoadContentObserver();
    }

    public StoreCursorLoader(Context context, Class cls, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        super(context, null, projection, selection, selectionArgs, sortOrder);
        mObserver = new ForceLoadContentObserver();
        mCls = cls;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor cursor = DataStore.query(mCls, getProjection(), getSelection(),
                getSelectionArgs(), getSortOrder());
        if (cursor != null) {
            // Ensure the cursor window is filled
            cursor.getCount();
            cursor.registerContentObserver(mObserver);
        }
        mCursor = new WeakReference<Cursor>(cursor);
        return cursor;
    }

    public CursorColumnMap getColumnMap() {
        if (mCursorMap == null) {
            Cursor c = mCursor.get();
            if (c == null)
                throw new RuntimeException(
                        "CursorColumnMap can't be built as cursor not loaded yet"
                                + " or is old or invalid for this loader");
            mCursorMap = CursorColumnMap.buildMap(c);
        }

        return mCursorMap;
    }

}
