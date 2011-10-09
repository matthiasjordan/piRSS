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
package de.codefu.android.rss.feedlist;

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.FeedProvider;



/**
 * Adapter for the list items in the {@link FeedList} activity.
 * 
 * @author mj
 */
public class FeedListAdapter extends SimpleCursorAdapter {

    /**
     * Reference to the date format used in the current context.
     */
    private final DateFormat dateFormat;
    /**
     * Reference to the time format used in the current context.
     */
    private final DateFormat timeFormat;


    /**
     * Creates a new adapter.
     * 
     * @param context
     *            the context
     * @param c
     *            the cursor to adapt the view to
     */
    public FeedListAdapter(Context context, Cursor c) {
        super(context, R.layout.feedlist_row, c, new String[] {
            "name",
        }, new int[] {
            R.id.feedlist_row_name
        });
        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.feedlist_row, null);
        return view;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final TextView nameView = (TextView) view.findViewById(R.id.feedlist_row_name);
        final String name = cursor.getString(cursor.getColumnIndex(FeedProvider.FEEDS_COL_NAME));
        if (name != null) {
            nameView.setText(name);
        }
        else {
            final String url = cursor.getString(cursor.getColumnIndex(FeedProvider.FEEDS_COL_URL));
            if (url != null) {
                nameView.setText(url);
            }
            else {
                nameView.setText(R.string.feedlist_label_new_feed);
            }
        }
        nameView.setTextAppearance(context, R.style.Base_FeedName);

        final TextView dateView = (TextView) view.findViewById(R.id.feedlist_row_date);
        long dateMs = cursor.getLong(cursor.getColumnIndex(FeedProvider.FEEDS_COL_LASTPOLLDATE));
        if (dateMs != 0) {
            final Date date = new Date(dateMs);
            if (date != null) {
                final String dateStr = dateFormat.format(date);
                final String timeStr = timeFormat.format(date);
                dateView.setText(dateStr + " - " + timeStr);
            }
        }
        else {
            dateView.setText(R.string.feedlist_label_new_feed);
        }
        dateView.setTextAppearance(context, R.style.Base_FeedInfo);

        final TextView numView = (TextView) view.findViewById(R.id.feedlist_row_num);
        final String num = cursor.getString(cursor.getColumnIndex(FeedProvider.FEEDS_COL_NUM));
        final String numUnread = cursor.getString(cursor.getColumnIndex(FeedProvider.FEEDS_COL_NUM_UNREAD));
        if (num != null) {
            numView.setText(numUnread + "/" + num);
            numView.setTextAppearance(context, R.style.Base_FeedInfo);
        }

        final ImageView statusView = (ImageView) view.findViewById(R.id.feedlist_row_status);
        if (!"0".equals(numUnread)) {
            statusView.setVisibility(ImageView.VISIBLE);
            statusView.setImageResource(R.drawable.unread);
            statusView.setScaleType(ScaleType.FIT_XY);
        }
        else {
            statusView.setVisibility(ImageView.INVISIBLE);
        }
    }

}
