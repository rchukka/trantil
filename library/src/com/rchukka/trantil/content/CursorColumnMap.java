package com.rchukka.trantil.content;

import java.util.HashMap;

import android.database.Cursor;

public class CursorColumnMap {

    private final HashMap<String, Integer> mNameToIndex;

    private CursorColumnMap(Cursor c) {
        mNameToIndex = new HashMap<String, Integer>(10, 0.9f);
        String[] colNames = c.getColumnNames();
        for(int i=0; i < colNames.length;i++){
            mNameToIndex.put(colNames[i], i);
        }
    }
    
    public static CursorColumnMap buildMap(Cursor c){
        return new CursorColumnMap(c);
    }
    
    public int getColumnIndex(String columnName){
        Integer colIndex = mNameToIndex.get(columnName);
        return colIndex;
    }

    public String getString(Cursor c, String columnName,
            String defaultValue) {

        String out = defaultValue;
        Integer colIndex = mNameToIndex.get(columnName);
        if (colIndex != null) out = c.getString(colIndex);
        return out;
    }

    public int getInt(Cursor c, String columnName, int defaultValue) {

        int out = defaultValue;
        Integer colIndex  = mNameToIndex.get(columnName);
        if (colIndex != null) out = c.getInt(colIndex);
        return out;
    }

    public long getLong(Cursor c, String columnName, long defaultValue) {

        long out = defaultValue;
        Integer colIndex = mNameToIndex.get(columnName);
        if (colIndex != null) out = c.getLong(colIndex);
        return out;
    }

    public float getFloat(Cursor c, String columnName, float defaultValue) {

        float out = defaultValue;
        Integer colIndex = mNameToIndex.get(columnName);
        if (colIndex != null) out = c.getFloat(colIndex);
        return out;
    }
}
