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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import de.codefu.android.rss.db.UriHelper.UriParts;



/**
 * Content provider for news items.
 * <p>
 * The URIs are in this format:
 * <ul>
 * <li>.../11 - an action regarding one item (11)</li>
 * <li>.../feed/22 - an action regarding items in feed 22</li>
 * <li>.../...#read - an action regarding read items</li>
 * </ul>
 * 
 * @author mj
 */
public class ItemProvider extends ContentProvider {

    public static final String ITEMS_NAME = "items";
    public static final String ITEMS_TMP_NAME = "itemstmp";
    public static final String ITEMS_COL_FEEDID = "_feedid";
    public static final String ITEMS_COL_GUID = "guid";
    public static final String ITEMS_COL_HEADLINE = "headline";
    public static final String ITEMS_COL_CONTENT = "content";
    public static final String ITEMS_COL_DATE = "date";
    public static final String ITEMS_COL_LINK = "link";
    public static final String ITEMS_COL_READ = "read";
    public static final String ITEMS_COL_KEEPER = "keeper";

    private static final String ITEMS_COLHACK = ITEMS_COL_HEADLINE;

    public static final Uri CONTENT_URI = Uri.parse("content://de.codefu.rss.itemprovider");

    public static final String FEED = "feed";

    public static final String READ = "read";
    public static final String UNREAD = "unread";
    public static final String KEEP = "keep";
    public static final String UNKEEP = "unkeep";

    public static final String AUX = "aux";

    public static final Uri CONTENT_URI_FEED = Uri.parse("content://de.codefu.rss.itemprovider/" + FEED);

    public static final Uri CONTENT_URI_AUX = Uri.parse("content://de.codefu.rss.itemprovider/" + AUX);

    private static final String TYPE = "de.codefu.rss.item";
    private static final String SKIP_KEEPERS = " AND " + ITEMS_COL_KEEPER + " IS NULL OR 0=" + ITEMS_COL_KEEPER;

    private DB db;


    @Override
    public boolean onCreate() {
        db = DB.getInstance(getContext());
        return false;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        UriParts up = UriHelper.analyze(uri);
        if (up.hasPath(FEED) && up.hasId()) {
            if (!up.hasFragment(READ)) {
                return removeAllItemsFromFeed(up.id);
            }
            else {
                return removeReadItemsFromFeed(up.id);
            }
        }
        else if (up.hasPath(AUX) && !up.hasId()) {
            cleanAuxTable();
        }
        else if (up.hasPath(AUX) && up.hasId()) {
            deleteAuxContent(up.id);
        }
        return 0;
    }


    @Override
    public String getType(Uri uri) {
        final String lps = uri.getLastPathSegment();
        final boolean singleItem = isInteger(lps);
        return "vnd.android.cursor." + (singleItem ? "item" : "dir") + "/vnd." + TYPE;
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
        if (up.hasPath(FEED) && up.hasId()) {
            if (up.hasFragment("move")) {
                moveItemsToFinalTable(up.id);
            }
            else {
                addItemToTmpTable(up.id, values);
            }
        }
        else if (up.hasPath(AUX) && !up.hasId()) {
            String content = values.getAsString("content");
            long id = addAuxContent(content);
            if (id != -1) {
                return ContentUris.withAppendedId(CONTENT_URI_AUX, id);
            }
        }
        return null;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        UriParts up = UriHelper.analyze(uri);
        if (!up.hasPath() && up.hasId()) {
            if (up.hasFragment("keeper")) {
                return getKeeperCursor(up.id);
            }
            else {
                setItemRead(up.id);
                return getItemCursor(up.id);
            }
        }
        else if (up.hasPath(FEED) && up.hasId()) {
            return getItemsCursor(up.id);
        }
        else if (up.hasPath(AUX) && up.hasId()) {
            return getAuxContent(up.id);
        }
        return null;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        UriParts up = UriHelper.analyze(uri);
        if (up.hasFragment(READ)) {
            if (!up.hasPath() && up.hasId()) {
                return setItemRead(up.id);
            }
            if (up.hasPath(FEED) && up.hasId()) {
                return setAllItemsRead(up.id);
            }
        }
        else if (up.hasFragment(KEEP)) {
            return setKeep(up.id, 1);
        }
        else if (up.hasFragment(UNKEEP)) {
            return setKeep(up.id, 0);
        }
        return 0;
    }


