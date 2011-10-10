/**
 * Copyright (C) 2011 Matthias Jordan <matthias.jordan@googlemail.com>
 *
 * This file is part of piRSS.
 *
 * piRSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * piRSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with piRSS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.codefu.android.rss.updateservice;

import java.text.ParseException;
import java.util.Stack;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.codefu.android.rss.updateservice.FeedHandlerClient.Item;



/**
 * SAX parser for RSS feeds.
 * 
 * @author mj
 */
class FeedHandler extends DefaultHandler {

    private static final String E_ENTRY = "entry";

    private static final String E_PUB_DATE = "pubDate";

    private static final String E_GUID = "guid";

    private static final String E_ITEM = "item";

    private static final String E_DESCRIPTION = "description";

    private static final String E_LINK = "link";

    private static final String E_TITLE = "title";

    private static final String E_SUBTITLE = "subtitle";

    private static final String E_ID = "id";

    private Stack<String> elements;

    private String channelTitle;
    private String channelLink;
    private String channelDescription;

    private Item item;

    private FeedHandlerClient fhc;

    private String collectAllUntilEndTag;
    private StringBuilder collector;

    private boolean errorOccurred;

    private long feedId;
    private int cleanHtml;

    private TimeZone timezone;


    public FeedHandler(final long feedId, int cleanHtml, FeedHandlerClient fhc, TimeZone tz) {
        this.feedId = feedId;
        this.cleanHtml = cleanHtml;
        this.fhc = fhc;
        this.elements = new Stack<String>();
        this.errorOccurred = false;
        this.timezone = tz;
    }


    public String getChannelLink() {
        return channelLink;
    }


