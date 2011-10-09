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
package de.codefu.android.rss.itemlist;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import de.codefu.android.rss.CursorChangedReceiver;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.db.ItemHelper;
import de.codefu.android.rss.db.ItemProvider;
import de.codefu.android.rss.item.ItemAct;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Activity that shows the list of items in a feed or the list of all feeds'
 * items.
 * 
 * @author mj
 */
public class ItemList extends ListActivity {

    /**
     * The name of the extra that holds the ID of the feed whose items to show.
     */
    public static final String FEED_ID = "feedid";
    /**
     * The cursor with the items to show.
     */
    private Cursor itemCursor;
    /**
     * The ID of the feed to show. A value of {@link FeedProvider#ALL_FEEDS}
     * means the items of all feeds are to be shown.
     */
    private long feedId;
    /**
     * Reference to the broadcast receiver that deals with updating the view if
     * the data is changed.
     */
    private CursorChangedReceiver receiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.itemlist);
        final Bundle extras = getIntent().getExtras();
        feedId = FeedProvider.ALL_FEEDS;
        if (extras != null) {
            feedId = extras.getLong(FEED_ID, FeedProvider.ALL_FEEDS);
        }

        final Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
        itemCursor = managedQuery(uri, null, null, null, null);

        ItemListAdapter a = new ItemListAdapter(this, itemCursor);
        setListAdapter(a);

        String feedName = null;
        if (feedId != FeedProvider.ALL_FEEDS) {
            Uri furi = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
            Cursor c = getContentResolver().query(furi, null, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    feedName = c.getString(c.getColumnIndex(FeedProvider.FEEDS_COL_NAME));
                }
                c.close();
            }
        }
        else {
            feedName = getText(R.string.itemlist_title_all_feeds).toString();
        }
        if (feedName != null) {
            setTitle(feedName);
        }

        receiver = new CursorChangedReceiver(itemCursor);
        registerReceiver(receiver, new IntentFilter(CursorChangedReceiver.DATA_CHANGED));

        registerForContextMenu(getListView());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final Intent i = new Intent(this, ItemAct.class);
        i.putExtra(ItemAct.ITEM_ID, id);
        i.putExtra(ItemAct.FEED_ID, feedId);
        startActivity(i);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, R.string.feedlist_menu_reload, 1, R.string.feedlist_menu_reload).setIcon(
                        R.drawable.ic_menu_refresh);
        menu.add(Menu.NONE, R.string.itemlist_menu_markallread, 1, R.string.itemlist_menu_markallread).setIcon(
                        R.drawable.ic_menu_view);
        menu.add(Menu.NONE, R.string.itemlist_menu_delread, 1, R.string.itemlist_menu_delread).setIcon(
                        R.drawable.ic_menu_delete);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.feedlist_menu_reload: {
                ServiceComm.sendPollIntent(this, feedId);
                break;
            }
            case R.string.itemlist_menu_markallread: {
                Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
                uri = uri.buildUpon().encodedFragment(ItemProvider.READ).build();
                getContentResolver().update(uri, null, null, null);
                ServiceComm.sendDataChangedBroadcast(this);
                break;
            }
            case R.string.itemlist_menu_delread: {
                Uri uri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
                uri = uri.buildUpon().encodedFragment(ItemProvider.READ).build();
                getContentResolver().delete(uri, null, null);
                ServiceComm.sendDataChangedBroadcast(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    // //////////////////////////////////////////////////////////////////////
    //
    // Context menu
    //
    //

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (menuInfo instanceof AdapterContextMenuInfo) {
            final AdapterContextMenuInfo ai = (AdapterContextMenuInfo) menuInfo;
            final Uri uri = ItemProvider.CONTENT_URI.buildUpon().appendPath(Long.toString(ai.id))
                            .encodedFragment("keeper").build();
            final Cursor res = managedQuery(uri, null, FEED_ID, null, FEED_ID);
            if (res.moveToFirst()) {
                final int keeper = res.getInt(res.getColumnIndex(ItemProvider.ITEMS_COL_KEEPER));
                if (keeper == 0) {
                    menu.add(Menu.NONE, R.string.itemlist_context_keeper, 0, R.string.itemlist_context_keeper);
                }
                else {
                    menu.add(Menu.NONE, R.string.itemlist_context_unkeep, 0, R.string.itemlist_context_unkeep);
                }
            }
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ContextMenuInfo info = item.getMenuInfo();
        if (info instanceof AdapterContextMenuInfo) {
            final AdapterContextMenuInfo ai = (AdapterContextMenuInfo) info;
            final long selectedItem = ai.id;
            final int menuItem = item.getItemId();
            switch (menuItem) {
                case R.string.itemlist_context_keeper: {
                    ItemHelper.setKeep(this, selectedItem, true);
                    return true;
                }
                case R.string.itemlist_context_unkeep: {
                    ItemHelper.setKeep(this, selectedItem, false);
                    return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }
}