    private int setKeep(long id, int i) {
        final ContentValues cv = new ContentValues();
        cv.put(ITEMS_COL_KEEPER, i);
        return db.getWritableDatabase().update(ITEMS_NAME, cv, "_id=?", new String[] {
            "" + id
        });
    }


    public static String getCreateTable() {
        return getCreateTableInner(ITEMS_NAME);
    }


    public static String getCreateTmpTable() {
        return getCreateTableInner(ITEMS_TMP_NAME);
    }


    private static String getCreateTableInner(String name) {
        return "CREATE TABLE " + name + " (" //
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " //
                        + ITEMS_COL_READ + " INTEGER default 0, " //
                        + ITEMS_COL_FEEDID + " INTEGER REFERENCES " + FeedProvider.FEEDS_NAME + ", " //
                        + ITEMS_COL_GUID + " TEXT UNIQUE ON CONFLICT IGNORE, " //
                        + ITEMS_COL_HEADLINE + " TEXT, " //
                        + ITEMS_COL_CONTENT + " TEXT UNIQUE ON CONFLICT IGNORE, " //
                        + ITEMS_COL_DATE + " INTEGER, " //
                        + ITEMS_COL_KEEPER + " INTEGER, " //
                        + ITEMS_COL_LINK + " TEXT " + ");";
    }


    private static String thisOrAllFeeds(long feedId) {
        return "(" + ITEMS_COL_FEEDID + "=? OR " + feedId + "=" + FeedProvider.ALL_FEEDS + ")";
    }


    public void addItemToTmpTable(long feedId, ContentValues cv) {
        cv.put(ITEMS_COL_FEEDID, feedId);
        db.getWritableDatabase().insert(ITEMS_TMP_NAME, ITEMS_COLHACK, cv);
    }


    // public Item getItem(long id) {
    // Item item = null;
    //
    // final Cursor res = getItemCursor(id);
    // if (res.moveToNext()) {
    // item = new Item();
    // item.headline = res.getString(res.getColumnIndex(ITEMS_COL_HEADLINE));
    // item.content = res.getString(res.getColumnIndex(ITEMS_COL_CONTENT));
    // final long dateLong = res.getLong(res.getColumnIndex(ITEMS_COL_DATE));
    // item.date = new Date(dateLong);
    // item.link = res.getString(res.getColumnIndex(ITEMS_COL_LINK));
    // item.feedName =
    // res.getString(res.getColumnIndex(FeedProvider.FEEDS_COL_NAME));
    // }
    //
    // res.close();
    // return item;
    // }

    public Cursor getItemCursor(long id) {
        final Cursor res = db.getReadableDatabase().query(
                        ITEMS_NAME + " JOIN " + FeedProvider.FEEDS_NAME + " ON " + ITEMS_NAME + "." + ITEMS_COL_FEEDID
                                        + "=" + FeedProvider.FEEDS_NAME + "._id",
                        new String[] {
                                        ITEMS_COL_HEADLINE, ITEMS_COL_CONTENT, ITEMS_COL_DATE, ITEMS_COL_LINK,
                                        FeedProvider.FEEDS_COL_NAME
                        }, ITEMS_NAME + "._id=?", new String[] {
                            Long.toString(id)
                        }, null, null, null);
        return res;
    }


    public Cursor getItemsCursor(long feedId) {
        final Cursor res = db.getReadableDatabase().query(
                        ITEMS_NAME + " JOIN " + FeedProvider.FEEDS_NAME + " ON " + ITEMS_NAME + "." + ITEMS_COL_FEEDID
                                        + "=" + FeedProvider.FEEDS_NAME + "._id", new String[] {
                                        ITEMS_NAME + "._id", //
                                        FeedProvider.FEEDS_COL_NAME,//
                                        FeedProvider.FEEDS_COL_CLEANHTML,//
                                        ITEMS_COL_HEADLINE, //
                                        ITEMS_COL_CONTENT, //
                                        ITEMS_COL_DATE, //
                                        ITEMS_COL_READ, //
                                        ITEMS_COL_KEEPER, //
                                        ITEMS_COL_LINK
                        }, thisOrAllFeeds(feedId), new String[] {
                            Long.toString(feedId)
                        }, null, null, ITEMS_NAME + "._id DESC");
        return res;
    }


