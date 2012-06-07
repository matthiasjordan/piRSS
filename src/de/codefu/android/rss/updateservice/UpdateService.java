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

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.codefu.android.rss.db.FeedProvider;



/**
 * The service that deals with downloading data from RSS feeds.
 * <p>
 * The insertion is dealt with by {@link InsertService}.
 * 
 * @author mj
 */
public class UpdateService extends IntentService {

    /**
     * The download timeout in milliseconds. If a download takes longer than
     * this it will be aborted.
     */
    private static final int DOWNLOAD_TIMEOUT_MS = 120 * 1000;
    /**
     * The connection timeout in milliseconds.
     */
    private static final int CONNECT_TIMEOUT_MS = 10 * 1000;


    public UpdateService() {
        super("UpdateService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        long feedId = FeedProvider.ALL_FEEDS;
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            feedId = extras.getLong(ServiceComm.FEED_ID);
        }

        ServiceComm.sendPollingStartedBroadcast(this);

        if (feedId == FeedProvider.ALL_FEEDS) {
            final Uri uri = FeedProvider.CONTENT_URI;
            final Cursor feeds = getContentResolver().query(uri, null, null, null, null);
            if (feeds != null) {
                final  int ciLPDate = feeds.getColumnIndex(FeedProvider.FEEDS_COL_LASTPOLLDATE);
                final int ciUrl = feeds.getColumnIndex(FeedProvider.FEEDS_COL_URL);
                final int ciId = feeds.getColumnIndex("_id");
                while (feeds.moveToNext()) {
                    final long id = feeds.getLong(ciId);
                    final String urlStr = feeds.getString(ciUrl);
                    final long lastPollDateMs = feeds.getLong(ciLPDate);
                    Log.i("UpdateService", "Polling " + id + " - " + urlStr);
                    poll(id, urlStr, lastPollDateMs);
                }
                feeds.close();
            }
        }
        else {
            final Uri uri = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
            final Cursor feed = getContentResolver().query(uri, null, null, null, null);
            if (feed != null) {
                if (feed.moveToFirst()) {
                    final String urlStr = feed.getString(feed.getColumnIndex(FeedProvider.FEEDS_COL_URL));
                    final long lastPollDateMs = feed.getLong(feed.getColumnIndex(FeedProvider.FEEDS_COL_LASTPOLLDATE));
                    poll(feedId, urlStr, lastPollDateMs);
                }
                feed.close();
            }
        }

        WakeLockHolder.getInstance().release(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     * Uses {@link UrlHttpRetriever} to download feed data and sends that data
     * to the {@link InsertService}.
     * 
     * @param feedId
     *            the ID of the feed to poll
     * @param urlStr
     *            the URL of the feed's data
     * @param lastPollDateMs 
     */
    private void poll(final long feedId, final String urlStr, long lastPollDateMs) {
        if (urlStr == null) {
            return;
        }

        final UrlHttpRetriever retriever = new UrlHttpRetriever();
        final String responseBody = retriever.retrieveHttpContent(urlStr, lastPollDateMs, CONNECT_TIMEOUT_MS, DOWNLOAD_TIMEOUT_MS);
        if (responseBody != null) {
            final Intent i = ServiceComm.createInsertIntent(this, feedId, responseBody);
            Log.i("UpdateService", "starting Insert service for feed " + feedId);
            WakeLockHolder.getInstance().acquire(this);
            startService(i);
        }
        else {
            ServiceComm.sendPollingProblemBroadcast(this, feedId);
        }
    }

}
