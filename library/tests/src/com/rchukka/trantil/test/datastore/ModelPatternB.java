package com.rchukka.trantil.test.datastore;

import android.database.Cursor;

import com.rchukka.trantil.content.CursorColumnMap;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.Table;

@Table(version = 0)
public class ModelPatternB {

    @Column(isKey = true) private long mBookId;
    @Column private long                 mAuthorId;
    @Column private String               mTitle;
    @Column private String               mDescription;
    @Column private long                 mDatePublished;

    public ModelPatternB(CursorColumnMap k, Cursor c) {

        mBookId = k.getLong(c, "mBookId", 0l);
        mAuthorId = k.getLong(c, "mAuthorId", -1);
        mTitle = k.getString(c, "mTitle", null);
        mDescription = k.getString(c, "mDescription", null);
        mDatePublished = k.getLong(c, "mDatePublished", 0);
    }
}