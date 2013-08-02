package com.rchukka.trantil.test.unit;

import java.io.InputStream;
import java.util.List;

import com.rchukka.trantil.common.XNode;
import com.rchukka.trantil.common.XPath;
import com.rchukka.trantil.common.XmlToCVHandler;
import com.rchukka.trantil.content.CursorColumnMap;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.ColumnInt;
import com.rchukka.trantil.content.type.Table;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.util.Log;
import android.util.Xml;

public class Xml2CVHandlerTest extends InstrumentationTestCase {

    int repeatCount = 10;

    public Xml2CVHandlerTest() {
        super();
    }

    protected void setUp() throws Exception {

    }

    public void ptestBooksXml() throws Exception {
        _testBooksXml("books.xml");
    }

    public void testParentAtt() throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open("books.xml");

        XmlToCVHandler handler = new XmlToCVHandler();

        XmlToCVHandler.Collector booksCol = handler
                .addCollector("/response/book")
                .collect("@bookid=id, @name, @pubDate, comment")
                .collect("comment", "@userid");

        Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        resStream.close();
        List<ContentValues> books = booksCol.getData();

        assertEquals(2, books.size());
        assertEquals(5, books.get(0).size());
        assertEquals("1", books.get(0).getAsString("id"));
        assertEquals("Title One", books.get(0).getAsString("name"));
        assertEquals("10Jan2003", books.get(0).getAsString("pubDate"));
        assertEquals("Is this a good book?.",
                books.get(0).getAsString("comment"));
        assertEquals("3434", books.get(0).getAsString("userid"));

