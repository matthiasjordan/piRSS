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

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Displays the main preferences screen.
 * 
 * @author mj
 */
public class MainPreferences extends PreferenceActivity {

    private boolean autoPoll;
    private String TAG = "MainPreferences";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mainprefs);
        autoPoll = getAutoPoll(this);
    }


    public static boolean getAutoPoll(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("mainprefs_autopoll", false);
    }


    @Override
    protected void onDestroy() {
        boolean newAutoPoll = getAutoPoll(this);
        if (newAutoPoll && !autoPoll) {
            Log.i(TAG, "Started auto poll service");
            ServiceComm.sendAutoPollIntent(this);
        }
        super.onDestroy();
    }
}
