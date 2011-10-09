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
package de.codefu.android.rss.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.UriHelper.UriParts;



/**
 * Content provider for RSS feed data.
 * <p>
 * The feed data contains all the meta-information about the feed but no news
 * items. The news items are dealt with in {@link ItemProvider}.
 * 
 * @author mj
 */
public class FeedProvider extends ContentProvider {

    /**
     * Constant value that means a feed's data should not be processed before
     * displaying it to the user as text.
     */
    public static int CLEAN_RAW = 0;
    /**
     * Constant value that means a feed's data should be stripped of HTML and
     * shown as text.
     */
    public static int CLEAN_STRIP_HTML = 1;
    /**
     * Constant value that means a feed's data should not be processed before
     * displaying it to the user in a browser view (i.e. full HTML with images
     * if the feed supplies some).
     */
    public static int CLEAN_FULL_HTML = 2;
    /**
     * The name of the table that keeps feed data.
     */
    public static final String FEEDS_NAME = "feeds";
    /**
     * Table column that has the feed's URL.
     */
    public static final String FEEDS_COL_URL = "url";
    /**
     * Table column that has the site's URL.
     */
    public static final String FEEDS_COL_SITEURL = "siteurl";
    /**
     * Table column that has the feed's name.
     */
    public static final String FEEDS_COL_NAME = "name";
    /**
     * Table column that has the feed's description.
     */
    public static final String FEEDS_COL_DESCRIPTION = "description";
    /**
     * Table column that has the feed's number of items.
     */
    public static final String FEEDS_COL_NUM = "num";
    /**
     * Table column that has the feed's number of unread items.
     */
    public static final String FEEDS_COL_NUM_UNREAD = "numunread";
    /**
     * Table column that has the the date the feed's data (items) was last
     * updated
     */
    public static final String FEEDS_COL_LASTPOLLDATE = "lastdate";
    /**
     * Table column that has the number of minutes between updates.
     */
    public static final String FEEDS_COL_AUTOPOLLMIN = "autopollmin";
    /**
     * Table column that has the feed's setting concerning how to process the
     * items' data. See {@link #CLEAN_RAW}, {@link #CLEAN_STRIP_HTML} etc.
     */
    public static final String FEEDS_COL_CLEANHTML = "cleanhtml";
    /**
     * This feed id means "all feeds".
     */
    public static final long ALL_FEEDS = -1;
    /**
     * The content URI used to query this provider.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://de.codefu.rss.feedprovider");
    /**
     * The fragment used to designate that the big feeds table is requested that
     * includes number of items and number of items read.
     */
    public static final String FANCY_FRAGMENT = "fancy";
    /**
     * The fragment used to designate that the name is to be overwritten.
     */
    public static final String FORCE_NAME_FRAGMENT = "force";
    /**
     * Convenience constant for querying the fancy feed cursor.
     */
    public static final Uri FEEDS_FANCY_URI = Uri.parse("content://de.codefu.rss.feedprovider/#" + FANCY_FRAGMENT);
    /**
     * The data type supported by the provider.
     */
    private static final String TYPE = "de.codefu.rss.feed";
    /**
     * Reference to the actual database.
     */
    private DB db;


    @Override
    public boolean onCreate() {
        db = DB.getInstance(getContext());
        return false;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        UriParts up = UriHelper.analyze(uri);
        if ((up.pathParts.isEmpty()) && up.hasId()) {
            return removeFeed(up.id);
        }
        return 0;
    }


    @Override
    public String getType(Uri uri) {
        final String lps = uri.getLastPathSegment();
        final boolean singleItem = isInteger(lps);
        return "vnd.android.cursor." + (singleItem ? "feed" : "dir") + "/vnd." + TYPE;
    }


    private boolean isInteger(String lps) {
        try {
            Integer.parseInt(lps);
        }
        catch (NumberFormatException e) {
            return false;
        }
        return true;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        UriParts up = UriHelper.analyze(uri);
        if (!up.hasPath() && !up.hasId()) {
            final String name = values.getAsString("name");
            final String url = values.getAsString("url");
            long id = addFeed(name, url);
            if (id != -1) {
                return ContentUris.withAppendedId(CONTENT_URI, id);
            }
        }
        return null;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        UriParts up = UriHelper.analyze(uri);
        if (!up.hasPath()) {
            if (up.hasId()) {
                return getPlainFeedsCursor(up.id);
            }
            else {
                if (up.hasFragment(FANCY_FRAGMENT)) {
                    return getFeedsCursor();
                }
                else {
                    return getPlainFeedsCursor();
                }
            }
        }
        return null;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        UriParts up = UriHelper.analyze(uri);
        if (!up.hasPath() && up.hasId()) {
            if (up.hasFragment(FORCE_NAME_FRAGMENT)) {
                updateFeed(up.id, true, values);
            }
            else {
                updateFeed(up.id, false, values);
            }
        }
        return 0;
    }


    public static String getCreateTable() {
        return "CREATE TABLE " + FEEDS_NAME + " (" //
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " //
                        + FEEDS_COL_NAME + " TEXT, " //
                        + FEEDS_COL_DESCRIPTION + " TEXT, " //
                        + FEEDS_COL_URL + " TEXT, " //
                        + FEEDS_COL_SITEURL + " TEXT, " //
                        + FEEDS_COL_AUTOPOLLMIN + " INTEGER, " //
                        + FEEDS_COL_LASTPOLLDATE + " INTEGER, " //
                        + FEEDS_COL_CLEANHTML + " INTEGER" //
                        + ");";
    }


