package com.rchukka.trantil.content;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.ColumnInt;
import com.rchukka.trantil.content.type.ColumnReal;
import com.rchukka.trantil.content.type.DataType;
import com.rchukka.trantil.content.type.Table;

@SuppressWarnings("rawtypes")
class TableManager {

    private static final boolean                    DEBUG = true;
    private static final String                     TAG   = "trantil_table_manager";
    private DatabaseHelper                          mDBHelper;
    private final ConcurrentHashMap<String, String> mClassToTableMap;

    public TableManager(DatabaseHelper DBHelper) {
        mDBHelper = DBHelper;
        mClassToTableMap = new ConcurrentHashMap<String, String>(20, 0.9f, 1);
    }

    public void maybeCreateOrUpdateTables(List<Class> klasses) {
        Class[] ar = new Class[klasses.size()];
        maybeCreateOrUpdateTables(klasses.toArray(ar));
    }

    public void maybeCreateOrUpdateTables(Class[] klasses) {
        for (Class klass : klasses)
            maybeCreateOrUpdateTable(klass);
    }

    @SuppressWarnings("unchecked")
    public String maybeCreateOrUpdateTable(Class klass) {

        // check if this class has been around before
        String tableName = mClassToTableMap.get(klass.getName());
        if (tableName != null) return tableName;

        Table annotation = (Table) klass.getAnnotation(Table.class);
        if (annotation == null)
            throw new DataType.DataTypeException("Invalid class. "
                    + klass.getName() + " is missing 'Table' annotation.");

        tableName = getTableName(annotation, klass);
        checkDuplicateTableNames(tableName);
        int ver = annotation.version();
        int dbVer = getDBDataTypeVersion(tableName);

        if (ver != dbVer) {
            if (DEBUG)
                Log.d(TAG, "Upgrading table '" + tableName + "' from " + dbVer
                        + " to " + ver);
            createTableFromDataType(tableName, ver, klass);
        }

        mClassToTableMap.put(klass.getName(), tableName);
        return tableName;
    }

