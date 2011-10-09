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
package de.codefu.android.rss.db;

import android.content.Context;
import android.net.Uri;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Helper class for operations on items.
 * 
 * @author mj
 */
public class ItemHelper {

    /**
     * Sets the "keep this" attribute of a news item given by its ID.
     * 
     * @param context
     *            the context for sending the broadcast event
     * @param selectedItem
     *            the ID of the news item in the database
     * @param keep
     *            true, if the item should be kept (not deleted during a purge),
     *            or false, if not.
     */
    public static void setKeep(Context context, long selectedItem, boolean keep) {
        final Uri uri = ItemProvider.CONTENT_URI.buildUpon().appendPath(Long.toString(selectedItem))
                        .encodedFragment(keep ? ItemProvider.KEEP : ItemProvider.UNKEEP).build();
        context.getContentResolver().update(uri, null, null, null);
        ServiceComm.sendDataChangedBroadcast(context);
    }

}
