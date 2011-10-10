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

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import junit.framework.Assert;
import android.content.res.AssetManager;
import android.test.InstrumentationTestCase;
import de.codefu.android.rss.updateservice.FeedHandlerClient.Item;



public class InsertServiceTest extends InstrumentationTestCase {

    private String TEST_HTML = "<html><head><title>foo</title></head></html>";
    private String TEST_NON_XML = "foo bar baz";
    private List<Item> itemInserted;


    private class TestableFeedHandlerClient implements FeedHandlerClient {

        public void addItem(long feedId, int cleanHtml, Item item) {
            itemInserted.add(item);
        }


        public void updateFeed(long feedId, String name, String description, String url) {
            feedTitle = name;
            feedDescription = description;
            feedUrl = url;
        }

    }


    private class TestableFeedHandler extends FeedHandler {

        public TestableFeedHandler(final long feedId) {
            super(feedId, 0, new TestableFeedHandlerClient(), TimeZone.getTimeZone("PST"));
        }
    }


    private TestableFeedHandler fh;
    private InsertService service;

    private String feedTitle;
    private String feedDescription;
    private String feedUrl;


    public void setUp() {
        itemInserted = new LinkedList<Item>();
        fh = new TestableFeedHandler(1);
        service = new InsertService();
    }


    public void testProcess1() throws IOException {
        final String xml = readFile("example091.xml", "iso8859-1");
        service.processXml(xml, 1, fh);

        Assert.assertEquals("feed name", "Foo title", feedTitle);
        Assert.assertEquals("feed description", "News", feedDescription);
        Assert.assertEquals("feed url", "http://example.com/", feedUrl);

        Assert.assertEquals("item count", 2, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("1 headline", "Title 1", item.headline.trim());
        Assert.assertEquals("1 content ", "<p>Description with HTML 1.</p>", item.content);
        Assert.assertEquals("1 link", "http://example.com/rss?item=1", item.link);
        Assert.assertEquals("1 guid", null, item.guid);
        Assert.assertEquals("1 date", null, item.date);
        item = itemInserted.get(1);
        Assert.assertEquals("2 headline", "Title 2", item.headline);
        Assert.assertEquals("2 content", "<p>Description 2 line 1\n" + "\t\t\t\t\tdescription 2 line 2\n"
                        + "\t\t\t\t\tdescription 2 line 3.</p>\n" + "\t\t\t\t<p>description 2 line 4\n"
                        + "\t\t\t\t\t</p>", item.content);
        Assert.assertEquals("2 link", "http://example.com/rss?item=2", item.link);
        Assert.assertEquals("2 guid", null, item.guid);
        Assert.assertEquals("2 date", null, item.date);
    }


    public void testProcess2() throws IOException {
        final String xml = readFile("test2.xml", "UTF-8");
        service.processXml(xml, 1, fh);
        Assert.assertEquals("feed name", "RSS Title", feedTitle);
        Assert.assertEquals("feed description", "This is an example of an RSS feed", feedDescription);
        Assert.assertEquals("feed url", "http://www.someexamplerssdomain.com/main.html", feedUrl);

        Assert.assertEquals("item count", 1, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("1 headline", "Example entry", item.headline);
        Assert.assertEquals(
                        "1 content ",
                        "Here is some text containing an interesting description\n\t\t\t\tof the thing to be described.",
                        item.content);
        Assert.assertEquals("1 link", "http://www.wikipedia.org/", item.link);
        Assert.assertEquals("1 guid", "unique string per item", item.guid);
        Assert.assertEquals("1 date ", new Date(1252255500000l), item.date);
    }


    public void testProcess3() throws IOException {
        final String xml = readFile("test3.xml", "UTF-8");
        service.processXml(xml, 1, fh);
        Assert.assertEquals("feed name", "Foo Blog", feedTitle);
        Assert.assertEquals("feed description", "Foo bar", feedDescription);
        Assert.assertEquals("feed url", "http://blog.example.com/", feedUrl);

        Assert.assertEquals("item count", 1, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("1 headline ", "Description 1.", item.headline);
        Assert.assertEquals("1 link", "http://blog.example.com/?ts=1", item.link);
        Assert.assertEquals("1 guid", "http://blog.example.com/?ts=1", item.guid);
        Assert.assertEquals("1 date", null, item.date);
    }


    public void testProcessRdf() throws IOException {
        String xml = readFile("rdf.xml", "ISO-8859-1");
        service.processXml(xml, 1, fh);

        Assert.assertEquals("feed title", "Foo feed", feedTitle);
        Assert.assertEquals("feed description", "Foo bar baz", feedDescription);
        Assert.assertEquals("feed url", "http://example.com/", feedUrl);

        Assert.assertEquals("item count", 2, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("0 headline ", "Title 1", item.headline);
        Assert.assertEquals("0 description", "Description 1 äöüÄÖÜß", item.content);
        Assert.assertEquals("0 link", "http://example.com/l/feed?id=1", item.link);
        Assert.assertEquals("0 guid", "http://example.com/feed?id=1", item.guid);
        Assert.assertEquals("0 date", new Date(1301131080000l), item.date);

        item = itemInserted.get(1);
        Assert.assertEquals("1 headline ", "Title 2", item.headline);
        Assert.assertEquals("1 description", "Description 2", item.content);
        Assert.assertEquals("1 link", "http://example.com/l/feed?id=2", item.link);
        Assert.assertEquals("1 guid", "http://example.com/feed?id=2", item.guid);
        Assert.assertEquals("1 date", new Date(1301120520000l), item.date);
    }


    public void testProcessRdf2() throws IOException {
        String xml = readFile("rdf2.xml", "UTF-8");
        service.processXml(xml, 1, fh);

        Assert.assertEquals("feed title", "Foo RSS feed", feedTitle);
        Assert.assertEquals("feed description", "Foo mobil RSS", feedDescription);
        Assert.assertEquals("feed url", "http://mobil.example.com", feedUrl);

        Assert.assertEquals("item count", 2, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("1 headline ", "News item title 1", item.headline);
        Assert.assertEquals("1 description", "Foo news item title description 1 äöüÄÖÜß", item.content);
        Assert.assertEquals("1 link", "http://mobil.example.com/l/rss?id=1", item.link);
        Assert.assertEquals("1 guid", "http://mobil.example.com/g/rss?id=1", item.guid);
        Assert.assertEquals("1 date", new Date(1302775380000l), item.date);

        item = itemInserted.get(1);
        Assert.assertEquals("2 headline ", "News item title 2", item.headline);
        Assert.assertEquals("1 description", "Foo news item title description 2", item.content);
        Assert.assertEquals("2 link", "http://mobil.example.com/l/rss?id=2", item.link);
        Assert.assertEquals("2 guid", "http://mobil.example.com/g/rss?id=2", item.guid);
        Assert.assertEquals("2 date", new Date(1302765600000l), item.date);
    }


    public void testProcessAtom1() throws IOException {
        String xml = readFile("atom.xml", "UTF-8");
        service.processXml(xml, 1, fh);

        Assert.assertEquals("feed title", "Dummy News", feedTitle);
        Assert.assertEquals("feed description", "Dummy subtitle a", feedDescription);
        Assert.assertEquals("feed url", "http://www.example.com/", feedUrl);

        Assert.assertEquals("item count", 4, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("1 headline ", "News item title 1", item.headline);
        Assert.assertEquals("1 link", "http://www.example.com/newsticker/foo1", item.link);
        Assert.assertEquals("1 guid", "http://www.example.com/u/newsticker/foo1", item.guid);
        Assert.assertEquals("1 date ", new Date(1303052040000l), item.date);

        item = itemInserted.get(1);
        Assert.assertEquals("2 headline ", "News item title 2", item.headline);
        Assert.assertEquals("2 link", "http://www.example.com/newsticker/foo2", item.link);
        Assert.assertEquals("2 guid", "http://www.example.com/u/newsticker/foo2", item.guid);
        Assert.assertEquals("2 date " + item.date + " " + item.date.getTime(), new Date(1303047240000l), item.date);

    }


    public void testProcess10() throws IOException {
        final String xml = readFile("example10.xml", "utf-8");
        service.processXml(xml, 1, fh);

        Assert.assertEquals("feed name", "Foo Bar", feedTitle);
        Assert.assertEquals("feed description", "Foo description", feedDescription);
        Assert.assertEquals("feed url", "http://example.com/", feedUrl);

        Assert.assertEquals("item count", 2, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(0);
        Assert.assertEquals("1 headline", "Item 1 title", item.headline.trim());
        Assert.assertEquals("1 content ", "description 1", item.content);
        Assert.assertEquals("1 link", "http://example.com/rss?id=1", item.link);
        Assert.assertEquals("1 guid", "http://example.com/l/rss?id=1", item.guid);
        Assert.assertEquals("1 date ", new Date(1311620520000l), item.date);

    }


    public void testProcessScienceDaily() throws IOException {
        final String xml = readFile("site2.xml", "utf-8");
        service.processXml(xml, 1, fh);

        Assert.assertEquals("feed name", "FooDaily News", feedTitle);
        Assert.assertEquals("feed description", "Feed description.", feedDescription);
        Assert.assertEquals("feed url", "http://example.com/news/", feedUrl);

        Assert.assertEquals("item count", 2, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());

        Item item;
        item = itemInserted.get(1);
        Assert.assertEquals("1 headline", "Title 2", item.headline);
        Assert.assertEquals("1 content ", "Description 2", item.content);
        Assert.assertEquals("1 link", "http://feeds.example.com/l/feeds/2", item.link);
        Assert.assertEquals("1 guid", "http://feeds.example.com/g/feeds/2", item.guid);
        Assert.assertEquals("1 date x", new Date(1311861599000l), item.date);
    }


    private String readFile(String name, String encoding) throws IOException {
        final AssetManager assets = getInstrumentation().getContext().getResources().getAssets();
        final InputStream is = assets.open(name, AssetManager.ACCESS_STREAMING);
        final String str = Utils.readStream(is, encoding);
        return str;
    }


    public void testProcessHtml() {
        service.processXml(TEST_HTML, 1, fh);
        Assert.assertEquals("item count", 0, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());
    }


    public void testProcessNonXml() {
        service.processXml(TEST_NON_XML, 1, fh);
        Assert.assertEquals("item count", 0, itemInserted.size());
        Assert.assertEquals("error", false, fh.hasErrorOccurred());
    }

}
