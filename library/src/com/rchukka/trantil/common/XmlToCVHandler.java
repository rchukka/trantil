package com.rchukka.trantil.common;

import android.content.ContentValues;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
    private StringBuilder   mBuff        = new StringBuilder(100);
    private List<Collector> mCollectors  = new ArrayList<Collector>(2);
    private String          mCurrentPath = "";
    private Node            mCurrentNode = new Node();

    public Collector addCollector(String path) {
        Collector col = new Collector(path);
        mCollectors.add(col);
        return col;
    }

    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        mBuff.setLength(0);

        if (mCurrentNode != null) {
            Node newCurrent = new Node();
            newCurrent.mParent = mCurrentNode;
            mCurrentNode = newCurrent;
        }

        mCurrentNode.mPath = mCurrentPath + "/" + localName;
        for (Collector col : mCollectors)
            col.processElement(mCurrentPath, localName, null, atts);
        mCurrentPath = mCurrentNode.mPath;
    }

    @Override
    public void characters(char ch[], int start, int length) {
        mBuff.append(ch, start, length);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        mCurrentPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf("/"));
        for (Collector col : mCollectors)
            col.processElement(mCurrentPath, localName, mBuff.toString(), null);
        mCurrentNode = mCurrentNode.mParent;
    }

    public class Collector {
        private String                        mCollectorPath;
        ContentValues                         mCurrentCV;
        List<ContentValues>                   mContents;
        private HashMap<String, String>       mParseEl;
        private HashMap<String, List<Attrib>> mParseAttr;

        Collector(String nodePath) {
            mCollectorPath = nodePath;
            mContents = new ArrayList<ContentValues>(10);
            mParseEl = new HashMap<String, String>(3);
            mParseAttr = new HashMap<String, List<Attrib>>(5);
        }

        public List<ContentValues> getData() {
            return mContents;
        }

        public Collector collect(String elements) {
            String[] splitCol = elements.split(",");
            for (String attProp : splitCol) {
                String[] split = attProp.split(":");
                mParseEl.put(split[0], split.length > 1 ? split[1] : split[0]);
            }

            return this;
        }

        /**
         * Collects node inner text value
         * 
         * @param elementName
         *            Xml element/node name
         * @param collectElAs
         *            Collect element as
         * @return
         */
        public Collector collect(String elementName, String collectElAs) {
            mParseEl.put(elementName, collectElAs);
            return this;
        }

        /**
         * Collect atttributes from @param elementName node. The value after :
         * denotes the key to be used in contentvalue to save it's value. neat
         * ah?. ya.
         * 
         * <pre>
         * addCollector("/path").
         *  .collectAttributes("attr1", "attr2:prdId,attr3:price,desc:desc");
         * </pre>
         * 
         * @param elementName
         * @param collectAttrAs
         * @return
         */
        public Collector collectAttributes(String elementName,
                String attrName_collectAttrAs) {
            // blow away spaces
            attrName_collectAttrAs = attrName_collectAttrAs.replace(" ", "");
            String[] splitCol = attrName_collectAttrAs.split(",");
            return collectAttributes(elementName, splitCol);
        }

        private Collector collectAttributes(String elementName,
                String[] attrName_collectAttrAs) {
            for (String attProp : attrName_collectAttrAs) {
                String[] split = attProp.split(":");

                List<Attrib> attrList = mParseAttr.get(elementName);
                if (attrList == null) {
                    attrList = new ArrayList<Attrib>(6);
                    mParseAttr.put(elementName, attrList);
                }
                Attrib newAttr = new Attrib(split[0],
                        split.length > 1 ? split[1] : split[0]);

                addCollAtrib(attrList, newAttr);
            }
            return this;
        }

        private void addCollAtrib(List<Attrib> attrList, Attrib newAttr) {
            for (Attrib attr : attrList) {
                if (attr.mCollectAs.equalsIgnoreCase(newAttr.mCollectAs))
                    throw new IllegalArgumentException(String.format(
                            "Attribute '%s' is being collected multiple"
                                    + " times with same key", attr.mCollectAs));
            }
            
            attrList.add(newAttr);
        }

        private void processElement(String elePath, String eleName,
                String value, Attributes atts) {

            // new element collector or new path with attr collector is
            // starting...
            if (atts != null
                    && (mCollectorPath.equals(elePath + "/" + eleName) || (mCollectorPath
                            .equals(elePath) && mParseAttr.containsKey(eleName)))) {
                if (mCurrentCV == null || mCurrentCV.size() > 0) {
                    mCurrentCV = new ContentValues(6);
                    mContents.add(mCurrentCV);
                }
            }

            if (!elePath.contains(mCollectorPath)) return;

            mCurrentNode.mCVs.add(mCurrentCV);

            String parentChildDiffPath = elePath.substring(mCollectorPath
                    .length());
            parentChildDiffPath = parentChildDiffPath.length() > 0 ? parentChildDiffPath
                    + "/"
                    : parentChildDiffPath;

            // parse element
            String collectEleAs = mParseEl.get(parentChildDiffPath + eleName);
            if (value != null && collectEleAs != null) {
                mCurrentCV.put(collectEleAs, value);
            }

            // parse attribute
            List<Attrib> attrList = mParseAttr.get(eleName);
            if (atts == null || attrList == null) return;

            for (Attrib attr : attrList) {
                if (attr.mKey.startsWith(".") || false) {
                    mCurrentCV.put(attr.mCollectAs,
                            mCurrentNode.getAncestorValue(attr.mKey));
                } else {
                    mCurrentCV.put(attr.mCollectAs, atts.getValue(attr.mKey));
                }
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