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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import android.test.InstrumentationTestCase;



/**
 * Tests the Utils class.
 * 
 * @author mj
 */
public class UtilsTest extends InstrumentationTestCase {

    private interface Fixture {

        String getInput();


        String getExpectedOutput();
    }


    private class Fixture1 implements Fixture {

        public String getInput() {
            return "What&rsquo;s &#x3042;";
        }


        public String getExpectedOutput() {
            return "What’s あ";
        }
    }


    private class Fixture2 implements Fixture {

        public String getInput() {
            return "<p>&Uuml;berf&auml;lle</p>";
        }


        public String getExpectedOutput() {
            return "Überfälle";
        }
    }


    private List<Fixture> fixtures = new ArrayList<Fixture>();


    public void setUp() {
        fixtures.add(new Fixture1());
        fixtures.add(new Fixture2());
    }


    public void test_fixtures() {
        Assert.assertEquals(2, fixtures.size());
    }


    /**
     * If one of these tests fail you might have the wrong encoding set up for
     * your Eclipse workspace. Try UTF-8 and run the tests again.
     */
    public void test_htmlClean() {
        for (Fixture f : fixtures) {
            final String id = f.getClass().getSimpleName();
            final String in = f.getInput();
            final String expected = f.getExpectedOutput();
            final String output = Utils.htmlClean(in);
            Assert.assertEquals(id, expected, output);
        }
    }
}
