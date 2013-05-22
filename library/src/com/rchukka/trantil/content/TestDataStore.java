package com.rchukka.trantil.content;

import android.content.Context;

/** ONLY for instrumentation purpose. Do not use it in real app. */
@SuppressWarnings("rawtypes")
public class TestDataStore {
    
    /** ONLY for instrumentation purpose. Do not use it in real app. */
    public static void init(Context context, int storeDBVersion){
        DataStore.init(context, storeDBVersion);
    }
    
    /** ONLY for instrumentation purpose. Do not use it in real app. */
    public static void dropTable(Class klass){
        DataStore.dropTable(klass);
    }
    
    /** ONLY for instrumentation purpose. Do not use it in real app. */
    public static void dropAllTables(){
        DataStore.dropAllTables();
    }
    
    /** ONLY for instrumentation purpose. Do not use it in real app. */
    public static boolean tableExists(Class klass){
        return DataStore.tableExists(klass);
    }
}
