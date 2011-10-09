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
import android.database.Cursor;
import android.widget.CursorAdapter;



/**
 * Handler for the {@link DB#DATA_CHANGED} intent.
 * <p>
 * The intent signals that the database content has changed so cursors used in
 * {@link CursorAdapter} objects can be refreshed.
 * 
 * @author mj
 */
public class CursorChangedReceiver extends BroadcastReceiver {

    public static final String PACKAGE_NAME = CursorChangedReceiver.class.getPackage().getName();

    public static final String DATA_CHANGED = PACKAGE_NAME + ".action.datachanged";

    /**
     * Reference to the cursor that should be refreshed when the intent is
     * received.
     */
    private Cursor cursor;


    /**
     * Creates a new instance.
     * 
     * @param cursor
     *            the cursor that should be refreshed when the intent is
     *            received
     */
    public CursorChangedReceiver(Cursor cursor) {
        this.cursor = cursor;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (DATA_CHANGED.equals(intent.getAction())) {
            cursor.requery();
        }
    }
};
