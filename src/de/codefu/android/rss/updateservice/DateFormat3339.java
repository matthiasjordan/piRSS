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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;



/**
 * Parser for the date format from RFC 3339.
 * 
 * @author mj
 */
class DateFormat3339 {

    private static final SimpleDateFormat basicf = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z", Locale.US);
    private static final SimpleDateFormat fancyf = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss z", Locale.US);
    private static final SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat timef = new SimpleDateFormat("HH:mm:ss");


    public static Date parse(String string, TimeZone tz) throws ParseException {
        Date out;
        try {
            out = parseRFC3339(string, tz);
        }
        catch (ParseException e) {
            out = parseXX(string);
        }
        return out;
    }


    // 2011-04-17T22:19:04+02:00
    public static Date parseRFC3339(String string, TimeZone tz) throws ParseException {
        string = string.toUpperCase();
        String[] parts = string.split("T");
        if (parts.length != 2) {
            parts = string.split(" ");
            if (parts.length != 2) {
                throw new ParseException("No date time sep found", 0);
            }
        }

        final String fullDate = parts[0];
        final String fullTime = parts[1];

        final Date date = datef.parse(fullDate);

        final String partialTime = fullTime.substring(0, 8);
        final Date time = timef.parse(partialTime);

        int timeOffsetPos = 8;
        if (fullTime.charAt(timeOffsetPos) == '.') {
            timeOffsetPos += 2;
        }

        final TimeZone timezone;
        final String timeOffset = fullTime.substring(timeOffsetPos);
        final char firstChar = timeOffset.charAt(0);
        if (firstChar == 'Z') {
            timezone = TimeZone.getTimeZone("UTC");
        }
        else {
            timezone = TimeZone.getTimeZone("GMT" + timeOffset);
        }

        GregorianCalendar c = new GregorianCalendar(timezone);
        c.set(date.getYear() + 1900, date.getMonth(), date.getDate(), time.getHours(), time.getMinutes(), time.getSeconds());

        return c.getTime();
    }


    // Mon, 06 Sep 2010 00:01:00 +0000
    // Sun, 17 Jul 2011 21:56:57 +0200
    public static Date parseXX(String string) throws ParseException {
        try {
            return basicf.parse(string);
        }
        catch (ParseException e) {
            string = string.replace("MEST", "+02:00");
            return fancyf.parse(string);
        }
    }
}
