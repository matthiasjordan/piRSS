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

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;



/**
 * A singleton that keeps a reference to the wake lock that keeps the device
 * active when a service is working.
 * 
 * @author mj
 */
class WakeLockHolder {

    /**
     * The number of references to the wake lock. Mainly used for logging
     * purposes.
     */
    private int references;


    /**
     * The holder.
     */
    private static class WakeLockHolderHolder {

        public static final WakeLockHolder HOLDER = new WakeLockHolder();
    }


    /**
     * @return a reference to the singleton
     */
    public static WakeLockHolder getInstance() {
        return WakeLockHolderHolder.HOLDER;
    }


    /**
     * The reference to the wake lock.
     */
    private static PowerManager.WakeLock wakeLock;


    private WakeLockHolder() {
        this.references = 0;
    }


    /**
     * Notifies the wake lock holder that the given context needs the wake lock.
     * 
     * @param context
     *            the context that needs the wake lock
     */
    public synchronized void acquire(Context context) {
        references++;
        getWakeLock(context).acquire();
        Log.i("WakeLockHolder", "now " + references + " - lock  acquired: " + context.getClass().toString());
    }


    /**
     * Notifies the wake lock holder that the given context does not need the
     * wake lock any longer.
     * 
     * @param context
     *            the context that releases the wake lock
     */
    public synchronized void release(Context context) {
        final PowerManager.WakeLock wakeLock = getWakeLock(context);
        if (wakeLock.isHeld()) {
            wakeLock.release();
            references--;
            Log.i("WakeLockHolder", "now " + references + " - lock freed: " + context.getClass().toString());
        }
    }


    /**
     * Actually acquires a new wake lock from the operating system.
     * 
     * @param context
     *            the context used to talk the the OS
     * @return the wake lock reference
     */

    private PowerManager.WakeLock getWakeLock(Context context) {
        if (wakeLock == null) {
            synchronized (WakeLockHolderHolder.HOLDER) {
                if (wakeLock == null) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getClass().toString());
                    wakeLock.setReferenceCounted(true);
                }
            }
        }
        return wakeLock;
    }
}
