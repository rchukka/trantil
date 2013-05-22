package com.rchukka.trantil.test.datastore;

import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.ColumnInt;
import com.rchukka.trantil.content.type.Table;

@Table(version = 0)
public class ModelPatternA {
    
    @ColumnInt(isKey = true) public static final String BOOK_ID   = "BOOK_ID";
    @ColumnInt public static final String            AUTHOR_ID = "AUTHOR_ID";
    @Column public static final String               TITLE     = "TITLE";
    @Column public static final String               DESC      = "DESC";
    @ColumnInt public static final String            PUB_DATE  = "PUB_DATE";
}