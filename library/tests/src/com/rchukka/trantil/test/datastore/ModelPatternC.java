package com.rchukka.trantil.test.datastore;

import android.database.Cursor;

import com.rchukka.trantil.content.CursorColumnMap;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.ColumnInt;
import com.rchukka.trantil.content.type.Table;

@Table(version = 1)
public class ModelPatternC {

    @ColumnInt(isKey = true) public static final String BOOK_ID   = "BOOK_ID";
    @ColumnInt public static final String               AUTHOR_ID = "AUTHOR_ID";
    @Column public static final String                  TITLE     = "TITLE";
    @Column public static final String                  DESC      = "DESC";
    @ColumnInt public static final String               PUB_DATE  = "PUB_DATE";

    private long                                        mBookId;
    private long                                        mAuthorId;
    private String                                      mTitle;
    private String                                      mDescription;
    private long                                        mDatePublished;

    public ModelPatternC(CursorColumnMap k, Cursor c) {

        mBookId = k.getLong(c, BOOK_ID, 0l);
        mAuthorId = k.getLong(c, AUTHOR_ID, -1);
        mTitle = k.getString(c, TITLE, null);
        mDescription = k.getString(c, DESC, null);
        mDatePublished = k.getLong(c, PUB_DATE, 0);
    }
    
    public long getBookId(){
        return mBookId;
    }
    
    public String getTitle(){
        return mTitle;
    }
}