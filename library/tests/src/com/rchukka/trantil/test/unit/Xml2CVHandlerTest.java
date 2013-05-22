package com.rchukka.trantil.test.unit;

import java.io.InputStream;
import java.util.List;

import com.rchukka.trantil.common.XmlToCVHandler;

import android.content.ContentValues;
import android.content.res.AssetManager;
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

    public void testBooksXml() throws Exception {
        _testBooksXml("books.xml");
    }

    private void _testBooksXml(String fileName) throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open(fileName);
        
        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector statusCol = handler
                .addCollector("/response/status")
                .collect("errorcode:code,errormsg")
                .collectAttributes("errorcode", "critical");
        XmlToCVHandler.Collector booksCol = handler.addCollector("/response")
                .collectAttributes("book", "bookid:id,name,pubDate");

        Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        resStream.close();
        List<ContentValues> status = statusCol.getData();
        List<ContentValues> books = booksCol.getData();

        if (fileName.contains("big")) 
            assertEquals(true, books.size() > 200);
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

    private void _testFolderXml(String fileName) throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;

        resStream = am.open(fileName);
        XmlToCVHandler handler = new XmlToCVHandler();

        XmlToCVHandler.Collector cFolders = handler.addCollector("/response")
                .collectAttributes("folder", "name");

        XmlToCVHandler.Collector cFiles = handler.addCollector(
                "/response/folder").collectAttributes("file",
                ".name:foldername,name,owner");

        Xml.parse(resStream, Xml.Encoding.UTF_8, handler);
        resStream.close();
        List<ContentValues> folders = cFolders.getData();
        List<ContentValues> files = cFiles.getData();

        if (fileName.contains("big")) {
            assertEquals(true, files.size() > 70);
        }
        else{
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
                .collectAttributes("folder", "desc");

        try {
            XmlToCVHandler failhandler = new XmlToCVHandler();
            XmlToCVHandler.Collector cFiles = failhandler.addCollector(
                    "/response/folder").collectAttributes("file",
                    ".name,name,owner");
            fail("Duplicate attribute collection was not detected.");
        } catch (IllegalArgumentException expected) {
        }

        try {
            XmlToCVHandler.Collector cFiles = handler.addCollector(
                    "/response/folder").collectAttributes("file",
                    ".name:foldername,name,owner");
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

        if (avg > 150 && avg < 200) fail("Processing is not fast." + avg);

        Log.i("Xml2CVHandlerTest",
                "testPerformanceBooks() completed with avg time of " + avg + "ms.");
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

        if (avg > 150 && avg < 200) fail("Processing is not fast." + avg);

        Log.i("Xml2CVHandlerTest",
                "testPerformanceFolder() completed with avg time of " + avg + "ms.");
    }
}
