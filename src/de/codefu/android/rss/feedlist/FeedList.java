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

import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import de.codefu.android.rss.MainPreferences;
import de.codefu.android.rss.R;
import de.codefu.android.rss.UserNotification;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.db.ItemProvider;
import de.codefu.android.rss.feedprops.AddFeed;
import de.codefu.android.rss.feedprops.FeedProps;
import de.codefu.android.rss.itemlist.ItemList;
import de.codefu.android.rss.updateservice.ServiceComm;
import de.codefu.android.rss.updateservice.UpdateService;



/**
 * The activity that shows the list of feeds along with last poll time and
 * unread/total items.
 * 
 * @author mj
 */
public class FeedList extends ListActivity {

    /**
     * Dialog ID for the {@link AddFeed} dialog.
     */
    private static final int DIALOG_FEED_ADD = 2;

    /**
     * The cursor that contains the feed items to show in the list view.
     */
    private Cursor feedsCursor;
    /**
     * Reference to the object that handles DB changes for later freeing.
     */
    private CursorChangedReceiver cursorChangedReceiver;
    /**
     * Reference to the object that handles polling problem broadcasts for later
     * freeing.
     */
    private BroadcastReceiver pollingProblemReceiver;


    // //////////////////////////////////////////////////////////////////////
    //
    // Activity life cycle
    //
    //

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.feedlist);

        feedsCursor = managedQuery(FeedProvider.FEEDS_FANCY_URI, null, null, null, null);
        final FeedListAdapter a = new FeedListAdapter(getApplicationContext(), feedsCursor);
        setListAdapter(a);

        cursorChangedReceiver = new CursorChangedReceiver(feedsCursor);
        registerReceiver(cursorChangedReceiver, new IntentFilter(CursorChangedReceiver.DATA_CHANGED));
        pollingProblemReceiver = UserNotification.registerPollingProblemReceiver(this);
        registerForContextMenu(getListView());

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            final Set<String> cats = getIntent().getCategories();
            if ((cats != null) && cats.contains(Intent.CATEGORY_BROWSABLE)) {
                showDialog(DIALOG_FEED_ADD);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cursorChangedReceiver);
        unregisterReceiver(pollingProblemReceiver);
    }


    // //////////////////////////////////////////////////////////////////////
    //
    // Dialogs
    //
    //

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case DIALOG_FEED_ADD: {
                Uri intentUri = getIntent().getData();
                String url = null;
                if (intentUri != null) {
                    url = intentUri.toString();
                }

                dialog = new AddFeed(this, url);
                break;
            }
            default: {
                dialog = null;
            }
        }
        return dialog;
    }


    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
            case DIALOG_FEED_ADD: {
                ((AddFeed) dialog).onPrepareDialog();
                break;
            }
        }
    }


    // //////////////////////////////////////////////////////////////////////
    //
    // Menu
    //
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, R.string.feedlist_menu_add, 99, R.string.feedlist_menu_add).setIcon(
                        R.drawable.ic_menu_add);
        menu.add(Menu.NONE, R.string.feedlist_menu_reload, 1, R.string.feedlist_menu_reload).setIcon(
                        R.drawable.ic_menu_refresh);
        menu.add(Menu.NONE, R.string.feedlist_menu_show_all, 4, R.string.feedlist_menu_show_all).setIcon(
                        R.drawable.ic_menu_view);
        menu.add(Menu.NONE, R.string.feedlist_menu_prefs, 1000, R.string.feedlist_menu_prefs).setIcon(
                        R.drawable.ic_menu_preferences);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.feedlist_menu_add: {
                showDialog(DIALOG_FEED_ADD);
                break;
            }
            case R.string.feedlist_menu_reload: {
                final Intent i = new Intent(this, UpdateService.class);
                startService(i);
                break;
            }
            case R.string.feedlist_menu_show_all: {
                final Intent i = new Intent(this, ItemList.class);
                i.putExtra(ItemList.FEED_ID, FeedProvider.ALL_FEEDS);
                startActivity(i);
                break;
            }
            case R.string.feedlist_menu_prefs: {
                final Intent i = new Intent(this, MainPreferences.class);
                startActivity(i);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    // //////////////////////////////////////////////////////////////////////
    //
    // Items
    //
    //

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        final Intent i = new Intent(this, ItemList.class);
        i.putExtra(ItemList.FEED_ID, id);
        startActivity(i);
    }


    // //////////////////////////////////////////////////////////////////////
    //
    // Context menu
    //
    //

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, R.string.feedlist_context_edit, 0, R.string.feedlist_context_edit);
        menu.add(Menu.NONE, R.string.feedlist_context_deletefeed, 0, R.string.feedlist_context_deletefeed);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ContextMenuInfo info = item.getMenuInfo();
        if (info instanceof AdapterContextMenuInfo) {
            final AdapterContextMenuInfo ai = (AdapterContextMenuInfo) info;
            final long selectedFeed = ai.id;
            final int menuItem = item.getItemId();
            switch (menuItem) {
                case R.string.feedlist_context_edit: {
                    final Intent i = new Intent(this, FeedProps.class);
                    i.putExtra(FeedProps.FEEDID, selectedFeed);
                    startActivity(i);
                    return true;
                }
                case R.string.feedlist_context_deletefeed: {
                    deleteFeedAfterConfirm(selectedFeed);
                    return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }


    /**
     * Shows an {@link AlertDialog} with an OK/Cancel choice to make sure the
     * user only deletes a feed she really doesn't like anymore.
     * 
     * @param feedId
     *            the ID of the feed that might get deleted
     */
    private void deleteFeedAfterConfirm(final long feedId) {
        final AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setCancelable(false);
        ad.setMessage(getText(R.string.deletefeed_question));

        ad.setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.button_cancel), new OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                ad.dismiss();
            }
        });

        ad.setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.button_ok), new OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                Uri furi = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
                getContentResolver().delete(furi, null, null);

                Uri iuri = ContentUris.withAppendedId(ItemProvider.CONTENT_URI_FEED, feedId);
                getContentResolver().delete(iuri, null, null);

                ServiceComm.sendDataChangedBroadcast(getApplicationContext());
            }
        });
        ad.show();
    }
}