package com.rchukka.trantil.test.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.rchukka.trantil.content.DataStore;
import com.rchukka.trantil.content.TestDataStore;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.ColumnInt;
import com.rchukka.trantil.content.type.ColumnReal;
import com.rchukka.trantil.content.type.DataType.DataTypeException;
import com.rchukka.trantil.content.type.Table;

public class DataStoreTest extends InstrumentationTestCase {

    private static final String TAG           = "DataStoreTest";
    private static boolean      initCompleted = false;

    static class LooperThread extends Thread {
        @Override
        public void start() {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    LooperThread.this.run();
                    Looper.loop();
                }
            }.start();
        }
    }

    @Table(version = 0)
    class table_one {
        @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
        @ColumnInt public static final String            DATA_1     = "DATA_1";
        @Column public static final String               DATA_2     = "DATA_2";
    }

    protected void setUp() throws Exception {
        if (initCompleted) return;
        TestDataStore.init(getInstrumentation().getTargetContext(), 2);
        TestDataStore.dropAllTables();

        assertEquals("deleted db still around.", false,
                TestDataStore.tableExists(table_one.class));
        DataStore.delete(table_one.class, null, null);
        assertEquals("table not found.", true,
                TestDataStore.tableExists(table_one.class));

        initCompleted = true;
    }

    public void testMemLeak() {
        // if there is leak you will see it in logcat.
        TestDataStore.init(getInstrumentation().getTargetContext(), 2);
        TestDataStore.dropAllTables();

        assertEquals("deleted db still around.", false,
                TestDataStore.tableExists(table_one.class));
        DataStore.delete(table_one.class, null, null);
        assertEquals("table not found.", true,
                TestDataStore.tableExists(table_one.class));

        TestDataStore.init(getInstrumentation().getTargetContext(), 2);
        TestDataStore.dropAllTables();

        assertEquals("deleted db still around.", false,
                TestDataStore.tableExists(table_one.class));
        DataStore.delete(table_one.class, null, null);
        assertEquals("table not found.", true,
                TestDataStore.tableExists(table_one.class));
    }

    public void testCreate() {
        DataStore.delete(table_one.class, null, null);
        Cursor c = DataStore.query(table_one.class, null, null, null, null);
        assertEquals(0, c.getCount());
        c.close();

        @Table(version = 0)
        class ModelPatternB {

            @Column(isKey = true) private String mType;
            @Column private long                 mRetrievedTime;
            @Column private String               mData;
            @Column private int                  mRefreshInterval;
            @Column private long                 mInvalidAfter;
        }

        DataStore.delete(ModelPatternB.class, null, null);
    }

    public void testInsert() {

        DataStore.delete(table_one.class, null, null);
        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "Test 1");
        cv.put(table_one.DATA_1, "1");
        cv.put(table_one.DATA_2, "DATA_2_1");
        DataStore.insert(table_one.class, cv);

        Cursor c = DataStore.query(table_one.class, null, null, null, null);
        assertEquals("expected row not found in table", 1, c.getCount());
        c.moveToFirst();
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 1);
        assertEquals(
                "column data is wrong",
                true,
                c.getString(c.getColumnIndex(table_one.DATA_2)).equals(
                        "DATA_2_1"));
        c.close();

        cv.clear();
        cv.put(table_one.KEY_COLUMN, "Test 2");
        cv.put(table_one.DATA_1, "2");
        cv.put(table_one.DATA_2, "DATA_2_2");
        DataStore.insert(table_one.class, cv);

        c = DataStore.query(table_one.class, null, null, null, null);
        assertEquals("expected row not found in table", 2, c.getCount());
        c.moveToFirst();
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 1);
        assertEquals(
                "column data is wrong",
                true,
                c.getString(c.getColumnIndex(table_one.DATA_2)).equals(
                        "DATA_2_1"));
        c.moveToNext();
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 2);
        assertEquals(
                "column data is wrong",
                true,
                c.getString(c.getColumnIndex(table_one.DATA_2)).equals(
                        "DATA_2_2"));
        c.close();
    }

    public void testConflictingInsert() {

        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "CON_1");
        cv.put(table_one.DATA_1, "1");
        cv.put(table_one.DATA_2, "DATA_2_1");
        DataStore.insert(table_one.class, cv);

        try {
            DataStore.insert(table_one.class, cv);
            fail("failed to detect duplicate record insert");
        } catch (SQLiteConstraintException ex) {
        }
    }

    public void testDuplicateCreate() {
        @Table(version = 0, name = "peach")
        class con_one {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final String            DATA_1     = "DATA_1";
            @Column public static final String               DATA_2     = "DATA_2";
        }

        @Table(version = 0, name = "peach")
        class con_two {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final String            DATA_1     = "DATA_1";
            @Column public static final String               DATA_2     = "DATA_2";
        }

        assertEquals("Incorrect table name",
                DataStore.getTableName(con_one.class), "peach");

        try {
            DataStore.getTableName(con_two.class);
            fail("failed to detect duplicate table create");
        } catch (DataTypeException ex) {
        }
    }

    public void testUpdate() {

        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "UP_1");
        cv.put(table_one.DATA_1, 5);
        cv.put(table_one.DATA_2, "UP_D2");
        DataStore.insert(table_one.class, cv);

        Cursor c = DataStore.query(table_one.class, null, table_one.KEY_COLUMN
                + "='UP_1'", null, null);
        assertEquals("expected row not found in table", 1, c.getCount());
        c.moveToFirst();
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 5);
        assertEquals("column data is wrong", true,
                c.getString(c.getColumnIndex(table_one.DATA_2)).equals("UP_D2"));
        c.close();

        cv.clear();
        cv.put(table_one.DATA_1, 3);
        cv.put(table_one.DATA_2, "UP_D4");
        DataStore.insert(table_one.class, cv);

        int rowsUpdated = DataStore.update(table_one.class, cv,
                table_one.KEY_COLUMN + "='UP_1'", null);
        assertEquals("number of updated rows is wrong.", 1, rowsUpdated);

        c = DataStore.query(table_one.class, null, table_one.KEY_COLUMN
                + "='UP_1'", null, null);
        assertEquals("expected row not found in table", 1, c.getCount());
        c.moveToFirst();
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 3);
        assertEquals("column data is wrong", true,
                c.getString(c.getColumnIndex(table_one.DATA_2)).equals("UP_D4"));
        c.close();
    }

    public void testDelete() {

        @Table(version = 0, name = "cache")
        class del_one {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final String            DATA_1     = "DATA_1";
            @Column public static final String               DATA_2     = "DATA_2";
        }

        DataStore.delete(del_one.class, null, null);
        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "DL_te");
        cv.put(table_one.DATA_1, 1);
        cv.put(table_one.DATA_2, "DL_D2");
        DataStore.insert(del_one.class, cv);

        int deleted = DataStore.delete(del_one.class, null, null);
        assertEquals("delete count is not correct", 1, deleted);

        cv.clear();
        cv.put(table_one.KEY_COLUMN, "DL_1");
        cv.put(table_one.DATA_1, 2);
        cv.put(table_one.DATA_2, "DL_D2");
        DataStore.insert(del_one.class, cv);

        Cursor c = DataStore.query(del_one.class, null,
                table_one.DATA_1 + "=2", null, null);
        assertEquals("expected row not found in table", 1, c.getCount());
        c.moveToFirst();
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 2);
        assertEquals("column data is wrong", true,
                c.getString(c.getColumnIndex(table_one.DATA_2)).equals("DL_D2"));
        c.close();

        cv.clear();
        cv.put(table_one.KEY_COLUMN, "DL_2");
        cv.put(table_one.DATA_1, 3);
        cv.put(table_one.DATA_2, "UP_D4");
        DataStore.insert(del_one.class, cv);

        c = DataStore.query(del_one.class, null, null, null, null);
        assertEquals("row count is not correct", 2, c.getCount());
        c.close();

        deleted = DataStore.delete(del_one.class, null, null);
        assertEquals("delete count is not correct", 2, deleted);
    }

    public void testMultipleKey() {

        @Table(version = 0)
        class mul_key {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final String            DATA_1     = "DATA_1";
            @Column(isKey = true) public static final String DATA_2     = "DATA_2";
        }

        DataStore.delete(mul_key.class, null, null);
        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "MUL_K1");
        cv.put(table_one.DATA_1, 1);
        cv.put(table_one.DATA_2, "MUL_K2");
        DataStore.insert(mul_key.class, cv);

        Cursor c = DataStore.query(mul_key.class, null,
                table_one.DATA_1 + "=1", null, null);
        assertEquals("expected row not found in table", 1, c.getCount());
        c.moveToFirst();
        assertEquals(
                "column data is wrong",
                true,
                c.getString(c.getColumnIndex(table_one.KEY_COLUMN)).equals(
                        "MUL_K1"));
        assertEquals("column data is wrong", true,
                c.getInt(c.getColumnIndex(table_one.DATA_1)) == 1);
        assertEquals("column data is wrong", true,
                c.getString(c.getColumnIndex(table_one.DATA_2))
                        .equals("MUL_K2"));
        c.close();

        cv.clear();
        cv.put(table_one.KEY_COLUMN, "MUL_K1");
        cv.put(table_one.DATA_1, 3);
        cv.put(table_one.DATA_2, "MUL_K3");
        DataStore.insert(mul_key.class, cv);

        c = DataStore.query(mul_key.class, null, null, null, null);
        assertEquals("row count is not correct", 2, c.getCount());
        c.close();

        try {
            DataStore.insert(mul_key.class, cv);
            fail("failed to detect duplicate record insert");
        } catch (SQLiteConstraintException ex) {
        }

        c = DataStore.query(mul_key.class, null, null, null, null);
        assertEquals("row count is not correct", 2, c.getCount());
        c.close();
    }

    public void testInvalidColumnInsert() {

        @Table(version = 0)
        class inv_col {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final String            DATA_1     = "DATA_1";
        }

        DataStore.delete(inv_col.class, null, null);
        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "MUL_K1");
        cv.put(table_one.DATA_1, 1);
        cv.put(table_one.DATA_2, "MUL_K2");

        try {
            DataStore.insert(inv_col.class, cv);
            fail("failed to detect invalid column");
        } catch (SQLiteException ex) {
        }

        try {
            DataStore.insert(inv_col.class, cv);
            fail("failed to detect invalid column");
        } catch (SQLiteException ex) {
        }

        Cursor c = DataStore.query(inv_col.class, null,
                table_one.DATA_1 + "=1", null, null);
        assertEquals("row count is not correct", 0, c.getCount());
        c.close();
    }

    @Table(version = 0)
    static class field_inv_type4 {
        @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
        @ColumnInt public static int                     DATA_1     = 0;
    }

    public void testInvalidTableModel() {

        @Table(version = 3)
        class field_value_mismatch {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @Column public static final String               DATA_1     = "DATA_2";
        }

        try {
            DataStore.delete(field_value_mismatch.class, null, null);
            fail("failed to detect final value and field name mismatch");
        } catch (DataTypeException ex) {
        }

        @Table(version = 0)
        class field_inv_type {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @Column public static final int                  DATA_1     = 0;
        }

        try {
            DataStore.delete(field_inv_type.class, null, null);
            fail("failed to detect invalid type for final value field");
        } catch (DataTypeException ex) {
        }

        @Table(version = 0)
        class field_inv_type2 {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public static final int               DATA_1     = 0;
        }

        try {
            DataStore.delete(field_inv_type2.class, null, null);
            fail("failed to detect invalid ColumnInt annotation for final value field");
        } catch (DataTypeException ex) {
        }

        @Table(version = 0)
        class field_inv_type3 {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public final int                      DATA_1     = 0;
        }

        try {
            DataStore.delete(field_inv_type3.class, null, null);
            fail("failed to detect final only field");
        } catch (DataTypeException ex) {
        }

        try {
            DataStore.delete(field_inv_type4.class, null, null);
            fail("failed to detect static only field");
        } catch (DataTypeException ex) {
        }

        @Table(version = 0)
        class field_inv_type5 {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnInt public String                         DATA_1     = "BEA";
        }

        try {
            DataStore.delete(field_inv_type5.class, null, null);
            fail("failed to detect invalid @ColumnInt annotation");
        } catch (DataTypeException ex) {
        }
    }

    public void testRealColumnType() {
        @Table(version = 0)
        class r_col {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @ColumnReal public static final String           DATA_1     = "DATA_1";
        }

        DataStore.delete(r_col.class, null, null);
        ContentValues cv = new ContentValues();
        cv.put(table_one.KEY_COLUMN, "MUL_K1");
        cv.put(table_one.DATA_1, 1.2f);

        DataStore.insert(r_col.class, cv);

        Cursor c = DataStore.query(r_col.class, null, null, null, null);
        assertEquals("row count is not correct", 1, c.getCount());
        c.moveToFirst();
        assertEquals("row count is not correct", 1.2f,
                c.getFloat(c.getColumnIndex(table_one.DATA_1)));
        c.close();

        c = DataStore.query(r_col.class, null, table_one.DATA_1
                + " BETWEEN 1.2 AND 1.3", null, null);
        assertEquals("row count is not correct", 1, c.getCount());
        c.moveToFirst();
        assertEquals("row count is not correct", 1.2f,
                c.getFloat(c.getColumnIndex(table_one.DATA_1)));
        c.close();
    }

    public void testCreateSpeed() {

        long start = System.currentTimeMillis();
        @Table(version = 0)
        class ten_col {
            @Column(isKey = true) public String KEY_COLUMN = "KEY_COLUMN";
            @Column public String               DATA_1     = "DATA_1";
            @Column public String               DATA_2     = "DATA_2";
            @Column public int                  DATA_3     = 0;
            @Column public long                 DATA_4     = 2l;
            @Column public float                DATA_5     = 2.3f;
            @Column public String               DATA_6     = "DATA_2";
            @Column public String               DATA_7     = "DATA_2";
            @Column public String               DATA_8     = "DATA_2";
            @Column public String               DATA_9     = "DATA_2";
        }

        DataStore.query(ten_col.class, null, null, null, null).close();

        start = System.currentTimeMillis() - start;

        Log.i("DataStoreTest", "Time to create 10 column table: " + start
                + "ms");
    }

    public void testBulkInsert() {
        @Table(version = 0)
        class bulk_col {
            @Column(isKey = true) public static final String KEY_COLUMN = "KEY_COLUMN";
            @Column public static final String               DATA_1     = "DATA_1";
            @Column public static final String               DATA_2     = "DATA_2";
        }

        DataStore.delete(bulk_col.class, null, null);
        List<ContentValues> cvs = new ArrayList<ContentValues>(20);

        for (int i = 0; i < 20; i++) {
            ContentValues cv = new ContentValues();
            cv.put(table_one.KEY_COLUMN, "MUL_K1" + i);
            cv.put(table_one.DATA_1, "test file 1");
            cv.put(table_one.DATA_2, "test file 1");
            cvs.add(cv);
        }

        long sqIn = System.currentTimeMillis();
        for (ContentValues c : cvs) {
            DataStore.insert(bulk_col.class, c);
        }
        long nonBulk = System.currentTimeMillis() - sqIn;
        Log.d(TAG, "Non-Transaction insert time: " + nonBulk + "ms");

        DataStore.delete(bulk_col.class, null, null);

        sqIn = System.currentTimeMillis();
        DataStore.insert(bulk_col.class, cvs);
        long bulk = System.currentTimeMillis() - sqIn;
        Log.d(TAG, "Non-Transaction insert time: " + bulk + "ms");

        assertEquals("Bulk insert was not fast", true, nonBulk > (5 * bulk));
    }

    int            firstNotifications  = 0;
    int            secondNotifications = 0;
    Cursor         firstC              = null;
    Cursor         secondC             = null;
    CountDownLatch latch;

    public void testNotifications() {
        this.latch = new CountDownLatch(2);

        @Table(version = 0)
        class first {
            @Column(isKey = true) private long mBookId;
            @Column private String             mTitle;
        }

        @Table(version = 0)
        class second {
            @Column(isKey = true) private long mBookId;
            @Column private String             mTitle;
        }

        firstNotifications = 0;
        secondNotifications = 0;

        DataStore.delete(first.class, null, null);
        DataStore.delete(second.class, null, null);

        new LooperThread() {
            @Override
            public void run() {
                firstC = DataStore.query(first.class, null, null, null, null,
                        true);
                firstC.registerContentObserver(new ContentObserver(
                        new Handler()) {
                    @Override
                    public boolean deliverSelfNotifications() {
                        return true;
                    }

                    @Override
                    public void onChange(boolean selfChange) {
                        firstNotifications++;
                    }
                });

                secondC = DataStore.query(second.class, null, null, null, null,
                        true);
                secondC.registerContentObserver(new ContentObserver(
                        new Handler()) {
                    @Override
                    public boolean deliverSelfNotifications() {
                        return true;
                    }

                    @Override
                    public void onChange(boolean selfChange) {
                        secondNotifications++;
                    }
                });
                latch.countDown();
            }

        }.start();

        new LooperThread() {
            @Override
            public void run() {
                ContentValues cv = new ContentValues();
                for (int i = 0; i < 2; i++) {
                    cv.put("mBookId", System.currentTimeMillis());
                    cv.put("mTitle", "Title :" + first.class.getSimpleName());
                    DataStore.insert(first.class, cv, true);
                    cv.clear();
                }

                for (int i = 0; i < 3; i++) {
                    cv.put("mBookId", System.currentTimeMillis());
                    cv.put("mTitle", "Title :" + second.class.getSimpleName());
                    DataStore.insert(second.class, cv, true);
                    cv.clear();
                }
                latch.countDown();
            }

        }.start();

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        firstC.close();
        secondC.close();

        Cursor test = DataStore
                .query(first.class, null, null, null, null, true);
        int firstCount = test.getCount();
        test.close();

        assertEquals("table is missing data", 2, firstCount);

        assertEquals("Notifications don't match", 2, firstNotifications);
        assertEquals("Notifications don't match", 3, secondNotifications);
    }
}
