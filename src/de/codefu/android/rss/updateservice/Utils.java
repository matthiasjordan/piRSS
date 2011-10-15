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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.text.Html;



/**
 * Contains some utility methods.
 * 
 * @author mj
 */
public class Utils {

    /**
     * Makes a string out of an {@link InputStream}.
     * 
     * @param inStream
     *            the input stream
     * @param encoding
     *            the encoding of the input stream
     * @return the string that contains the data from the input stream
     * @throws IOException
     */
    public static String readStream(InputStream inStream, String encoding) throws IOException {
        final StringBuilder builder = new StringBuilder();
        int size = 1024;
        char[] buffer = new char[size];
        int len;
        final InputStreamReader isr = new InputStreamReader(inStream, encoding);
        while ((len = isr.read(buffer, 0, size)) > 0) {
            builder.append(buffer, 0, len);
        }

        return builder.toString();
    }


    /**
     * Removes everything from the given string that looks like an HTML tag.
     * 
     * @param c
     *            the string to clean
     * @return the cleaned string
     */
    public static String htmlClean(String c) {
        if (c == null) {
            return null;
        }
        final String tmp = c.replaceAll("<[\\s\\S]*?>", "").trim();
        final String out = Html.fromHtml(tmp).toString();
        return out;
    }

}
