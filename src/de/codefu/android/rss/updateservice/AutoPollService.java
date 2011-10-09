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

import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import de.codefu.android.rss.MainPreferences;
import de.codefu.android.rss.db.FeedProvider;



/**
 * Handles the automatic polling.
 * <p>
 * When being called, iterates over all feeds and collects those that should be
 * called based on their last poll time and poll frequency. Then it sets the
 * alarm to the next time a feed wants to be polled.
 * 
 * @author mj
 */
public class AutoPollService extends IntentService {

    /**
     * Tag for the logger.
     */
    private static final String TAG = "AutoPollService";
    /**
     * Maximum number of milliseconds between now and the calculated next poll
     * time for a feed that we consider equal.
     */
    private static final long POLL_SLACK_MS = 30 * 1000;


    public AutoPollService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent arg0) {
        WakeLockHolder.getInstance().acquire(this);

        Log.i(TAG, "started");
        final Cursor feeds = getContentResolver().query(FeedProvider.CONTENT_URI, null, null, null, null);

        if ((feeds != null) && isAutoPollingEnabled()) {
            final long now = System.currentTimeMillis();
            final int idI = feeds.getColumnIndex("_id");
            final int lastPolledI = feeds.getColumnIndex(FeedProvider.FEEDS_COL_LASTPOLLDATE);
            final int pollMinI = feeds.getColumnIndex(FeedProvider.FEEDS_COL_AUTOPOLLMIN);

            long nextAlarmMs = Long.MAX_VALUE;
            while (feeds.moveToNext()) {
                final long feedId = feeds.getLong(idI);
                final long lastPolledMs = feeds.getLong(lastPolledI);
                final long pollMin = feeds.getLong(pollMinI);

                if (pollMin != 0) {
                    final long pollMs = pollMin * 60 * 1000;
                    long nextPollMs = lastPolledMs + pollMs;
                    if (feedShouldBePolledNow(nextPollMs, now)) {
                        Log.i(TAG, "polling " + feedId);
                        WakeLockHolder.getInstance().acquire(this);

                        ServiceComm.sendPollIntent(this, feedId);
                        nextPollMs = System.currentTimeMillis() + pollMs;
                    }

                    if (nextPollMs < nextAlarmMs) {
                        nextAlarmMs = nextPollMs;
                    }
                }
            }
            feeds.close();
            scheduleNextAlarm(nextAlarmMs);
            Log.i(TAG, "finished");
        }

        WakeLockHolder.getInstance().release(this);
    }


    private void scheduleNextAlarm(long nextAlarmMs) {
        final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        final Intent i = new Intent(this, AutoPollService.class);
        final PendingIntent p = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        final boolean someFeedWantsToBePolled = nextAlarmMs != Long.MAX_VALUE;
        final boolean pollingEnabled = isAutoPollingEnabled();
        if (someFeedWantsToBePolled && pollingEnabled) {
            Log.i(TAG, "Setting next alarm at " + new Date(nextAlarmMs));
            am.set(AlarmManager.RTC_WAKEUP, nextAlarmMs, p);
        }
        else {
            Log.i(TAG, "Cancelled alarm");
            am.cancel(p);
        }
    }


    private boolean isAutoPollingEnabled() {
        return MainPreferences.getAutoPoll(this);
    }


    private boolean feedShouldBePolledNow(long nextPollMs, long now) {
        return (now + POLL_SLACK_MS) > nextPollMs;
    }

}