    private Cursor getKeeperCursor(long id) {
        final Cursor res = db.getReadableDatabase().query(ITEMS_NAME, new String[] {
                        "_id", ITEMS_COL_KEEPER
        }, ITEMS_NAME + "._id=?", new String[] {
            Long.toString(id)
        }, null, null, null);
        return res;
    }


    public int removeAllItemsFromFeed(long feedId) {
        return db.getWritableDatabase().delete(ITEMS_NAME, thisOrAllFeeds(feedId) + SKIP_KEEPERS, new String[] {
            Long.toString(feedId)
        });
    }


    public int setItemRead(long id) {
        final ContentValues cv = new ContentValues();
        cv.put(ITEMS_COL_READ, 1);
        return db.getWritableDatabase().update(ITEMS_NAME, cv, "_id=?", new String[] {
            Long.toString(id)
        });
    }


    public int setAllItemsRead(long feedId) {
        final ContentValues cv = new ContentValues();
        cv.put(ITEMS_COL_READ, 1);
        return db.getWritableDatabase().update(ITEMS_NAME, cv, thisOrAllFeeds(feedId), new String[] {
            Long.toString(feedId)
        });
    }


    public int removeReadItemsFromFeed(long feedId) {
        return db.getWritableDatabase().delete(ITEMS_NAME,
                        ITEMS_COL_READ + "=1 AND " + thisOrAllFeeds(feedId) + SKIP_KEEPERS, new String[] {
                            Long.toString(feedId)
                        });
    }


    public void moveItemsToFinalTable(long feedId) {
        final SQLiteDatabase wdb = db.getWritableDatabase();
        final Cursor c = wdb.query(ITEMS_TMP_NAME, new String[] {
            "*"
        }, ITEMS_COL_FEEDID + "=?", new String[] {
            "" + feedId
        }, null, null, "_id DESC");
        final int idIndex = c.getColumnIndex("_id");
        final String[] columnNames = c.getColumnNames();
        while (c.moveToNext()) {
            wdb.beginTransaction();
            try {
                final long id = c.getLong(idIndex);
                final ContentValues cv = new ContentValues();
                final int cc = c.getColumnCount();
                for (int column = 0; (column < cc); column++) {
                    if (column != idIndex) {
                        cv.put(columnNames[column], c.getString(column));
                    }
                }
                wdb.insert(ITEMS_NAME, ITEMS_COLHACK, cv);
                wdb.delete(ITEMS_TMP_NAME, "_id=?", new String[] {
                    Long.toString(id)
                });
                wdb.setTransactionSuccessful();
            }
            finally {
                wdb.endTransaction();
            }
        }
        c.close();
    }


    private static final String AUX_NAME = "tmp";
    public static final String AUX_COL_CONTENT = "content";


    public static String getCreateAuxTable() {
        return "CREATE TABLE " + AUX_NAME + " (" //
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT, " //
                        + AUX_COL_CONTENT + " TEXT " //
                        + ");";
    }


    private long addAuxContent(String content) {
        final ContentValues values = new ContentValues();
        values.put(AUX_COL_CONTENT, content);
        return db.getWritableDatabase().insert(AUX_NAME, AUX_COL_CONTENT, values);
    }


    private Cursor getAuxContent(long id) {
        final SQLiteDatabase wdb = db.getWritableDatabase();
        wdb.beginTransaction();
        try {
            final String idStr = Long.toString(id);
            final Cursor c = wdb.query(AUX_NAME, new String[] {
                AUX_COL_CONTENT
            }, "_id=?", new String[] {
                idStr
            }, null, null, null);
            wdb.setTransactionSuccessful();
            return c;
        }
        finally {
            wdb.endTransaction();
        }
    }


    private void deleteAuxContent(long id) {
        final SQLiteDatabase wdb = db.getWritableDatabase();
        wdb.beginTransaction();
        try {
            final String idStr = Long.toString(id);
            wdb.delete(AUX_NAME, "_id=?", new String[] {
                idStr
            });
            wdb.setTransactionSuccessful();
        }
        finally {
            wdb.endTransaction();
        }
    }


    private void cleanAuxTable() {
        final SQLiteDatabase wdb = db.getWritableDatabase();
        wdb.beginTransaction();
        try {
            wdb.execSQL("delete from " + AUX_NAME + ";");
            wdb.setTransactionSuccessful();
        }
        finally {
            wdb.endTransaction();
        }
    }

}
