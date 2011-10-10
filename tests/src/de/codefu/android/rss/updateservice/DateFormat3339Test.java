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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.Assert;
import android.test.InstrumentationTestCase;
import android.util.Log;



public class DateFormat3339Test extends InstrumentationTestCase {

    public void test_0() {
        GregorianCalendar gc = new GregorianCalendar();
        TimeZone tz = gc.getTimeZone();
        Log.i("##", tz.getDisplayName());
        gc.set(2011, 9, 10, 23, 19);
        Log.i("##", "time: " + gc.getTime());
        gc.setTimeZone(TimeZone.getTimeZone("UTC"));
        Log.i("##", "time in UTC: " + gc.getTime());
        gc.setTimeZone(tz);
        Log.i("##", "time in local time: " + gc.getTime());
    }


    public void test_1() {
        doTheThing("2011-04-01T22:19:05+02:00", 2011, GregorianCalendar.APRIL, 1, 22, 19, 5, 120);
    }


    public void test_2() {
        doTheThing("2011-04-17T22:19:05+02:00", 2011, GregorianCalendar.APRIL, 17, 22, 19, 5, 120);
    }


    public void test_3() {
        doTheThing("2012-04-17T22:19:05+02:00", 2012, GregorianCalendar.APRIL, 17, 22, 19, 5, 120);
    }


    public void test_4a() {
        doTheThing("2012-03-17T22:19:05Z", 2012, GregorianCalendar.MARCH, 17, 23, 19, 5, 60);
    }


    public void test_4b() {
        doTheThing("2012-03-17T22:19:05+01:00", 2012, GregorianCalendar.MARCH, 17, 22, 19, 5, 60);
    }


    public void test_4c() {
        doTheThing("2012-03-17T22:19:05Z", 2012, GregorianCalendar.MARCH, 17, 23, 19, 5, 60);
    }


    public void test_5a() {
        doTheThing("2012-01-17T23:59:59-02:00", 2012, GregorianCalendar.JANUARY, 18, 2, 59, 59, 60);
    }


    public void test_5b() {
        doTheThing("2012-01-17T23:59:59-01:00", 2012, GregorianCalendar.JANUARY, 18, 1, 59, 59, 60);
    }


    public void test_5c() {
        doTheThing("2012-01-17T23:59:59+01:00", 2012, GregorianCalendar.JANUARY, 17, 23, 59, 59, 60);
    }


    public void test_5d() {
        doTheThing("2012-01-17T23:59:59+02:00", 2012, GregorianCalendar.JANUARY, 17, 22, 59, 59, 60);
    }


    public void test_6() {
        doTheThing("Sun, 17 Jul 2011 16:50:25 MEST", 2011, GregorianCalendar.JULY, 17, 16, 50, 25, 120);
    }


    public void test_7() {
        doTheThing("Sun, 17 Jul 2011 21:56:57 +0200", 2011, GregorianCalendar.JULY, 17, 21, 56, 57, 120);
    }


    private void doTheThing(String dateStr, int year, int mon, int day, int h, int m, int s, int offs) {
        Date ex = new Date();
        try {
            final TimeZone tz = TimeZone.getTimeZone("PST");
            ex = DateFormat3339.parse(dateStr, tz);
            Log.i("tztest", dateStr + " -> " + ex);
        }
        catch (ParseException e) {
            Assert.fail(e.getMessage());
        }

        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("CET"));
        c.clear();
        c.setTime(ex);
        Assert.assertEquals("year", year, c.get(Calendar.YEAR));
        Assert.assertEquals("month", mon, c.get(Calendar.MONTH));
        Assert.assertEquals("day", day, c.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals("hour", h, c.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals("minute", m, c.get(Calendar.MINUTE));
        Assert.assertEquals("sec", s, c.get(Calendar.SECOND));
        Assert.assertEquals("offset", offs, (c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 60000);
    }
}
