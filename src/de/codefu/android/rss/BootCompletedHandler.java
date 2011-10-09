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
package de.codefu.android.rss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.codefu.android.rss.db.ItemProvider;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Handles BOOT_COMPLETE message and makes sure that the auto poll mechanism is
 * started.
 * 
 * @author mj
 */
public class BootCompletedHandler extends BroadcastReceiver {

    /**
     * The tag for logging.
     */
    private static final String TAG = "BootCompletedHandler";


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            deleteOrphanAuxRecords(context);
            handleAutopollPref(context);
        }

        if (Intent.ACTION_TIME_CHANGED.equals(action)) {
            handleAutopollPref(context);
        }
    }


    /**
     * Starts auto poll service if related preference option is set.
     * 
     * @param context
     *            the context
     */
    private void handleAutopollPref(Context context) {
        if (MainPreferences.getAutoPoll(context)) {
            ServiceComm.sendAutoPollIntent(context);
            Log.i(TAG, "Started auto poll service after boot");
        }
    }


    /**
     * Delete orphaned records in auxiliary table.
     * 
     * @param context
     *            the context
     */
    private void deleteOrphanAuxRecords(Context context) {
        context.getContentResolver().delete(ItemProvider.CONTENT_URI_AUX, null, null);
        Log.i(TAG, "Cleaned aux table after boot");
    }
}
