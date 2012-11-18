/*
    Copyright (C) 2012 by GENYMOBILE and Frederic-Charles Barthelery
    fbarthelery@genymobile.com
    http://www.genymobile.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package com.genymobile.demo.p2pshare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class NsdHelper {

    private static final String TAG = NsdHelper.class.getSimpleName();
    private NsdManager nsdMgr;
    private MyDiscoveryListener discoveryListener;
    private boolean discoveryStarted;
    private Set<String> solvedServiceName = Collections.synchronizedSet(new HashSet<String>());
    private List<NsdServiceInfo> serviceFound = new ArrayList<NsdServiceInfo>();
    private List<NsdListener> listeners = new ArrayList<NsdHelper.NsdListener>();
    private boolean isDiscoverRunning;

    public NsdHelper(Context ctx) {
	nsdMgr = (NsdManager) ctx.getSystemService(Context.NSD_SERVICE);
	discoveryListener = new MyDiscoveryListener();
    }

    public void startDiscovery() {
	if (!discoveryStarted && !isDiscoverRunning) {
	    Log.d(TAG, "Start discovery");
	    nsdMgr.discoverServices("_p2pShare._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
	    isDiscoverRunning = true;
	}
    }

    public boolean isDiscoveryStarted() {
	return discoveryStarted;
    }

    public void stopDiscovery() {
	if (discoveryStarted) {
	    nsdMgr.stopServiceDiscovery(discoveryListener);
	    discoveryStarted = false;
	}
    }

    // the service name change after resolution
    public void resolveService(NsdServiceInfo service, NsdManager.ResolveListener listener) {
	nsdMgr.resolveService(service, listener);
    }

    private String getNsdError(int code) {
	if (code == NsdManager.FAILURE_ALREADY_ACTIVE) {
	    return "NSD Already active";
	} else if (code == NsdManager.FAILURE_INTERNAL_ERROR) {
	    return "NSD Internal error";
	} else if (code == NsdManager.FAILURE_MAX_LIMIT) {
	    return "NSD Max request limit";
	} else
	    return "NSD Unknown error";
    }

    private class MyDiscoveryListener implements DiscoveryListener {

	@Override
	public void onStopDiscoveryFailed(String arg0, int arg1) {
	    Log.d(TAG, "Failed to stop discovery for " + arg0 + " error " + getNsdError(arg1));
	    // nsdMgr.stopServiceDiscovery(this);
	}

	@Override
	public void onStartDiscoveryFailed(String arg0, int arg1) {
	    Log.d(TAG, "Failed to start discovery for " + arg0 + " error " + getNsdError(arg1));
	    // NO NEED ?
	    //			nsdMgr.stopServiceDiscovery(this);
	    discoveryStarted = false;
	    isDiscoverRunning = false;
	}

	@Override
	public void onServiceLost(NsdServiceInfo service) {
	    Log.d(TAG, "Service lost " + service);
	    solvedServiceName.remove(service.getServiceName());
	    serviceFound.remove(service);
	    fireServiceLost(service);
	}

	@Override
	public void onServiceFound(NsdServiceInfo service) {
	    Log.d(TAG, "service found " + service.getServiceName());
	    serviceFound.add(service);
	    fireServiceFound(service);
	}

	@Override
	public void onDiscoveryStopped(String arg0) {
	    Log.d(TAG, "Stop discovery for " + arg0);
	    discoveryStarted = false;
	    isDiscoverRunning = false;

	}

	@Override
	public void onDiscoveryStarted(String arg0) {
	    Log.d(TAG, "Start discovery for " + arg0);
	    discoveryStarted = true;
	}
    }

    public void addNsdListener(NsdListener listener) {
	listeners.add(listener);
    }

    public void removeNsdListener(NsdListener listener) {
	listeners.remove(listener);
    }

    private void fireServiceFound(NsdServiceInfo info) {
	for (NsdListener l : listeners) {
	    l.onServiceFound(info);
	}
    }

    private void fireServiceLost(NsdServiceInfo info) {
	for (NsdListener l : listeners) {
	    l.onServiceLost(info);
	}
    }

    interface NsdListener {
	void onServiceFound(NsdServiceInfo info);

	void onServiceLost(NsdServiceInfo info);
    }

}
