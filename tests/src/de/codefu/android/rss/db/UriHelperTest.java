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

import junit.framework.Assert;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import de.codefu.android.rss.db.UriHelper.UriParts;



public class UriHelperTest extends InstrumentationTestCase {

    private static final String URI_PREFIX = "content://authority";


    public void test_0() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX));
        Assert.assertEquals("path", "[]", up.pathParts.toString());
        Assert.assertEquals("hasPath", false, up.hasPath());
        Assert.assertEquals("id", false, up.hasId());
    }


    public void test_00() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/"));
        Assert.assertEquals("path", "[]", up.pathParts.toString());
        Assert.assertEquals("hasPath", false, up.hasPath());
        Assert.assertEquals("id", false, up.hasId());
    }


    public void test_1() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/path"));
        Assert.assertEquals("path", "[path]", up.pathParts.toString());
        Assert.assertEquals("hasPath", true, up.hasPath());
        Assert.assertEquals("hasPath 1", true, up.hasPath("path"));
        Assert.assertEquals("id", false, up.hasId());
    }


    public void test_2() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/path/seg"));
        Assert.assertEquals("path", "[path, seg]", up.pathParts.toString());
        Assert.assertEquals("hasPath", true, up.hasPath());
        Assert.assertEquals("hasPath 1", true, up.hasPath("path", "seg"));
        Assert.assertEquals("id", false, up.hasId());
    }


    public void test_3() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/path/seg/foo"));
        Assert.assertEquals("path", "[path, seg, foo]", up.pathParts.toString());
        Assert.assertEquals("hasPath", true, up.hasPath());
        Assert.assertEquals("hasPath 1", true, up.hasPath("path", "seg", "foo"));
        Assert.assertEquals("id", false, up.hasId());
        Assert.assertEquals("hasId", false, up.hasId());
    }


    public void test_10() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/1"));
        Assert.assertEquals("path", "[]", up.pathParts.toString());
        Assert.assertEquals("hasPath", false, up.hasPath());
        Assert.assertEquals("id", 1, up.id);
        Assert.assertEquals("hasId", true, up.hasId());
    }


    public void test_10a() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/-1"));
        Assert.assertEquals("path", "[]", up.pathParts.toString());
        Assert.assertEquals("hasPath", false, up.hasPath());
        Assert.assertEquals("id", -1, up.id);
        Assert.assertEquals("hasId", true, up.hasId());
        
    }


    public void test_11() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/a/1"));
        Assert.assertEquals("path", "[a]", up.pathParts.toString());
        Assert.assertEquals("hasPath", true, up.hasPath());
        Assert.assertEquals("hasPath 1", true, up.hasPath("a"));
        Assert.assertEquals("id", 1, up.id);
        Assert.assertEquals("hasId", true, up.hasId());
    }


    public void test_12() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/a/b/1"));
        Assert.assertEquals("path", "[a, b]", up.pathParts.toString());
        Assert.assertEquals("hasPath", true, up.hasPath());
        Assert.assertEquals("hasPath 1", true, up.hasPath("a", "b"));
        Assert.assertEquals("id", 1, up.id);
        Assert.assertEquals("hasId", true, up.hasId());
    }


    public void test_19() {
        UriParts up = UriHelper.analyze(Uri.parse(URI_PREFIX + "/a/b/4454576"));
        Assert.assertEquals("path", "[a, b]", up.pathParts.toString());
        Assert.assertEquals("hasPath", true, up.hasPath());
        Assert.assertEquals("hasPath 1", true, up.hasPath("a", "b"));
        Assert.assertEquals("id", 4454576, up.id);
        Assert.assertEquals("hasId", true, up.hasId());
    }

}
