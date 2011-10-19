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
package de.codefu.android.rss.item;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.db.ItemHelper;
import de.codefu.android.rss.db.ItemProvider;



/**
 * Activity that shows one single news item.
 * 
 * @author mj
 */
public class ItemAct extends Activity {

    /**
     * The name of the extra that keeps the ID of the item to show.
     */
    public static final String ITEM_ID = "itemid";
    /**
     * The name of the extra that keeps the ID of the feed, the item to show is
     * from.
     */
    public static final String FEED_ID = "feedid";
    /**
     * The ID of the feed, the item to show is from.
     */
    private long feedId;
    /**
     * The ID of the item to show.
     */
    private long id;
    /**
     * The cursor that holds the item's data.
     */
    private Cursor itemC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item);

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        feedId = extras.getLong(FEED_ID);
        id = extras.getLong(ITEM_ID);
    }


    @Override
    protected void onResume() {
        super.onResume();

        final Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
        itemC = managedQuery(uri, null, null, null, null);

        moveCursorToItem();

        showItem();
    }


    private void moveCursorToItem() {
        if (itemC.moveToFirst()) {

            final int idAttrPos = itemC.getColumnIndex("_id");

            if (idAttrPos == -1) {
                Log.e("ItemAct", "Tried to show item " + id + " from feed " + feedId + " but id column is unknown.");
                finish();
                return;
            }

            while ((itemC.getLong(idAttrPos) != id) && (!itemC.isLast())) {
                itemC.moveToNext();
            }

            if (itemC.getLong(idAttrPos) != id) {
                Log.e("ItemAct", "Tried to show item " + id + " from feed " + feedId + " but item is unknown.");
                finish();
                return;
            }
        }
        else {
            Log.e("ItemAct", "Cursor empty.");
            finish();
            return;
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(FEED_ID, feedId);
        outState.putLong(ITEM_ID, id);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        feedId = savedInstanceState.getLong(FEED_ID);
        id = savedInstanceState.getLong(ITEM_ID);
    }


    private void setButtonVisibility(final boolean show, final Button button) {
        if (!show) {
            button.setVisibility(Button.INVISIBLE);
        }
        else {
            button.setVisibility(Button.VISIBLE);
        }
    }


    private void showItem() throws IllegalArgumentException {
        final String h;
        final String c;

        id = itemC.getLong(itemC.getColumnIndex("_id"));
        final String feedName = itemC.getString(itemC.getColumnIndex(FeedProvider.FEEDS_COL_NAME));
        final String headline = itemC.getString(itemC.getColumnIndex(ItemProvider.ITEMS_COL_HEADLINE));
        final String content = itemC.getString(itemC.getColumnIndex(ItemProvider.ITEMS_COL_CONTENT));
        final String link = itemC.getString(itemC.getColumnIndex(ItemProvider.ITEMS_COL_LINK));
        final long dateline = itemC.getLong(itemC.getColumnIndex(ItemProvider.ITEMS_COL_DATE));
        final Date date = new Date(dateline);

        if (headline != null) {
            if (content != null) {
                h = headline;
                c = content;
            }
            else {
                h = null;
                c = headline;
            }
        }
        else {
            if (content != null) {
                h = null;
                c = content;
            }
            else {
                h = null;
                c = null;
            }
        }

        if (feedName != null) {
            setTitle(feedName);
        }

        final ScrollView scrollView = (ScrollView) findViewById(R.id.item_scrollview);
        scrollView.scrollTo(0, 0);

        final TextView headlineView = (TextView) findViewById(R.id.item_headline);
        if (h != null) {
            headlineView.setMaxLines(20);
            headlineView.setText(h);
            headlineView.setTextAppearance(this, R.style.Base_ItemHeadline);
        }
        else {
            headlineView.setHeight(0);
        }

        DateFormat datef = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        DateFormat timef = android.text.format.DateFormat.getTimeFormat(getApplicationContext());

        final TextView dateView = (TextView) findViewById(R.id.item_dateline);
        if (dateline != 0) {
            final String fd = datef.format(date) + " - " + timef.format(date);
            dateView.setMaxLines(1);
            dateView.setText(fd);
            dateView.setTextAppearance(this, R.style.Base_ItemDate);
        }
        else {
            dateView.setHeight(0);
        }

        final WebView webView = (WebView) findViewById(R.id.item_content_web);
        final TextView contentView = (TextView) findViewById(R.id.item_content);
        final LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayout01);

        final int htmlMode = itemC.getInt(itemC.getColumnIndex(FeedProvider.FEEDS_COL_CLEANHTML));
        if (htmlMode == FeedProvider.CLEAN_FULL_HTML) {
            if (c != null) {
                ll.removeView(contentView);
                webView.loadDataWithBaseURL(link, c, "text/html", "utf-8", null);
                webView.setNetworkAvailable(true);
            }
        }
        else {
            if (c != null) {
                ll.removeView(webView);
                contentView.setText(insertLineFeeds(c));
                contentView.setTextAppearance(this, R.style.Base_ItemBody);
            }
        }

        final Button browserButton = (Button) findViewById(R.id.item_button_browser);
        if (link != null) {
            final Uri itemUri = Uri.parse(link);
            if ((itemUri != null) && itemUri.isAbsolute()) {

                browserButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        final Intent i = new Intent(Intent.ACTION_VIEW, itemUri);
                        try {
                            startActivity(i);
                        }
                        catch (ActivityNotFoundException e) {
                            Log.i("ItemAct", "URL could not be handled - bummer.");
                        }
                    }
                });
            }
        }
        else {
            browserButton.setVisibility(Button.INVISIBLE);
        }

        final Button prevButton = (Button) findViewById(R.id.item_button_previous);
        prevButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                itemC.moveToNext();
                showItem();
            }
        });
        setButtonVisibility(!itemC.isLast(), prevButton);

        final Button nextButton = (Button) findViewById(R.id.item_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                itemC.moveToPrevious();
                showItem();
            }
        });
        setButtonVisibility(!itemC.isFirst(), nextButton);

        Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI, id);
        uri = uri.buildUpon().encodedFragment(ItemProvider.READ).build();
        getContentResolver().update(uri, null, null, null);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     * Supposed to fix an issue with TextView that only renders lines shorter
     * than 4705 characters correctly.
     * 
     * @param in
     *            the string to handle
     * @return the string with inserted line feeds
     */
    private String insertLineFeeds(String in) {
        final int everyNChars = 3500;
        final StringBuilder out = new StringBuilder();
        final int len = in.length();
        int start = 0;
        int pos = everyNChars;
        while ((pos = in.indexOf(' ', pos)) != -1) {
            out.append(in.substring(start, pos).trim());
            out.append('\n');
            start = pos + 1;
            pos += everyNChars;
        }
        out.append(in.substring(start, len).trim());

        return out.toString();
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.removeGroup(Menu.NONE);
        final Uri uri = ItemProvider.CONTENT_URI.buildUpon().appendPath(Long.toString(id)).encodedFragment("keeper")
                        .build();
        final Cursor res = getContentResolver().query(uri, null, FEED_ID, null, FEED_ID);
        if (res != null) {
            if (res.moveToFirst()) {
                int keeper = res.getInt(res.getColumnIndex(ItemProvider.ITEMS_COL_KEEPER));
                if (keeper == 0) {
                    menu.add(Menu.NONE, R.string.item_menu_keeper, 1, R.string.item_menu_keeper).setIcon(
                                    R.drawable.ic_menu_save);
                }
                else {
                    menu.add(Menu.NONE, R.string.item_menu_unkeep, 1, R.string.item_menu_unkeep).setIcon(
                                    R.drawable.ic_menu_revert);
                }
            }
            res.close();
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final long selectedItem = id;
        final int menuItem = item.getItemId();
        switch (menuItem) {
            case R.string.item_menu_keeper: {
                ItemHelper.setKeep(this, selectedItem, true);
                return true;
            }
            case R.string.item_menu_unkeep: {
                ItemHelper.setKeep(this, selectedItem, false);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
