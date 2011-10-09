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



/**
 * Defines how the {@link FeedHandler} interacts with its client.
 * 
 * @author mj
 */
interface FeedHandlerClient {

    /**
     * A whole news item in a cute small package.
     * 
     * @author mj
     */
    public static class Item {

        public String headline;
        public String content;
        public Date date;
        public String link;
        public String guid;
        public String feedName;

    }


    /**
     * Adds the item to the database.
     * 
     * @param feedId
     *            the id of the feed the item belongs to
     * @param cleanHtml
     *            if the item's data is to be cleaned
     * @param item
     *            the item to add
     */
    void addItem(long feedId, int cleanHtml, Item item);


    /**
     * Updates a feed's meta-information
     * 
     * @param feedId
     *            the id of the feed whose data to update
     * @param name
     *            the new name
     * @param description
     *            the new description
     * @param url
     *            the new URL
     */
    void updateFeed(long feedId, String name, String description, String url);
}
