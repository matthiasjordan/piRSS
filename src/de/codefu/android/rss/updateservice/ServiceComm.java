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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.codefu.android.rss.CursorChangedReceiver;
import de.codefu.android.rss.db.ItemProvider;



/**
 * Helper that defines the communication between the Activities and Services.
 * <p>
 * One important thing covered here are insert intents. An insert intent is an
 * intent whose receiver is the {@link InsertService} and that communicates that
 * some RSS feed data should be inserted into the database. This intent has two
 * different forms:
 * <ul>
 * <li>One uses the {@link #CONTENT}</li> extra whose value is the data that was
 * downloaded from the RSS feed. </li>
 * <li>The other form uses the {@link #CONTENT_REF} extra whose value is the ID
 * of a row in an auxiliary database table that has the data that was downloaded
 * from the RSS feed.</li>
 * </ul>
 * The reason for this intent having two different kinds of behavior is that
 * normally passing the RSS feed data directly is better in terms of CPU cycles.
 * But an intent is passed using RPC and for RPC data there is a maximum size.
 * So whenever feed data is larger than that maximum size {@link ServiceComm}
 * stores it in the database and passes the ID of that database entry using the
 * {@link #CONTENT_REF} extra.
 * 
 * @author mj
 */
public class ServiceComm {

    /**
     * The key for the extra hat has the ID of the feed whose data is attached
     * or referenced.
     */
    public static final String FEED_ID = "feedid";
    /**
     * The key for the extra that has the RSS feed data.
     */
    private static final String CONTENT = "content";
    /**
     * The key for the extra that has the ID of the row in the auxiliary table
     * where the RSS feed data is stored.
     */
    private static final String CONTENT_REF = "contentid";


    /**
     * Wrapper for the information extracted from the insert intent.
     */
    public static class IntentContent {

        /**
         * The data to insert.
         */
        public String content;
        /**
         * The ID of the feed for which to insert the data.
         */
        public long feedId;
    }


    /**
     * The maximum size of data that can be stored in an intent.
     */
    // TODO: Get correct max size
    private static final int MAX_RPC_SIZE = 50 * 1024;
    /**
     * Name of a broadcast that is sent when polling starts.
     */
    public static final String POLLING_STARTED = "pollingstarted";
    /**
     * Name of a broadcast that is sent when there are problems during polling.
     */
    public static final String POLLING_PROBLEM = "pollingproblem";


    /**
     * Creates an insert intent.
     * <p>
     * If the data given is too large for the RPC system (larger than
     * {@link #MAX_RPC_SIZE}) the data is stored in a database and the ID of
     * that data in the database is stored in the intent. Otherwise the data
     * itself is stored in the intent.
     * 
     * @param c
     *            the context to create the intent for
     * @param feedId
     *            the ID of the feed whose data is in body
     * @param body
     *            the data of the RSS feed
     * @return an intent object ready for sending
     */
    public static Intent createInsertIntent(Context c, long feedId, String body) {
        final Intent i = new Intent(c, InsertService.class);
        if (body.length() > MAX_RPC_SIZE) {
            final Uri uri = ItemProvider.CONTENT_URI_AUX;
            final ContentValues cv = new ContentValues();
            cv.put("content", body);
            final Uri id = c.getContentResolver().insert(uri, cv);
            i.putExtra(CONTENT_REF, id);
        }
        else {
            i.putExtra(CONTENT, body);
        }
        i.putExtra(FEED_ID, feedId);
        Log.d("ServComm", "Created " + i);
        return i;
    }


    /**
     * Takes an insert intent created with
     * {@link #createInsertIntent(Context, long, String)} and retrieves the data
     * in it.
     * <p>
     * The handling of the data in the intent (reference or directly attached
     * data) is totally transparent. The caller also does not have to care about
     * the maintenance of the auxiliary table.
     * 
     * @param c
     *            the content to use for a possible database access
     * @param intent
     *            the intent to read from
     * @return the object with the data and feed ID read from the intent.
     */
    public static IntentContent getInsertContent(Context c, Intent intent) {
        final IntentContent ic = new IntentContent();
        final Bundle extras = intent.getExtras();
        final Object contentRefO = extras.get(CONTENT_REF);

        ic.feedId = extras.getLong(FEED_ID);

        if (contentRefO != null) {
            if (contentRefO instanceof Uri) {
                final Uri contentRef = (Uri) contentRefO;
                final Cursor cursor = c.getContentResolver().query(contentRef, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        ic.content = cursor.getString(cursor.getColumnIndex(ItemProvider.AUX_COL_CONTENT));
                    }
                    cursor.close();
                }
                c.getContentResolver().delete(contentRef, null, null);
            }
            Log.i("ServComm", "Read intent for feed " + ic.feedId + " from aux table");
        }
        else {
            ic.content = extras.getString(CONTENT);
            Log.i("ServComm", "Read intent for feed " + ic.feedId);
        }
        return ic;
    }


    private static void sendBroadcast(Context context, String action) {
        final Intent intent = new Intent(action);
        intent.setPackage(CursorChangedReceiver.PACKAGE_NAME);
        context.sendBroadcast(intent);
    }


    /**
     * Sends a broadcast to announce that the data in the DB has changed.
     * 
     * @param context
     *            the content to use for sending the intent
     */
    public static void sendDataChangedBroadcast(Context context) {
        sendBroadcast(context, CursorChangedReceiver.DATA_CHANGED);
    }


    /**
     * Sends a broadcast to announce that polling has stated.
     * 
     * @param context
     *            the content to use for sending the intent
     */
    public static void sendPollingStartedBroadcast(Context context) {
        sendBroadcast(context, POLLING_STARTED);
    }


    /**
     * Sends a broadcast to announce that there was a problem during the
     * download for a given feed.
     * 
     * @param context
     *            the content to use for sending the intent
     * @param feedId
     *            the ID of the feed that was attempted to poll
     */
    public static void sendPollingProblemBroadcast(Context context, long feedId) {
        final Intent intent = new Intent(POLLING_PROBLEM);
        intent.setPackage(CursorChangedReceiver.PACKAGE_NAME);
        intent.putExtra(FEED_ID, feedId);
        context.sendBroadcast(intent);
    }


    public static void sendPollIntent(Context context, long feedId) {
        final Intent i = new Intent(context, UpdateService.class);
        i.putExtra(ServiceComm.FEED_ID, feedId);
        context.startService(i);
    }


    /**
     * Sends an intent to the {@link AutoPollService} to trigger polling the
     * feeds for which automatic polling is configured.
     * 
     * @param context
     *            the context to use for sending the intent
     */
    public static void sendAutoPollIntent(Context context) {
        final Intent i = new Intent(context, AutoPollService.class);
        context.startService(i);
    }
}
