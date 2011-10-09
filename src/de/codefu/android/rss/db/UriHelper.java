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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProvider;
import android.net.Uri;



/**
 * Helper that makes parsing URIs easier.
 * 
 * @author mj
 */
class UriHelper {

    /**
     * The parts of the URI that are interesting for {@link ContentProvider}
     * implementations.
     * 
     * @author mj
     */
    public static class UriParts {

        /**
         * Used if no ID is given.
         */
        private static final long NO_ID = Long.MIN_VALUE;
        /**
         * The parts of the path.
         */
        public List<String> pathParts;
        /**
         * The ID.
         */
        public long id = NO_ID;
        /**
         * The fragment found.
         */
        public String fragment;


        /**
         * @return true, if the URI has an ID part (e.g. the "11" in
         *         "foo://..../11"). Else false.
         */
        public boolean hasId() {
            return (id != NO_ID);
        }


        /**
         * @return true, if the URI has a path part (e.g. the "bar/baz" in
         *         "foo://../bar/baz/..."). Else false.
         */
        public boolean hasPath() {
            return ((pathParts != null) && !pathParts.isEmpty());
        }


        /**
         * @param path
         *            a path, given as the list of individual parts of the path
         * @return true, if the path is the same (same length, same parts) as
         *         the one in the URI. Else false.
         */
        public boolean hasPath(String... path) {
            if ((pathParts == null) || pathParts.isEmpty()) {
                return false;
            }
            if (path.length != pathParts.size()) {
                return false;
            }

            int i;
            for (i = 0; (i < path.length); i++) {
                if (!path[i].equals(pathParts.get(i))) {
                    break;
                }
            }
            return i == path.length;
        }


        /**
         * @param frag
         *            the fragment whose existence to test for
         * @return true if the given fragment is set in the URI. Else false.
         */
        public boolean hasFragment(String frag) {
            return ((fragment != null) && (fragment.length() != 0) && (frag != null) && frag.equals(fragment));
        }
    }


    /**
     * Analyzes the given URI.
     * 
     * @param uri
     *            the URI to analyze
     * @return the parts of the URI
     */
    public static UriParts analyze(Uri uri) {
        final UriParts up = new UriParts();
        up.pathParts = new ArrayList<String>(uri.getPathSegments());

        try {
            final String lps = uri.getLastPathSegment();
            if (lps != null) {
                try {
                    up.id = Long.parseLong(lps);
                }
                catch (NumberFormatException e) {
                }
            }
            if (up.hasId()) {
                up.pathParts.remove(up.pathParts.size() - 1);
            }
        }
        catch (NumberFormatException e) {
            up.id = UriParts.NO_ID;
        }
        catch (UnsupportedOperationException e) {
            return null;
        }

        up.fragment = uri.getFragment();
        return up;
    }
}