        assertEquals(3, books.get(1).size());
        assertEquals("2", books.get(1).getAsString("id"));
        assertEquals("Title Two", books.get(1).getAsString("name"));
        assertEquals("12Jan2003", books.get(1).getAsString("pubDate"));
    }

    public void testModelConfig() throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open("books.xml");

        @XPath(path = "/response/book")
        class Book {
            @XNode(name = "@bookid") private int     id;
            @XNode(name = "@name") private String    name;
            @XNode(name = "@pubDate") private String published;
        }

        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector booksCol = handler.addCollector(Book.class);
        XmlToCVHandler.Collector statusCol = handler
                .addCollector("/response/status")
                .collect("errorcode=code,errormsg")
                .collect("errorcode", "@critical");
        
        handler.parse(resStream);

        //Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        //resStream.close();
        List<ContentValues> books = booksCol.getData();

        // List<ContentValues> books = XmlToCVHandler.parse(resStream,
        // Book.class);

        assertEquals(2, books.size());
        assertEquals(3, books.get(0).size());
        assertEquals("1", books.get(0).getAsString("id"));
        assertEquals("Title One", books.get(0).getAsString("name"));
        assertEquals("10Jan2003", books.get(0).getAsString("published"));
        assertEquals("2", books.get(1).getAsString("id"));
        assertEquals("Title Two", books.get(1).getAsString("name"));
        assertEquals("12Jan2003", books.get(1).getAsString("published"));
    }

    public void testModelAutoConfig() throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open("books.xml");

        @Table(version = 0)
        @XPath(path="/response/status")
        class Status {
            @Column private String errorcode;
            @Column private String errormsg;
        }

        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector statusCol = handler.addCollector(Status.class);
        handler.parse(resStream);
        List<ContentValues> status = statusCol.getData();

        assertEquals(1, status.size());
        assertEquals("0", status.get(0).getAsString("errorcode"));
        assertEquals("No Error", status.get(0).getAsString("errormsg"));
    }
    
    public void testNameSpace() throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open("atom-small.xml");
        Uri atom = Uri.parse("http://www.w3.org/2005/Atom");
        Uri itunes = Uri.parse("http://www.itunes.com/dtds/podcast-1.0.dtd");

        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector rssCol = handler
                .addCollector("/rss/channel").collect("link")
                .collect(atom, "link", "@href=atomlink")
                .collect(itunes, "image", "@href=image");
        
        XmlToCVHandler.Collector itemCol = handler
                .addCollector("/rss/channel/item").collect("link, title")
                .collect(itunes, "subtitle");

        Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        resStream.close();
        List<ContentValues> rss = rssCol.getData();
        List<ContentValues> items = itemCol.getData();

        assertEquals(true, rss.size() > 0);
        assertEquals(3, rss.get(0).size());
        assertEquals("http://www.startalkradio.net/feed/shows/", rss.get(0).getAsString("atomlink"));
        assertEquals("http://www.startalkradio.net", rss.get(0).getAsString("link"));
        assertEquals("http://www.startalkradio.net/img/feed_image_lg.jpg", rss.get(0).getAsString("image"));
        
        assertEquals(true, items.size() > 6);
        assertEquals(3, items.get(0).size());
        assertEquals("http://www.podtrac.com/pts/redirect.mp3/media.startalkradio.net/uploads/shows/STR-S04E18-2013-07-07-startalk-live-storms-of-our-century-part-2.mp3", items.get(1).getAsString("link"));
        assertEquals("StarTalk Live! Storms of Our Century (Part 2)", items.get(1).getAsString("title"));
        assertEquals("The climate heats up in the Bell House when Neil deGrasse Tyson and the StarTalk Live crew, including Questlove and Dr. Adam Sobel, look at the causes, politics and science of climate change.", items.get(1).getAsString("subtitle"));

    }
    
    public void _testBooksXml(String fileName) throws Exception {
        _testBooksXml(fileName, null);
    }
    
    public void _testBooksXml(String fileName, Context context) throws Exception {
        
        Context app = context == null ? getInstrumentation().getContext() : context;

        AssetManager am = app.getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open(fileName);

        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector statusCol = handler
                .addCollector("/response/status")
                .collect("errorcode=code,errormsg")
                .collect("errorcode", "@critical");
        XmlToCVHandler.Collector booksCol = handler.addCollector(
                "/response/book").collect("@bookid=id,@name,@pubDate");

        Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        resStream.close();
        List<ContentValues> status = statusCol.getData();
        List<ContentValues> books = booksCol.getData();

        if (fileName.contains("big")) assertEquals(true, books.size() > 200);
        else {
            assertEquals(1, status.size());
            assertEquals(3, status.get(0).size());
            assertEquals("0", status.get(0).getAsString("code"));
            assertEquals("No Error", status.get(0).getAsString("errormsg"));
            assertEquals("false", status.get(0).getAsString("critical"));
            assertEquals(2, books.size());
            assertEquals(3, books.get(0).size());
            assertEquals("1", books.get(0).getAsString("id"));
            assertEquals("Title One", books.get(0).getAsString("name"));
            assertEquals("10Jan2003", books.get(0).getAsString("pubDate"));
            assertEquals("2", books.get(1).getAsString("id"));
            assertEquals("Title Two", books.get(1).getAsString("name"));
            assertEquals("12Jan2003", books.get(1).getAsString("pubDate"));
        }
    }

    public void testFolderXml() throws Exception {
        _testFolderXml("folder.xml");
    }
    
    public void _testFolderXml(String fileName) throws Exception {
        _testFolderXml(fileName, null);
    }

    public void _testFolderXml(String fileName, Context context) throws Exception {

        Context app = context == null ? getInstrumentation().getContext() : context;

        AssetManager am = app.getResources()
                .getAssets();
        InputStream resStream = null;

        resStream = am.open(fileName);
        XmlToCVHandler handler = new XmlToCVHandler();

        XmlToCVHandler.Collector cFolders = handler.addCollector(
                "/response/folder").collect("@name");

        XmlToCVHandler.Collector cFiles = handler.addCollector(
                "/response/folder/file").collect(
                ".name=foldername, @name, @owner");

        Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        resStream.close();
        List<ContentValues> folders = cFolders.getData();
        List<ContentValues> files = cFiles.getData();

        if (fileName.contains("big")) {
            assertEquals(true, files.size() > 70);
        } else {
            assertEquals(2, folders.size());
            assertEquals(7, files.size());
            assertEquals(1, folders.get(0).size());
            assertEquals(1, folders.get(1).size());

            assertEquals("firstfolder", folders.get(0).getAsString("name"));
            assertEquals("secondfolder", folders.get(1).getAsString("name"));

            assertEquals("file1.txt", files.get(0).getAsString("name"));
            assertEquals("user1", files.get(0).getAsString("owner"));
            assertEquals("firstfolder", files.get(0).getAsString("foldername"));

            assertEquals("file3.txt", files.get(2).getAsString("name"));
            assertEquals("user3", files.get(2).getAsString("owner"));
            assertEquals("firstfolder", files.get(2).getAsString("foldername"));

            assertEquals("file4.txt", files.get(3).getAsString("name"));
            assertEquals("user1", files.get(3).getAsString("owner"));
            assertEquals("firstfolder", files.get(3).getAsString("foldername"));

            assertEquals("file5.txt", files.get(4).getAsString("name"));
            assertEquals("user1", files.get(4).getAsString("owner"));
            assertEquals("secondfolder", files.get(4).getAsString("foldername"));

            assertEquals("file6.txt", files.get(5).getAsString("name"));
            assertEquals("user3", files.get(5).getAsString("owner"));
            assertEquals("secondfolder", files.get(5).getAsString("foldername"));

            assertEquals("file7.txt", files.get(6).getAsString("name"));
            assertEquals("user7", files.get(6).getAsString("owner"));
            assertEquals("secondfolder", files.get(6).getAsString("foldername"));
        }
    }

    @SuppressWarnings("unused")
    public void testFailures() throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;

        resStream = am.open("folder.xml");
        XmlToCVHandler handler = new XmlToCVHandler();

        XmlToCVHandler.Collector cFolders = handler.addCollector("/response")
                .collect("folder", "@desc");

        try {
            XmlToCVHandler failhandler = new XmlToCVHandler();
            XmlToCVHandler.Collector cFiles = failhandler.addCollector(
                    "/response/folder").collect("file", ".name,@name,@owner");
            fail("Duplicate attribute collection was not detected.");
        } catch (IllegalArgumentException expected) {
        }

        try {
            XmlToCVHandler.Collector cFiles = handler.addCollector(
                    "/response/folder").collect("file",
                    ".name=foldername,@name,@owner");
            Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
            fail("Failed to notify that parent attribute is not being collected.");
        } catch (IllegalArgumentException expected) {
        }

        resStream.close();
    }

    public void testPerformanceBooks() throws Exception {

        // run few times, to ignore any JVM optimizations/delays
        _testBooksXml("books-big.xml");
        _testBooksXml("books-big.xml");

        long time = System.currentTimeMillis();
        for (int i = 0; i < repeatCount; i++) {
            _testBooksXml("books-big.xml");
        }
        time = System.currentTimeMillis() - time;
        long avg = time / repeatCount;

        if (avg > 250) fail("Processing is very slow." + avg);

        if (avg > 200 && avg < 250) fail("Processing is slow." + avg);

        if (avg > 100 && avg < 200) fail("Processing is not fast." + avg);

        Log.i("Xml2CVHandlerTest",
                "testPerformanceBooks() completed with avg time of " + avg
                        + "ms.");
    }

    public void testPerformanceFolder() throws Exception {

        // run few times, to ignore any JVM optimizations/delays
        _testFolderXml("folder-big.xml");
        _testFolderXml("folder-big.xml");

        long time = System.currentTimeMillis();
        for (int i = 0; i < repeatCount; i++) {
            _testFolderXml("folder-big.xml");
        }
        time = System.currentTimeMillis() - time;
        long avg = time / repeatCount;

        if (avg > 250) fail("Processing is very slow." + avg);

        if (avg > 200 && avg < 250) fail("Processing is slow." + avg);

        if (avg > 100 && avg < 200) fail("Processing is not fast." + avg);

        Log.i("Xml2CVHandlerTest",
                "testPerformanceFolder() completed with avg time of " + avg
                        + "ms.");
    }
}