    private void checkDuplicateTableNames(String tableName) {
        String previousMappedKlass = null;
        Iterator it = mClassToTableMap.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = mClassToTableMap.get(key);
            if (tableName.equals(value)) {
                previousMappedKlass = key;
                break;
            }
        }
        // containesValue is slow, but this
        if (previousMappedKlass != null) { throw new DataType.DataTypeException(
                "Table name '" + tableName
                        + "' was already mapped to another class '"
                        + previousMappedKlass + "'."); }
    }

    private String getTableName(Table annotation, Class type) {

        String name = annotation.name().length() > 0 ? annotation.name() : type
                .getName().replaceAll("[ .$]", "_");

        return name;
    }

    private int getDBDataTypeVersion(String tableName) {
        int schemaVersion = -1;

        Cursor cursor = mDBHelper.getReadableDatabase().rawQuery(
                "SELECT version FROM modelversions WHERE tablename='"
                        + tableName + "'", null);

        if (cursor.moveToFirst()) {
            schemaVersion = cursor.getInt(0);
        }
        cursor.close();

        if (DEBUG)
            Log.d(TAG, "Currrent table version in db for '" + tableName + "': "
                    + schemaVersion);
        return schemaVersion;
    }

    private void createTableFromDataType(String tableName, int version,
            Class klass) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        String primaryKey = "";
        StringBuilder sbt = new StringBuilder(200);

        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.execSQL("DELETE FROM modelversions WHERE tablename='" + tableName
                + "'");

        sbt.append("CREATE TABLE IF NOT EXISTS ").append(tableName)
                .append(" (");

        Field[] fields = getDataFields(klass);

        for (int i = 0; i < fields.length; i++) {

            String keyColName = appendColumn(fields[i], sbt);
            sbt.append(i == fields.length - 1 ? "" : ",");

            if (keyColName != null) primaryKey += keyColName + ",";
        }

        if (primaryKey.endsWith(","))
            primaryKey = primaryKey.substring(0, primaryKey.length() - 1);

        if (primaryKey.length() > 0) sbt.append(", PRIMARY KEY (")
                .append(primaryKey).append(")");
        else sbt.append(", _id INTEGER PRIMARY KEY AUTOINCREMENT");

        sbt.append(")");
        String tableCreateSql = sbt.toString();

        Log.d(TAG, "Dropped existing table(if exist) and created: "
                + tableCreateSql);

        db.execSQL(tableCreateSql);
        db.execSQL(String.format(
                "INSERT INTO modelversions VALUES('%s', '%s', %d)", tableName,
                klass.getName(), version));
    }

    private Field[] getDataFields(Class type) {

        List<Field> persFields = new ArrayList<Field>(10);
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)
                    || field.isAnnotationPresent(ColumnInt.class)
                    || field.isAnnotationPresent(ColumnReal.class)) {

                // this detects invalid type for all 3 Column[|int|Real] types
                String fieldType = field.getType().getSimpleName();
                if (!fieldType.equals("int") && !fieldType.equals("float")
                        && !fieldType.equals("long")
                        && !fieldType.equals("double")
                        && !fieldType.equals("String"))
                    throw new DataType.DataTypeException(
                            "Invalid field type: "
                                    + field.getType()
                                    + " for '"
                                    + field.getName()
                                    + "'. Only int, long, double, float and string types are supported.");
                persFields.add(field);
            }
        }

        return (Field[]) persFields.toArray(new Field[persFields.size()]);
    }

    private String appendColumn(Field field, StringBuilder sb) {
        boolean isKey = false;
        String colName = null;

        try {
            colName = getColumnName(field);
            isKey = isKey(field);
            String colType;
            colType = getColumnSQLType(field);
            sb.append(colName).append(colType);
        } catch (Exception e) {
            throw new DataType.DataTypeException("Error processing field '"
                    + field.getName() + "'. " + e.getMessage(), e);
        }

        return isKey ? colName : null;
    }

    //@formatter:off
    private String getColumnSQLType(Field field) throws Exception {
        String colType = null;
        int fieldMods = field.getModifiers();
        String fieldType = field.getType().getSimpleName();
        boolean hasStaticMod = Modifier.isStatic(fieldMods);
        boolean hasFinalMod = Modifier.isFinal(fieldMods);

        if (hasStaticMod != hasFinalMod)
            throw new DataType.DataTypeException("Invalid field '"
                    + field.getName()
                    + "'. fields should have both 'static' and 'final'"
                    + " modifiers or not have both.");

        if (hasStaticMod && hasFinalMod) {
            if (!fieldType.equals("String"))
                throw new DataType.DataTypeException(
                        "Invalid field '"
                                + field.getName()
                                + "'. static final fields needs to be of 'String' type.");

            field.setAccessible(true);
            String fieldValue = (String) field.get(null);
            if (!fieldValue.equals(field.getName()))
                throw new DataType.DataTypeException("Invalid field '"
                        + field.getName()
                        + "'. static final string fields need to have"
                        + " matching value.");

            Column cField = field.getAnnotation(Column.class);
            if (cField != null) colType = " TEXT";

            if (colType == null) {
                ColumnInt ciField = field.getAnnotation(ColumnInt.class);
                if (ciField != null) colType = " INTEGER";
            }

            if (colType == null) {
                ColumnReal crField = field.getAnnotation(ColumnReal.class);
                if (crField != null) colType = " REAL";
            }
        } else {

            Column cField = field.getAnnotation(Column.class);
            
            if (cField == null){
                throw new DataType.DataTypeException(
                        "Invalid annotation '"
                                + field.getName()
                                + "'. Only 'Column' annotation is available"
                                + " for instance members. Type is determined"
                                + " automatically for instance members."
                                + " Remove 'ColumnInt' or 'ColumnReal'"
                                + " annotations."); 
            }
            
            // if field is instance variable then figure out it's db type.
            if (fieldType.equals("int") || fieldType.equals("long")) colType = " INTEGER";
            else if (fieldType.equals("float")
             || fieldType.equals("double")) colType = " REAL";
            else if (fieldType.equals("String")) colType = " TEXT";

        }
        
        if (colType == null)
            throw new DataType.DataTypeException("Invalid field '"
                    + field.getName() + "'.");
        
        return colType;
    }
    //@formatter:on

    private boolean isKey(Field field) {
        boolean isKey = false;

        Column cField = field.getAnnotation(Column.class);
        if (cField != null) isKey = cField.isKey();

        if (!isKey) {
            ColumnInt ciField = field.getAnnotation(ColumnInt.class);
            if (ciField != null) isKey = ciField.isKey();
        }

        if (!isKey) {
            ColumnReal crField = field.getAnnotation(ColumnReal.class);
            if (crField != null) isKey = crField.isKey();
        }

        return isKey;
    }

    private String getColumnName(Field field) {
        String colName = field.getName();
        return colName;
    }

    // start instrumentation API
    @SuppressWarnings("unchecked")
    void dropTable(Class klass) {
        mClassToTableMap.clear();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        Table annotation = (Table) klass.getAnnotation(Table.class);
        String tableName = getTableName(annotation, klass);
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.execSQL("DELETE FROM modelversions WHERE tablename='" + tableName
                + "'");
    }

    void dropAllTables() {
        mClassToTableMap.clear();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        mDBHelper.dropAllTables(db);
        mDBHelper.onCreate(db);
    }

    @SuppressWarnings("unchecked")
    boolean tableExists(Class klass) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        Table annotation = (Table) klass.getAnnotation(Table.class);
        String tableName = getTableName(annotation, klass);

        boolean schemaExists = getDBDataTypeVersion(tableName) >= 0;
        boolean tableExists = false;

        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null);
        cursor.moveToFirst();
        do {
            if (cursor.getString(0).equals(tableName)) {
                tableExists = true;
                break;
            }
        } while (cursor.moveToNext());
        cursor.close();

        return (tableExists || schemaExists);
    }

    // end instrumentation API
}
