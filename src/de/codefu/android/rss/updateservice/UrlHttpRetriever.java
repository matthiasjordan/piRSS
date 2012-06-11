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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;



/**
 * Downloads data.
 * 
 * @author mj
 */
class UrlHttpRetriever {

    private static final String DEFAULT_XML_TEXT_ENCODING = "UTF-8";

    private static final String ENCODING_PREFIX = "encoding=\"";


    /**
     * Retrieves a remote resource given by the URL.
     * 
     * @param urlStr
     *            the URL of the remote resource to download
     * @param lastPollDateMs
     *            last poll date in milliseconds (see
     *            {@link System#currentTimeMillis()})
     * @param timeoutMs
     *            the timeout in milliseconds after which to terminate the
     *            download attempt
     * @param maxTransferTimeMs
     *            how long a download may last in milliseconds
     * @return what could be downloaded at the given URL as a String, or null, if an error occurred
     */
    public String retrieveHttpContent(String urlStr, long lastPollDateMs, int timeoutMs, int maxTransferTimeMs) {

        URL url;
        try {
            url = new URL(urlStr);
        }
        catch (MalformedURLException e) {
            return null;
        }

        HttpURLConnection urlConnection = null;
        KillThread killThread = null;
        String result = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setUseCaches(true);
            urlConnection.setIfModifiedSince(lastPollDateMs);
            urlConnection.setConnectTimeout(timeoutMs);
            killThread = new KillThread(urlConnection, maxTransferTimeMs);
            killThread.start();
            final InputStream inputStream = urlConnection.getInputStream();
            final String encoding = getEncodingFromStream(inputStream);
            Log.i("UrlHR", "encoding " + encoding);
            result = Utils.readStream(inputStream, encoding);
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == 304) {
                // Not Modified
                return "";
            }
        }
        catch (IOException e) {
            return null;
        }
        finally {
            if (killThread != null) {
                killThread.interrupt();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }


    private String getEncodingFromStream(InputStream in) throws IOException {
        String line = readXmlTextDecl(in);
        return getEncodingFromTextDecl(line);
    }


    protected String getEncodingFromTextDecl(String line) {
        String encoding = DEFAULT_XML_TEXT_ENCODING;
        final int pos = line.indexOf(ENCODING_PREFIX);
        if (pos != -1) {
            String e = line.substring(pos + ENCODING_PREFIX.length());
            final int dqPos = e.indexOf('"');
            if (dqPos != -1) {
                e = e.substring(0, dqPos);
                encoding = e;
            }
        }
        return encoding;
    }


    private String readXmlTextDecl(InputStream in) throws IOException {
        StringBuilder firstLine = new StringBuilder();
        byte[] buffer = new byte[1];
        while (in.read(buffer) != -1) {
            final byte b = buffer[0];
            firstLine.append((char) b);
            if (buffer[0] == '>') {
                break;
            }
        }
        String line = firstLine.toString();
        return line;
    }


    /**
     * The thread that is used to kill a download after a timeout expires.
     */
    private class KillThread extends Thread {

        private HttpURLConnection conn;
        private int maxTransfertTimeMs;


        public KillThread(HttpURLConnection conn, int maxTransferTimeMs) {
            this.conn = conn;
            this.maxTransfertTimeMs = maxTransferTimeMs;
        }


        public void run() {
            try {
                Thread.sleep(maxTransfertTimeMs);
            }
            catch (InterruptedException e) {
                Log.i("UpdateService", "KillThread stopped");
                return;
            }
            if ((conn != null)) {
                Log.i("UpdateService", "Aborted download of " + conn.getURL());
                conn.disconnect();
            }
        }
    }
}
