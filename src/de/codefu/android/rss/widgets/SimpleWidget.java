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
package de.codefu.android.rss.widgets;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.db.ItemProvider;
import de.codefu.android.rss.feedlist.FeedList;



/**
 * Provides the simple widget with information.
 * 
 * @author mj
 */
public class SimpleWidget extends AppWidgetProvider {

    /**
     * How many lines (or: items) to show.
     */
    private static final int LINES = 3;


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName provider = new ComponentName(context, SimpleWidget.class);
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] widgetIds = appWidgetManager.getAppWidgetIds(provider);
        onUpdate(context, appWidgetManager, widgetIds);
        super.onReceive(context, intent);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final ArrayList<String> headlines = getHeadlines(context);

        for (int appWidgetNum = 0; appWidgetNum < appWidgetIds.length; appWidgetNum++) {
            final int appWidgetId = appWidgetIds[appWidgetNum];

            final Intent intent = new Intent(context, FeedList.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple);
            views.setOnClickPendingIntent(R.id.widget_simple_outer, pendingIntent);
            setLine(views, headlines, R.id.widget_line_0, 0);
            setLine(views, headlines, R.id.widget_line_1, 1);
            setLine(views, headlines, R.id.widget_line_2, 2);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


    private ArrayList<String> getHeadlines(final Context context) {
        final Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, FeedProvider.ALL_FEEDS);
        final Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        final ArrayList<String> headlines = new ArrayList<String>();
        if ((cursor != null)) {
            int headlineNum = 0;

            while ((headlineNum++ < LINES) && cursor.moveToNext()) {
                String headline = cursor.getString(cursor.getColumnIndex(ItemProvider.ITEMS_COL_HEADLINE));
                headlines.add(headline);
            }
        }
        cursor.close();
        return headlines;
    }


    private void setLine(final RemoteViews views, final ArrayList<String> headlines, final int id, final int num) {
        if (num < headlines.size()) {
            final String headline = headlines.get(num);
            views.setTextViewText(id, headline);
        }
        else {
            views.setTextViewText(id, "");
        }
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }
}
