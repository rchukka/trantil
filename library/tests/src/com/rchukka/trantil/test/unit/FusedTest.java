package com.rchukka.trantil.test.unit;

import java.io.InputStream;
import java.util.List;

import com.rchukka.trantil.common.XNode;
import com.rchukka.trantil.common.XPath;
import com.rchukka.trantil.common.XmlToCVHandler;
import com.rchukka.trantil.content.CursorColumnMap;
import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.Table;

import android.content.ContentValues;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.util.Log;

public class FusedTest extends InstrumentationTestCase {

    private static final String ATOM_DTD = "http://www.w3.org/2005/Atom";
    private static final String ITUNES_DTD = "http://www.itunes.com/dtds/podcast-1.0.dtd";
    private static final String WFW = "http://wellformedweb.org/CommentAPI/";
    
    public void testAtomFeedSpeed() throws Exception {
        testAtomFeed(false);
        
        int count = 3;
        long time = System.currentTimeMillis();
        for(int i=0;i < count;i++){
            testAtomFeed(false);
        }
        time = (System.currentTimeMillis() - time)/count; 
        Log.d("XML", "testAtomFeed avg time: " + time + "ms");
    }
    
    public void testAtomFeed() throws Exception{
        testAtomFeed(true);
    }
    
    public void testAtomFeed(boolean validate) throws Exception {

        AssetManager am = getInstrumentation().getContext().getResources()
                .getAssets();
        InputStream resStream = null;
        resStream = am.open("atom.xml");

        @Table(version = 0)
        @XPath(path= "/rss/channel")
        final class Channel {
            
            @Column(isKey = true)
            @XNode(name="link@href", ns=ATOM_DTD)
            private String atomLink;
            
            @Column private String                  title;
            @Column private String                  link;
            @Column private String                  description;
            @Column private long                    lastBuildDate;
            @Column private String                  language;
            @Column private long                    updateFreqMins;
            
            @Column
            @XNode(name="image@href", ns=ITUNES_DTD)
            private String imgUrl;

            public Channel(CursorColumnMap k, Cursor c) {

                atomLink = k.getString(c, "atomLink", null);
                title = k.getString(c, "title", "");
                link = k.getString(c, "link", null);
                description = k.getString(c, "desc", null);
                lastBuildDate = k.getLong(c, "lastBuildDate", 0);
                updateFreqMins = k.getLong(c, "updateFreqMins", 60);
                imgUrl = k.getString(c, "imgUrl", null);
            }
        }

        @Table(version = 0)
        @XPath(path = "/rss/channel/item")
        final class ChannelItem {

            @Column private String               title;
            @Column(isKey = true) private long   channelId;
            @Column(isKey = true) private long   pubDate;
            
            @Column 
            @XNode(name="enclosure@url")
            private String               link;
            
            @Column 
            @XNode(name="enclosure@length")
            private long               size;
            
            @Column 
            @XNode(name="enclosure@type")
            private String               linkType;
            
            @Column 
            @XNode(ns=WFW)
            private String               commentsRss;
            
            @Column 
            @XNode(ns=ITUNES_DTD)
            private String               summary;
            
            private String               description;
            
            @Column 
            @XNode(ns=ITUNES_DTD)
            private String               duration;

            public ChannelItem(CursorColumnMap k, Cursor c) {

                channelId = k.getLong(c, "channelAtomLink", 0);
                pubDate = k.getLong(c, "pubDate", 0);
                link = k.getString(c, "link", null);
                size = k.getLong(c, "size", 0);
                linkType = k.getString(c, "linkType", null);
                title = k.getString(c, "title", null);
                commentsRss = k.getString(c, "commentsRss", null);
                summary = k.getString(c, "summary", null);
                description = k.getString(c, "description", null);
                duration = k.getString(c, "duration", null);
            }
        }

        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector channelCol = handler
                .addCollector(Channel.class);
        XmlToCVHandler.Collector itemCol = handler
                .addCollector(ChannelItem.class);
        handler.parse(resStream);
        
        List<ContentValues> channel = channelCol.getData();
        List<ContentValues> items = itemCol.getData();

        if(!validate) return;
        
        assertEquals(1, channel.size());
        assertEquals("StarTalk Radio Show by Neil deGrasse Tyson Shows",
                channel.get(0).getAsString("title"));
        assertEquals("http://www.startalkradio.net/feed/shows/", channel.get(0).getAsString("atomLink"));
        
        assertEquals(true, items.size() > 8);
        assertEquals("Cosmic Queries: Planet Earth",
                items.get(0).getAsString("title"));
        assertEquals("43:47", items.get(0).getAsString("duration"));
        assertEquals("audio/mp3", items.get(0).getAsString("linkType"));
        assertEquals("18333939", items.get(0).getAsString("size"));
        assertEquals("http://www.podtrac.com/pts/redirect.mp3/media.startalkradio.net/uploads/shows/STR-S04E19-2013-07-14-cosmic-queries-planet-earth.mp3", items.get(0).getAsString("link"));
        
        assertEquals("Cosmic Queries: Grab Bag",
                items.get(3).getAsString("title"));
        assertEquals("43:16", items.get(3).getAsString("duration"));
        assertEquals("audio/mp3", items.get(3).getAsString("linkType"));
        assertEquals("18121520", items.get(3).getAsString("size"));
        assertEquals("http://www.podtrac.com/pts/redirect.mp3/media.startalkradio.net/uploads/shows/STR-S04E16-2013-06-23-cosmic-queries-grab-bag.mp3", items.get(3).getAsString("link"));
    }
}
