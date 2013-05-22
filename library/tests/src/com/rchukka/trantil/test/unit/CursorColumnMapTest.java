package com.rchukka.trantil.test.unit;

import com.rchukka.trantil.content.CursorColumnMap;
import com.rchukka.trantil.content.DataStore;
import com.rchukka.trantil.content.TestDataStore;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.ColumnInt;
import com.rchukka.trantil.content.type.Table;
import com.rchukka.trantil.test.unit.DataStoreTest.table_one;

import android.content.ContentValues;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

public class CursorColumnMapTest extends InstrumentationTestCase {

    protected void setUp() throws Exception {
        TestDataStore.init(getInstrumentation().getTargetContext(), 2);
        TestDataStore.dropAllTables();
    }

    public void testQuery() {
        
        @Table(version = 0)
        class inv_col {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final String            DATA_1     = "DATA_1";
            @Column public static final String               DATA_2     = "DATA_2";
        }

        DataStore.delete(inv_col.class, null, null);
        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "MUL_K1");
        cv.put(table_one.DATA_1, 1);
        cv.put(table_one.DATA_2, "MUL_K2");
        DataStore.insert(inv_col.class, cv);

        Cursor c = DataStore.query(inv_col.class, null,
                table_one.DATA_1 + "=1", null, null);

        c.moveToFirst();
        CursorColumnMap cMap = CursorColumnMap.buildMap(c);
        assertEquals("row count is not correct", 1, c.getCount());

        assertEquals("invalid data in column", "MUL_K1",
                cMap.getString(c, inv_col.KEY_COLUMN, null));
        assertEquals("invalid data in column", 1,
                cMap.getInt(c, inv_col.DATA_1, -1));
        assertEquals("invalid data in column", "MUL_K2",
                cMap.getString(c, inv_col.DATA_2, null));

        assertEquals("invalid column was not detected", "INVALID",
                cMap.getString(c, "TEST_COL", "INVALID"));
        c.close();
        
        
        c = DataStore.query(inv_col.class, new String[]{table_one.KEY_COLUMN, table_one.DATA_1},
                null, null, null);

        c.moveToFirst();
        cMap = CursorColumnMap.buildMap(c);
        assertEquals("invalid data in column", "MUL_K1",
                cMap.getString(c, inv_col.KEY_COLUMN, null));
        assertEquals("invalid data in column", 1,
                cMap.getInt(c, inv_col.DATA_1, -1));
        assertEquals("invalid data in column", null,
                cMap.getString(c, inv_col.DATA_2, null));
        c.close();
    }
}
