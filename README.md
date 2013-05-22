#Trantil

Trantil (Transformational Utility) provides meta object-relational mapping(ORM) API for android platform. Trantil comes with 2 main classes, DataStore and XmlToCVHandler, these can be used independent of one another.


## DataStore
This class provides API for meta ORM. Once the annotation are added to a class, they can be referenced in DataStore API which will create the required tables. The CRUD operations are done by using the object's class. Select API will return a android cursor, which can be optionally typed into a object by application code.

You will love how much cruft and boiler plater code trantil can remove from your application!.

#### Goals and functionality

- Minimal configuration.
- Fast.
- CRUD API similar to android content provider.
- Compatibility classes for android loader manager.
- Design and implementation focused on Principle of least astonishment.
- Small jar size, ~20 KB.

#### Usage

**1. Initialize**

Add the below line to onCreate of your application class.

	DataStore.init(this);

**2. Annotate your class**

Annotate the fields that need to be persisted and define a constructor as shown below. The constructor helps avoid what is usually, complex and expensive part of a typical ORM. 

Below exmaple is one of the patterns, the sample [test](https://github.com/rchukka/trantil/tree/master/library/tests "test project") project shows multiple patterns ([1](https://github.com/rchukka/trantil/blob/master/library/tests/src/com/rchukka/trantil/test/datastore/DataStoreActivityA.java), [2](https://github.com/rchukka/trantil/blob/master/library/tests/src/com/rchukka/trantil/test/datastore/DataStoreActivityB.java), [3](https://github.com/rchukka/trantil/blob/master/library/tests/src/com/rchukka/trantil/test/datastore/DataStoreActivityC.java)) that can be used based on how your data object is used in your application.

Trantil uses annotations instead of automatically persisting all fields. This allows you to control and differentiate between persistent and non-persistent fields.

    @Table(version = 0)
	public class Book {
	
	    @Column(isKey = true) private long mBookId;
	    @Column private String             mTitle;
		
	    public Book(CursorColumnMap k, Cursor c) {
	        mBookId = k.getLong(c, "mBookId", 0l);
	        mTitle = k.getString(c, "mTitle", null);
	    }
	}

**3. Manage Data**

Trantil uses ContentValues instead of reflection used in typical ORM's, this is to avoid reflection overhead.

    ContentValues cv = new ContentValues();
    cv.put(Book.BOOK_ID, "1");
    cv.put(Book.TITLE, "Foo Bar");

    DataStore.insert(Book.class, cv);
    
	Cursor c = DataStore.query(Book.class, null, null, null, null);
	while (cursor.moveToNext()) {
		// converting to object is optional, you can also directly access the data,
    	// check tests project for examples.
		Book o = new Book(cursorMap, cursor);
	}	

    DataStore.delete(Book.class, null, null);


## XmlToCVHandler

This provides *very* cool API for converting XML into DB data.
#### Goals and functionality
- Super minimalistic API to convert XML into DB data.
- Handle data normalization and denormalization.
- Fast.

#### Usage
    XmlToCVHandler handler = new XmlToCVHandler();
    XmlToCVHandler.Collector dataCol = handler
            .addCollector("/node/path")
            .collect("nodename1, nodename2:collect_node_as")
            .collectAttributes("nodename", "attr1, attr2, attr3:collect_attr_as");

    Xml.parse(xmlStream, Xml.Encoding.UTF_8, handler);
    List<ContentValues> cvListData = dataCol.getData(); // You are done !!!.

*XmlToCVHandler can handle other complex XML parsing scenarious, check [examples](https://github.com/rchukka/trantil/blob/master/library/tests/src/com/rchukka/trantil/test/unit/Xml2CVHandlerTest.java) for more info*

You can use XmlToCVHandler and DataStore API together to easily convert XML to data in tables and update UI, just call "insert" method in DataStore API.

    DataStore.insert(Book.class, cvListData);

##Developed By
Raj Chukkapalli

##License
	Copyright 2013 Raj Chukkapalli
	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	   http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.