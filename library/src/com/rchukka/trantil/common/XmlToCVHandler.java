package com.rchukka.trantil.common;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.rchukka.trantil.content.type.Column;
import com.rchukka.trantil.content.type.DataType;
import com.rchukka.trantil.content.type.Table;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides super easy API to convert Xml to ContentValue list.
 * 
 * <p>
 * 
 * <pre>
 * <b><u>Sample 1:</u></b>
 * 
 *     <?xml version="1.0" encoding="utf-8"?>
 *     <response>
 *         <status>
 *             <errorcode critical="false">0</errorcode>
 *             <errormsg>No Error</errormsg>
 *         </status>
 *         <book bookid="1" name="Title One" author="John Doe" pubDate="10Jan2003" desc="Title One Description"/>
 *         <book bookid="2" name="Title Two" author="Johny Doe" pubDate="12Jan2003" desc="Title Two Description"/>  
 *     </response>
 * 
 * XmlHandler handler = new XmlHandler();
 * 
 * XmlHandler.Collector cStatus = handler.addCollector("/response/status")
 *          .collect("errorcode:code,errormsg").collectAttributes("errorcode", "critical");
 *          
 * XmlHandler.Collector cBooks = handler.addCollector("/response")
 *                     .collectAttributes("book", "bookid:id,name,desc");
 *                     
 * Xml.parse(resStream, Xml.Encoding.UTF_8, handler);  
 * List<ContentValues> status = cStatus.getData();
 * List<ContentValues> books = cBooks.getData();
 * 
 * </pre>
 * 
 * </p>
 * 
 * @author raj chukkapalli 04/12
 */
public class XmlToCVHandler extends DefaultHandler {
    private static final String COLLECT_AS_SEPR = "=";
    
    private StringBuilder   mBuff        = new StringBuilder(100);
    private List<Collector> mCollectors  = new ArrayList<Collector>(2);
    private String          mCurrentPath = "";
    private Node            mCurrentNode = new Node();

    public static List<ContentValues> parse(InputStream stream, Class klass) throws IOException, SAXException{
        XmlToCVHandler handler = new XmlToCVHandler();
        XmlToCVHandler.Collector col = handler.addCollector(klass);
        Xml.parse(stream, Xml.Encoding.UTF_8, handler);
        stream.close();
        return col.getData();
    }
    
    public void parse(InputStream stream) throws IOException, SAXException{
        Xml.parse(stream, Xml.Encoding.UTF_8, this);
        stream.close();
    }
    
    public Collector addCollector(String path) {
        Collector col = new Collector(path);
        mCollectors.add(col);
        return col;
    }

    @SuppressWarnings("unchecked")
    public Collector addCollector(Class klass) {
        String xPath = null;
        XPath xpa = (XPath) klass.getAnnotation(XPath.class);
        if (xpa != null) xPath = xpa.path();

/*        if (xPath == null) {
            Table txpa = (Table) klass.getAnnotation(Table.class);
            if (txpa != null) xPath = txpa.xPath();
        }*/

        if (xPath == null || xPath.length() == 0)
            throw new DataType.DataTypeException("Invalid class. "
                    + klass.getName()
                    + " is missing xpath as part of annotation.");

        Collector col = new Collector(xPath);

        for (Field field : klass.getDeclaredFields()) {
            String xNode = null;
            String xNS = "";
            String xNodeParent = "";
            XNode xfa = field.getAnnotation(XNode.class);
            if (xfa != null){
                xNode = xfa.name();
                xNS = xfa.ns();
            }

            /*if (xNode == null) {
                Column cfa = field.getAnnotation(Column.class);
                if (cfa != null){
                    xNS = cfa.xNS();
                    xNode = cfa.xNode();
                }
            }*/
            
            if (xNode == null || xNode.length() == 0)
                xNode = field.getName();
            
            if(xNode.contains("@")){
                String[] split = xNode.split("@");
                xNode = "@"+split[1];
                xNodeParent = split[0];
            }
            
            col.collect(Uri.parse(xNS), xNodeParent, xNode + "=" + field.getName());
        }

        mCollectors.add(col);
        return col;
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        mBuff.setLength(0);
        
        if(Util.DEBUG)
            Log.d("XML", "nameSpaceURI:" + namespaceURI + ", localName: " + localName + ", qName: " + qName);

        if (mCurrentNode != null) {
            Node newCurrent = new Node();
            newCurrent.mParent = mCurrentNode;
            mCurrentNode = newCurrent;
        }

        mCurrentNode.mPath = mCurrentPath + "/" + localName;
        for (Collector col : mCollectors)
            col.processElement(true, mCurrentNode.mPath, namespaceURI, mCurrentPath,
                    localName, null, atts);
        mCurrentPath = mCurrentNode.mPath;
    }

