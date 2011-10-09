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
package de.codefu.android.rss.feedprops;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Activity for editing the properties of an RSS feed.
 * 
 * @author mj
 */
public class FeedProps extends Activity {

    /**
     * Numerical values for the poll frequencies. These are the actual values
     * used. The array has to be synchronized with
     * {@link R.array.feedprops_autopoll_freqs}.
     */
    private static final int[] pollFreqMin = {
                    0, // never
                    1, // 1
                    5, // 2
                    10, // 3
                    15, // 4
                    20, // 5
                    30, // 6
                    1 * 60, // 7
                    2 * 60, // 8
                    4 * 60, // 9
                    8 * 60, // 9
                    1 * 24 * 60, // 10
                    3 * 24 * 60, // 11
                    7 * 24 * 60, // 12
    };

    /**
     * Array with the constants for the clean-up types for feed items. These
     * values have to be synchronizes with the values in
     * {@link R.array.feedprops_content_convert_types}.
     */
    private static final int[] cleanTypes = {
                    FeedProvider.CLEAN_RAW, //
                    FeedProvider.CLEAN_STRIP_HTML, //
                    FeedProvider.CLEAN_FULL_HTML, //
    };

    /**
     * Constant value to use when no feed ID is given. This should never happen,
     * though.
     */
    private static final int MISSING_FEED_ID = -1;
    /**
     * The name of the extra value in the incoming Intent that holds the ID of
     * the feed to create.
     */
    public static final String FEEDID = "feedid";
    /**
     * Reference to the name text field.
     */
    private TextView nameField;
    /**
     * Reference to the name URL field.
     */
    private TextView urlField;
    /**
     * Reference to the name auto poll field.
     */
    private Spinner autoPollField;
    /**
     * The number of minutes between automatically polling the RSS feed that is
     * being edited.
     */
    private int autoPollMin;
    /**
     * Reference to the Spinner that holds the possible options for content
     * conversion.
     */
    private Spinner contentConvertField;
    /**
     * The ID of the RSS feed to edit.
     */
    long feedId;


    @Override
    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        feedId = getIntent().getLongExtra(FEEDID, MISSING_FEED_ID);
        if (feedId == MISSING_FEED_ID) {
            finish();
        }

        setContentView(R.layout.feedprops);
        setTitle(R.string.feedprops_title);

        final Uri uri = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
        final Cursor c = getContentResolver().query(uri, null, null, null, null);
        if ((c == null) || !c.moveToFirst()) {
            finish();
        }

        final String name = c.getString(c.getColumnIndex(FeedProvider.FEEDS_COL_NAME));
        final String url = c.getString(c.getColumnIndex(FeedProvider.FEEDS_COL_URL));
        final int fautoPollMin = c.getInt(c.getColumnIndex(FeedProvider.FEEDS_COL_AUTOPOLLMIN));
        final int cleanHtml = c.getInt(c.getColumnIndex(FeedProvider.FEEDS_COL_CLEANHTML));

        c.close();

        nameField = (TextView) findViewById(R.id.feedprops_name);
        nameField.setText(name);

        urlField = (TextView) findViewById(R.id.feedprops_url);
        urlField.setText(url);

        autoPollField = (Spinner) findViewById(R.id.feedprops_autopoll_freq);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.feedprops_autopoll_freqs,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoPollField.setAdapter(adapter);
        autoPollField.setSelection(getValuePosition(pollFreqMin, fautoPollMin));
        autoPollMin = fautoPollMin;

        contentConvertField = (Spinner) findViewById(R.id.feedprops_content_convert);
        ArrayAdapter<CharSequence> adapterCC = ArrayAdapter.createFromResource(this,
                        R.array.feedprops_content_convert_types, android.R.layout.simple_spinner_item);
        adapterCC.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        contentConvertField.setAdapter(adapterCC);
        contentConvertField.setSelection(getValuePosition(cleanTypes, cleanHtml));

        final Button saveButton = (Button) findViewById(R.id.feedprops_button_save);
        saveButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                saveFeedProps();
                finish();
            }

        });
        final Button cancelButton = (Button) findViewById(R.id.feedprops_button_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                finish();
            }
        });
    }


    private int getValuePosition(int[] array, int value) {
        int i = 0;
        while (i < array.length) {
            if (array[i] == value) {
                return i;
            }
            i++;
        }
        return 0;
    }


    private void saveFeedProps() {
        ContentValues cv = new ContentValues();
        cv.put(FeedProvider.FEEDS_COL_NAME, nameField.getText().toString());
        cv.put(FeedProvider.FEEDS_COL_URL, urlField.getText().toString());
        cv.put(FeedProvider.FEEDS_COL_AUTOPOLLMIN, pollFreqMin[autoPollField.getSelectedItemPosition()]);
        cv.put(FeedProvider.FEEDS_COL_CLEANHTML, cleanTypes[contentConvertField.getSelectedItemPosition()]);

        Uri uri = ContentUris.withAppendedId(FeedProvider.CONTENT_URI, feedId);
        uri = uri.buildUpon().encodedFragment(FeedProvider.FORCE_NAME_FRAGMENT).build();
        getContentResolver().update(uri, cv, null, null);
        ServiceComm.sendDataChangedBroadcast(FeedProps.this);
        if (autoPollMin != cv.getAsInteger(FeedProvider.FEEDS_COL_AUTOPOLLMIN)) {
            ServiceComm.sendAutoPollIntent(this);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