    public String getChannelTitle() {
        return channelTitle;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (isCollectorConsuming(localName)) {
            collector.append('<').append(localName).append('>');
        }
        else if ((elements.size() == 0)) {
            if (E_TITLE.equals(localName) && (channelTitle == null)) {
                elements.push(E_TITLE);
                startCollecting(E_TITLE);
            }
            else if (E_LINK.equals(localName) && (channelLink == null)) {
                final String href = attributes.getValue("href");
                final String rel = attributes.getValue("rel");
                if ((channelLink == null) && (href != null) && (!"self".equals(rel))) {
                    channelLink = href;
                }
                else {
                    elements.push(E_LINK);
                    startCollecting(E_LINK);
                }
            }
            else if ((E_DESCRIPTION.equals(localName) || E_SUBTITLE.equals(localName)) && (channelDescription == null)) {
                elements.push(localName);
                startCollecting(localName);
            }
            else if (E_ITEM.equals(localName) || E_ENTRY.equals(localName)) {
                elements.push(localName);
                item = new Item();
                final String about = attributes.getValue("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
                if (about != null) {
                    item.guid = about;
                }
            }
        }
        else if ((elements.size() == 1) && (E_ITEM.equals(elements.peek()) || E_ENTRY.equals(elements.peek()))) {
            if (E_TITLE.equals(localName)) {
                elements.push(E_TITLE);
                startCollecting(E_TITLE);
            }
            else if (E_LINK.equals(localName)) {
                final String href = attributes.getValue("href");
                final String rel = attributes.getValue("rel");
                if ((item.link == null) && (href != null) && (!"self".equals(rel))) {
                    item.link = href;
                }
                else {
                    elements.push(E_LINK);
                    startCollecting(E_LINK);
                }
            }
            else if (E_DESCRIPTION.equals(localName)) {
                elements.push(E_DESCRIPTION);
                startCollecting(E_DESCRIPTION);
            }
            else if (E_GUID.equals(localName) || E_ID.equals(localName)) {
                elements.push(localName);
                startCollecting(localName);
            }
            else if (E_PUB_DATE.equals(localName) || "published".equals(localName) || "date".equals(localName)) {
                elements.push(localName);
                startCollecting(localName);
            }
        }
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (collector != null) {
            collector.append(new String(ch, start, length));
        }
        else if (elements.size() == 1) {
            if (E_TITLE.equals(elements.peek()) && (channelTitle == null)) {
                channelTitle = new String(ch, start, length).trim();
            }
            else if (E_DESCRIPTION.equals(elements.peek()) && (channelDescription == null)) {
                channelDescription = new String(ch, start, length).trim();
            }
            else if (E_LINK.equals(elements.peek()) && (channelLink == null)) {
                channelLink = new String(ch, start, length).trim();
            }
        }
        else if (elements.size() == 2) {
            if (E_TITLE.equals(elements.peek())) {
                if (item != null) {
                    item.headline = new String(ch, start, length).trim();
                }
            }
            else if (E_DESCRIPTION.equals(elements.peek())) {
                if (item != null) {
                    item.content = new String(ch, start, length).trim();
                }
            }
            else if (E_GUID.equals(elements.peek()) || E_ID.equals(elements.peek())) {
                if (item != null) {
                    item.guid = new String(ch, start, length).trim();
                }
            }
            else if (E_LINK.equals(elements.peek())) {
                if (item != null) {
                    item.link = new String(ch, start, length).trim();
                }
            }
            else if (E_PUB_DATE.equals(elements.peek())) {
                if (item != null) {
                    try {
                        item.date = DateFormat3339.parse(new String(ch, start, length), timezone);
                    }
                    catch (ParseException e) {
                    }
                }
            }
        }
    }


    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (isCollectorConsuming(localName)) {
            collector.append("</").append(localName).append('>');
        }
        else {
            if ((elements.size() != 0) && elements.peek().equals(localName)) {
                elements.pop();
                if ((elements.size() == 0)) {
                    if (E_TITLE.equals(localName)) {
                        channelTitle = endCollecting();
                    }
                    else if (E_LINK.equals(localName)) {
                        channelLink = endCollecting();
                    }
                    else if (E_DESCRIPTION.equals(localName) || E_SUBTITLE.equals(localName)) {
                        channelDescription = endCollecting();
                    }
                    else if (E_ITEM.equals(localName) || E_ENTRY.equals(localName)) {
                        fhc.addItem(feedId, cleanHtml, item);
                        item = null;
                    }
                }
                else if ((elements.size() == 1) && (E_ITEM.equals(elements.peek())) || E_ENTRY.equals(elements.peek())) {
                    if (E_TITLE.equals(localName)) {
                        if (item != null) {
                            item.headline = endCollecting();
                        }
                    }
                    else if (E_LINK.equals(localName)) {
                        if (item != null) {
                            item.link = endCollecting();
                        }
                    }
                    else if (E_DESCRIPTION.equals(localName)) {
                        if (item != null) {
                            item.content = endCollecting();
                        }
                    }
                    else if (E_GUID.equals(localName) || E_ID.equals(localName)) {
                        if (item != null) {
                            item.guid = endCollecting();
                        }
                    }
                    else if (E_PUB_DATE.equals(localName) || "published".equals(localName) || "date".equals(localName)) {
                        if (item != null) {
                            try {
                                item.date = DateFormat3339.parse(endCollecting(), timezone);
                            }
                            catch (ParseException e) {
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public void endDocument() throws SAXException {
        fhc.updateFeed(feedId, channelTitle, channelDescription, channelLink);
    }


    private void startCollecting(String tag) {
        collectAllUntilEndTag = tag;
        collector = new StringBuilder();
    }


    private boolean isCollectorConsuming(String tag) {
        return (collectAllUntilEndTag != null) && (!collectAllUntilEndTag.equals(tag));
    }


    private String endCollecting() {
        if (collectAllUntilEndTag != null) {
            final String out = collector.toString().trim();
            collectAllUntilEndTag = null;
            collector = null;
            return out;
        }
        return null;
    }


    public boolean hasErrorOccurred() {
        return errorOccurred;
    }
}