    @Override
    public void characters(char ch[], int start, int length) {
        mBuff.append(ch, start, length);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        
        if(Util.DEBUG)
            Log.d("XML", "nameSpaceURI:" + namespaceURI + ", localName: " + localName + ", qName: " + qName);
        
        mCurrentPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf("/"));
        for (Collector col : mCollectors)
            col.processElement(false, mCurrentNode.mPath, namespaceURI, mCurrentPath,
                    localName, mBuff.toString(), null);
        mCurrentNode = mCurrentNode.mParent;
    }

    public class Collector {
        private String                        mCollectorPath;
        ContentValues                         mCurrentCV;
        List<ContentValues>                   mContents;
        private HashMap<String, Index>        mNSIndex;
        
        final class Index{
            private HashMap<String, String>       mParseEl;
            private HashMap<String, List<Attrib>> mParseAttr;
            String namespace;
            
            Index(String namespace){
                mParseEl = new HashMap<String, String>(3);
                mParseAttr = new HashMap<String, List<Attrib>>(5);
                this.namespace  = namespace;
            }
        }

        Collector(String nodePath) {
            mCollectorPath = nodePath.endsWith("/") ? nodePath.substring(0,
                    nodePath.length() - 1) : nodePath;
            mContents = new ArrayList<ContentValues>(10);
            mNSIndex = new HashMap<String, Index>(3);
        }
        
        private Index getNSIndex(Uri namespace){
            String sNameSpace = namespace != null ?namespace.toString() : "";
            return getNSIndex(sNameSpace);
        }
        
        private Index getNSIndex(String namespace){
            Index index = mNSIndex.get(namespace);
            if(index == null){
                index = new Index(namespace);
                mNSIndex.put(namespace, index);
            }
            return index;
        }

        public List<ContentValues> getData() {
            return mContents;
        }

        /**
         * Collect atttributes from @param nodeName node. The value after :
         * denotes the key to be used in contentvalue to save it's value. neat
         * ah?. ya.
         * 
         * <pre>
         * addCollector("/path").
         *  .collect("@attr2:prdId,@attr3:price,@desc:desc");
         * </pre>
         * 
         * @param nodeattributes
         * @return
         */
        public Collector collect(String nodeattributes) {
            return collect("", nodeattributes);
        }
        
        /**
         * Collect atttributes from @param nodeName node. The value after :
         * denotes the key to be used in contentvalue to save it's value. neat
         * ah?. ya.
         * 
         * <pre>
         * addCollector("/path").
         *  .collect("nodename", "@attr2:prdId,@attr3:price,@desc:desc");
         * </pre>
         * 
         * @param nodeName
         * @param nodeattributes
         * @return
         */
        public Collector collect(String nodeName, String nodeattributes) {
            return collect(null, nodeName, nodeattributes);
        }
        
        /**
         * Collect atttributes from @param nodeName node. The value after :
         * denotes the key to be used in contentvalue to save it's value. neat
         * ah?. ya.
         * 
         * <pre>
         * addCollector("/path").
         *  .collect("nodename", "@attr2:prdId,@attr3:price,@desc:desc");
         * </pre>
         * 
         * @param namespaceurl
         * @param nodeattributes
         * @return
         */
        public Collector collect(Uri namespaceurl, String nodeattributes) {
            return collect(namespaceurl, "", nodeattributes);
        }

        /**
         * Collect atttributes from @param nodeName node. The value after :
         * denotes the key to be used in contentvalue to save it's value. neat
         * ah?. ya.
         * 
         * <pre>
         * addCollector("/path").
         *  .collect("nodename", "@attr2:prdId,@attr3:price,@desc:desc");
         * </pre>
         * 
         * @param namespaceurl
         * @param nodeName
         * @param nodeattributes
         * @return
         */
        public Collector collect(Uri namespaceurl, String nodeName, String nodeattributes) {

            String[] splitCol = nodeattributes.split(",");
            nodeName = adjustNodePath(nodeName);

            for (String attProp : splitCol) {
                attProp = attProp.trim();
                String[] col = attProp.split(COLLECT_AS_SEPR);
                
                if (!col[0].startsWith("@") && !col[0].startsWith(".")){
                    getNSIndex(namespaceurl).mParseEl.put(adjustNodePath(col[0]),
                            col.length > 1 ? col[1] : col[0]);
                }else{
                    collectAttribute(namespaceurl, nodeName, attProp);
                }
            }
            return this;
        }

        private String adjustNodePath(String nodeName) {
            return (nodeName.length() > 0 && !nodeName.startsWith("/") && !nodeName
                    .startsWith(".")) ? "/" + nodeName : nodeName;
        }

        private Collector collectAttribute(Uri namespaceurl, String nodeName, String attProp) {

            attProp = attProp.startsWith("@") ? attProp.substring(1) : attProp;
            
            if(attProp.contains(":"))
                throw new IllegalArgumentException(String.format(
                        "Error collecting attribute %s. namespace not allowed for attributes", attProp));
                
            String[] split = attProp.split(COLLECT_AS_SEPR);

            List<Attrib> attrList = getNSIndex(namespaceurl).mParseAttr.get(nodeName);
            if (attrList == null) {
                attrList = new ArrayList<Attrib>(6);
                getNSIndex(namespaceurl).mParseAttr.put(nodeName, attrList);
            }
            
            Attrib newAttr = new Attrib(split[0], split.length > 1 ? split[1]
                    : split[0]);

            for (Attrib attr : attrList) {
                if (attr.mCollectAs.equalsIgnoreCase(newAttr.mCollectAs))
                    throw new IllegalArgumentException(String.format(
                            "Attribute '%s' is being collected multiple"
                                    + " times with same key", attr.mCollectAs));
            }

            attrList.add(newAttr);
            return this;
        }

        private void processElement(boolean start, String fullPath, String nameSpace, 
                String elePath, String eleName, String value, Attributes atts) {

            if(Util.DEBUG)
                Log.d("XML",
                    String.format(
                            "Start: %s, fullPath: %s, elePath: %s, eleName: %s, value: %s, AttrsLength: %s",
                            start, fullPath, elePath, eleName, value,
                            atts != null ? atts.getLength() : "null"));

            if (!fullPath.startsWith(mCollectorPath)) return;

            // new element collector or new path with attr collector is
            // starting...
            if (start && mCollectorPath.equals(fullPath)) {
                mCurrentCV = new ContentValues(6);
                mContents.add(mCurrentCV);
            }

            mCurrentNode.mCVs.add(mCurrentCV);

            String childDiffPath = fullPath.substring(mCollectorPath.length());
            
            Index nsIndex = getNSIndex(nameSpace);
            
            // parse element
            String collectEleAs = nsIndex.mParseEl.get(childDiffPath);
            if (value != null && collectEleAs != null) {
                
                if(Util.DEBUG)
                    Log.d("XML",
                        String.format(
                                "Collecting %s as %s",
                                value, collectEleAs));
                
                mCurrentCV.put(collectEleAs, value);
            }

            // parse attribute
            List<Attrib> attrList = nsIndex.mParseAttr.get(childDiffPath);
            if (atts == null || attrList == null) return;

            for (Attrib attr : attrList) {
                String attrVal = null;

                if (attr.mKey.startsWith(".")) attrVal = mCurrentNode
                        .getAncestorValue(attr.mKey);
                else attrVal = atts.getValue(attr.mKey);
                
                if(Util.DEBUG)
                    Log.d("XML",
                        String.format(
                                "Collecting %s as %s",
                                attrVal, attr.mCollectAs));

                mCurrentCV.put(attr.mCollectAs, attrVal);
            }
        }
    }

    private class Node {
        String              mPath;
        List<ContentValues> mCVs = new ArrayList<ContentValues>(3);
        Node                mParent;

        String getAncestorValue(String pathKey) {
            String value = null;
            Node targetNode = this;
            for (char depth : pathKey.toCharArray()) {
                if (depth != '.' || targetNode == null) break;
                if (depth == '.') targetNode = targetNode.mParent;
            }

            String key = pathKey.replace(".", "");

            if (targetNode != null && targetNode.mCVs.size() > 0) {
                for (ContentValues cv : mCVs) {
                    value = cv.getAsString(key);
                    if (value != null) break;
                }
            }

            if (value == null)
                throw new IllegalArgumentException(
                        String.format(
                                "Unable to find value for selector '%s' for path '%s',"
                                        + " ensure path is valid and you are collecting"
                                        + " the required value through another collector.",
                                pathKey, mPath));

            return value;
        }
    }

    private class Attrib {
        String mKey;
        String mCollectAs;

        Attrib(String key, String collectAs) {
            mKey = key;
            mCollectAs = collectAs.replace(".", "");
        }
    }
}