    public static void addDefaultFeed(Context context, SQLiteDatabase db) {
        final String url = context.getString(R.string.default_rss_url);
        final String name = context.getString(R.string.default_rss_name);
        final ContentValues cv = new ContentValues();
        cv.put(FEEDS_COL_URL, url);
        if (name != null) {
            cv.put(FEEDS_COL_NAME, name);
        }

        db.insert(FEEDS_NAME, FEEDS_COL_NAME, cv);
    }


    public long addFeed(String name, String url) {
        final ContentValues cv = new ContentValues();
        cv.put(FEEDS_COL_URL, url);
        if (name != null) {
            cv.put(FEEDS_COL_NAME, name);
        }
        return db.getWritableDatabase().insert(FEEDS_NAME, FEEDS_COL_NAME, cv);
    }


    public Cursor getFeedsCursor() {
        final String q = "select _id,"
                        + FEEDS_COL_NAME
                        + ","//
                        + FEEDS_COL_URL
                        + "," //
                        + FEEDS_COL_DESCRIPTION
                        + ","//
                        + FEEDS_COL_SITEURL
                        + ","//
                        + FEEDS_COL_LASTPOLLDATE
                        + ", "//
                        + "(select count(*) from items where items." + ItemProvider.ITEMS_COL_FEEDID + "=feeds._id) "
                        + FEEDS_COL_NUM
                        + ", "//
                        + "(select count(*) from items where items." + ItemProvider.ITEMS_COL_FEEDID
                        + "=feeds._id and items." + ItemProvider.ITEMS_COL_READ + "=0) " + FEEDS_COL_NUM_UNREAD //
                        + " from " + FEEDS_NAME;
        final Cursor res = db.getReadableDatabase().rawQuery(q, new String[] {});
        return res;
    }


    public Cursor getPlainFeedsCursor() {
        final String q = "select _id," + FEEDS_COL_NAME + ","//
                        + FEEDS_COL_URL + "," //
                        + FEEDS_COL_DESCRIPTION + ","//
                        + FEEDS_COL_SITEURL + ","//
                        + FEEDS_COL_LASTPOLLDATE + ", "//
                        + FEEDS_COL_AUTOPOLLMIN + ", " //
                        + FEEDS_COL_CLEANHTML //
                        + " from " + FEEDS_NAME;
        final Cursor res = db.getReadableDatabase().rawQuery(q, new String[] {});
        return res;
    }


    public Cursor getPlainFeedsCursor(long id) {
        final String q = "select _id," + FEEDS_COL_NAME + ","//
                        + FEEDS_COL_URL + "," //
                        + FEEDS_COL_DESCRIPTION + ","//
                        + FEEDS_COL_SITEURL + ","//
                        + FEEDS_COL_LASTPOLLDATE + ", "//
                        + FEEDS_COL_AUTOPOLLMIN + ", " //
                        + FEEDS_COL_CLEANHTML //
                        + " from " + FEEDS_NAME //
                        + " where _id=?";
        final Cursor res = db.getReadableDatabase().rawQuery(q, new String[] {
            Long.toString(id)
        });
        return res;
    }


    public String getFeedUrl(long id) {
        return getFeedData(id, FEEDS_COL_URL);
    }


    public String getFeedName(long id) {
        return getFeedData(id, FEEDS_COL_NAME);
    }


    public String getFeedData(long id, String colName) {
        final String[] cols = new String[] {
            colName
        };
        final String[] selectParams = new String[] {
            Long.toString(id)
        };
        final Cursor res = db.getReadableDatabase().query(FEEDS_NAME, cols, "_id=?", selectParams, null, null, null);
        String data = null;
        if (res.moveToNext()) {
            data = res.getString(res.getColumnIndex(colName));
        }
        res.close();
        return data;
    }


    public void updateFeed(long id, boolean forceName, ContentValues cv) {
        db.getWritableDatabase().beginTransaction();

        if (!forceName) {
            // remove name from cv if name already set in feed data
            final String[] cols = new String[] {
                FEEDS_COL_NAME
            };
            final String[] selectParams = new String[] {
                Long.toString(id)
            };
            final Cursor res = db.getReadableDatabase()
                            .query(FEEDS_NAME, cols, "_id=?", selectParams, null, null, null);
            if (res != null) {
                if (res.moveToFirst()) {
                    String name = res.getString(res.getColumnIndex(FEEDS_COL_NAME));
                    if ((name != null) && (name.length() != 0)) {
                        cv.remove(FEEDS_COL_NAME);
                    }
                }
                res.close();
            }
        }

        cv.put(FEEDS_COL_LASTPOLLDATE, System.currentTimeMillis());
        db.getWritableDatabase().update(FEEDS_NAME, cv, "_id=?", new String[] {
            Long.toString(id)
        });
        db.getWritableDatabase().setTransactionSuccessful();
        db.getWritableDatabase().endTransaction();
    }


    public int removeFeed(long feedId) {
        return db.getWritableDatabase().delete(FEEDS_NAME, "_id=?", new String[] {
            "" + feedId
        });
    }

}
