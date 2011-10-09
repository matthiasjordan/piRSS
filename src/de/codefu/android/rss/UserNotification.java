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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import de.codefu.android.rss.feedlist.FeedList;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Class with helper methods for handling user notifications.
 * 
 * @author mj
 */
public class UserNotification {

    public static void notifyUser(Context context, String contentTitle, String contentText) {
        final int id = 1;
        final Intent notificationIntent = new Intent(context, FeedList.class);

        final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        final Notification notification = new Notification(R.drawable.notification_icon, contentTitle,
                        System.currentTimeMillis());
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }


    public static BroadcastReceiver registerPollingProblemReceiver(Context context) {
        BroadcastReceiver br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("UserNotification", "Handling POLLING_PROBLEM");
                final String title = context.getString(R.string.usernot_title);
                final String text = context.getString(R.string.usernot_text);
                notifyUser(context, title, text);
            }
        };
        context.registerReceiver(br, new IntentFilter(ServiceComm.POLLING_PROBLEM));
        return br;
    }
}
