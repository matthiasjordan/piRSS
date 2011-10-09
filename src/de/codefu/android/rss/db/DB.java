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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;



/**
 * Basic database access.
 * 
 * @author mj
 */
class DB extends SQLiteOpenHelper {

    /**
     * The package name. Used for intents.
     */
    public static final String PACKAGE_NAME = DB.class.getPackage().getName();
    /**
     * The intent name for the "data has changed" intent.
     */
    public static final String DATA_CHANGED = PACKAGE_NAME + "action.datachanged";
    /**
     * The database name.
     */
    private static final String DB_NAME = "rssdb";
    /**
     * The version of the database schema. Increase if the schema has changed
     * and an upgrade step has to be triggered.
     */
    private static final int DB_VERSION = 3;
    /**
     * Reference to the context.
     */
    private Context context;
    /**
     * Reference to the singleton instance.
     */
    private static volatile DB INSTANCE = null;


    private DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }


    /**
     * Returns the singleton's instance.
     * 
     * @param context
     *            the context - only needed at the first call
     * @return the instance
     */
    public static synchronized DB getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DB(context);
        }
        return INSTANCE;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FeedProvider.getCreateTable());
        db.execSQL(ItemProvider.getCreateTable());
        db.execSQL(ItemProvider.getCreateTmpTable());
        db.execSQL(ItemProvider.getCreateAuxTable());
        FeedProvider.addDefaultFeed(context, db);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if ((oldVersion == 2) && (newVersion == 3)) {
            db.execSQL("alter table " + ItemProvider.ITEMS_NAME + " add column " + ItemProvider.ITEMS_COL_KEEPER
                            + " INTEGER");
        }
    }

}
