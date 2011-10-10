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
import java.io.StringReader;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.db.ItemProvider;



/**
 * The service that gets feed data or a reference to a database record,
 * processes that data through a SAX parser into individual news items and
 * inserts these into the database.
 * 
 * @author mj
 */
public class InsertService extends IntentService implements FeedHandlerClient {

    public InsertService() {
        super("InsertService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            ServiceComm.IntentContent ic = ServiceComm.getInsertContent(this, intent);
            Log.i("InsertService", "Inserting content for feed " + ic.feedId);

            final int cleanHtml = getCleanHtml(ic.feedId);
            if (cleanHtml != -1) {
                final TimeZone tz = TimeZone.getDefault();
                process(ic.content, ic.feedId, new FeedHandler(ic.feedId, cleanHtml, this, tz));
                ServiceComm.sendDataChangedBroadcast(this);
            }
        }

        WakeLockHolder.getInstance().release(this);
    }


    private int getCleanHtml(final long feedId) {
        final Uri uri = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
        final Cursor c = getContentResolver().query(uri, null, null, null, null);
        int cleanHtml = -1;
        if (c != null) {
            if (c.moveToFirst()) {
                cleanHtml = c.getInt(c.getColumnIndex(FeedProvider.FEEDS_COL_CLEANHTML));
            }
            c.close();
        }
        return cleanHtml;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void process(final String content, final long feedId, final FeedHandler feedHandler) {
        processXml(content, feedId, feedHandler);
        Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
        uri = uri.buildUpon().encodedFragment("move").build();
        getContentResolver().insert(uri, null);
    }


    void processXml(final String content, final long feedId, final FeedHandler feedHandler) {
        final SAXParserFactory fac = SAXParserFactory.newInstance();
        try {
            final SAXParser parser = fac.newSAXParser();
            InputSource source = new InputSource(new StringReader(content));
            parser.parse(source, feedHandler);
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addItem(long feedId, int cleanHtml, Item item) {
        if (cleanHtml == FeedProvider.CLEAN_STRIP_HTML) {
            item.content = Utils.htmlClean(item.content);
            item.headline = Utils.htmlClean(item.headline);
        }
        Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
        getContentResolver().insert(uri, asContentValues(item));
    }


    public ContentValues asContentValues(Item item) {
        ContentValues cv = new ContentValues();
        if (item.headline != null) {
            cv.put(ItemProvider.ITEMS_COL_HEADLINE, item.headline);
        }
        if (item.content != null) {
            cv.put(ItemProvider.ITEMS_COL_CONTENT, item.content);
        }
        if (item.date != null) {
            cv.put(ItemProvider.ITEMS_COL_DATE, item.date.getTime());
        }
        if (item.link != null) {
            cv.put(ItemProvider.ITEMS_COL_LINK, item.link.toString());
        }
        if (item.guid != null) {
            cv.put(ItemProvider.ITEMS_COL_GUID, item.guid.toString());
        }
        return cv;
    }


    public void updateFeed(long feedId, String name, String description, String url) {
        ContentValues cv = new ContentValues();
        cv.put(FeedProvider.FEEDS_COL_NAME, name);
        cv.put(FeedProvider.FEEDS_COL_DESCRIPTION, description);
        cv.put(FeedProvider.FEEDS_COL_SITEURL, url);
        Uri uri = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
        getContentResolver().update(uri, cv, null, null);
    }

}
