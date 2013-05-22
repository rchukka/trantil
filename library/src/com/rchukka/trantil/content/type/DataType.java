package com.rchukka.trantil.content.type;

public class DataType {

    public static final int INT    = 100;
    public static final int LONG   = 100;
    public static final int TEXT   = 101;
    public static final int FLOAT  = 102;
    public static final int DOUBLE = 102;
    public static final int REAL   = 102;

    public static class DataTypeException extends IllegalArgumentException {
        private static final long serialVersionUID = 2165922874513598226L;

        public DataTypeException(String msg) {
            super(msg);
        }
        
        public DataTypeException(String msg, Exception ex) {
            super(msg, ex);
        }
    }
}
