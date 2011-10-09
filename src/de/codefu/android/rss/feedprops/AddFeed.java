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

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import de.codefu.android.rss.R;
import de.codefu.android.rss.db.FeedProvider;
import de.codefu.android.rss.updateservice.ServiceComm;



/**
 * Dialog shown when the user wants to add a new feed.
 * 
 * @author mj
 */
public class AddFeed extends Dialog {

    /**
     * The URL to display in the add dialog as a default.
     */
    private String presetUrl;


    /**
     * Creates a new dialog.
     * 
     * @param context
     *            the context
     * @param presetUrl
     *            the URL to display in the add dialog as a default - e.g. as
     *            obtained by a VIEW Intent.
     */
    public AddFeed(Context context, String presetUrl) {
        super(context);
        this.presetUrl = presetUrl;
    }


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.addfeed);
        setTitle(R.string.addfeed_title);
        final Button saveButton = (Button) findViewById(R.id.addfeed_button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                final EditText urlEditor = (EditText) findViewById(R.id.addfeed_url);
                final String rssUrl = urlEditor.getText().toString();
                Uri uri = FeedProvider.CONTENT_URI;
                ContentValues cv = new ContentValues();
                cv.put(FeedProvider.FEEDS_COL_URL, rssUrl);
                getContext().getContentResolver().insert(uri, cv);
                ServiceComm.sendDataChangedBroadcast(getContext());
                dismiss();
            }
        });
        final Button cancelButton = (Button) findViewById(R.id.addfeed_button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                cancel();
            }
        });
    }


    /**
     * Called to initialize the dialog. Sets the URL in the text field depending
     * on whether a {@link #presetUrl} is given.
     */
    public void onPrepareDialog() {
        final EditText urlEditor = (EditText) findViewById(R.id.addfeed_url);
        String url = getContext().getString(R.string.addfeed_url_template);
        if (presetUrl != null) {
            url = presetUrl;
        }
        urlEditor.setText(url);
    }

}
