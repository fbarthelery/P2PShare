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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import elonen.nanohttpd.NanoHTTPD;


public class ShareService extends Service {
	
	public static final String SHARE_SERVER_CHANGE_ACTION = "share_server_change";
	public static final String EXTRA_SHARE_SERVER_PORT = "share_server_port";
	public static final String EXTRA_SHARE_SERVER_NAME = "share_server_name";
	private final static String TAG = "ShareService";
	
	private NsdManager nsdMgr; 
	private MyRegistrationListener registrationListener;
	private ShareServer server;
	private File shareDir;
	private BroadcastReceiver externalStorageReceiver;
	private boolean externalStorageAvailable = false;
	private boolean shareStarted;
	private String myServiceName = "P2PShare";
	private boolean isRegistered;
	public static final String SERVICE_TYPE = "_p2pShare._tcp";

	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// nasty but we do can only get it
	// via the WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION broadcast intent
	// which is broadcast asynchronously
    private String getPersistedDeviceName() {

    	String deviceName = Settings.Secure.getString(getContentResolver(),
                "wifi_p2p_device_name");
        if (deviceName == null) {
            /* We use the 4 digits of the ANDROID_ID to have a friendly
             * default that has low likelihood of collision with a peer */
            String id = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            return "Android_" + id.substring(0,4);
        }    
        return deviceName;
    }    

	
	
	@Override
	public void onCreate() {
		super.onCreate();
		shareDir = new File(Environment.getExternalStorageDirectory(), "P2PShare");
		updateExternalStorageState();
		startWatchingExternalStorage();
		nsdMgr = (NsdManager) getSystemService(Context.NSD_SERVICE);
		registrationListener = new MyRegistrationListener();
		myServiceName = getPersistedDeviceName();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startSharingServer();
		registerService(server.getLocalPort());
		sendBroadcastStatus();
		shareStarted = true;
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Stopping ShareService");
		stopWatchingExternalStorage();
		stopSharingServer();
	}
	

	
	private void startSharingServer() {
		if (server == null) {
			try {
				if (!shareDir.exists())
					shareDir.mkdirs();
				server = new ShareServer(shareDir, "/list");
				Log.i(TAG, "Start sharing server on port " + server.getLocalPort());
			} catch (IOException e) {
				Log.e(TAG, "Unable to start P2PShare server");
			}
		}
	}
	
	private void stopSharingServer() {
		if (server != null) {
			server.stop();
			server = null;
			Log.i(TAG, "Stop P2PShare server");
			unregisterService();
		}
	}
	
	private void registerService(int port) {
		if (isRegistered)
			return;
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setPort(port);
		serviceInfo.setServiceName(myServiceName);
		serviceInfo.setServiceType(SERVICE_TYPE);
		nsdMgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
				registrationListener);
	}
	
	private void unregisterService() {
		if (isRegistered) {
			Log.i(TAG, "unregister ShareService");
			nsdMgr.unregisterService(registrationListener);
		}
	}

	void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			externalStorageAvailable = true;
		} else {
			externalStorageAvailable = false;
		}
		if (shareStarted && !externalStorageAvailable) {
			stopSelf();
		} else if (shareStarted) {
			// start server
			startSharingServer();
		}
	}

	void startWatchingExternalStorage() {
		externalStorageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateExternalStorageState();
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(externalStorageReceiver, filter);
		updateExternalStorageState();
	}

	void stopWatchingExternalStorage() {
		unregisterReceiver(externalStorageReceiver);
	}
	
	class MyRegistrationListener implements NsdManager.RegistrationListener {

		@Override
		public void onUnregistrationFailed(NsdServiceInfo arg0, int arg1) {
			Log.d(TAG, "Failed to unregister service " + arg0 + " error "
					+ arg1);
		}

		@Override
		public void onServiceUnregistered(NsdServiceInfo arg0) {
			Log.d(TAG, "Service unregistered " + arg0);

		}

		@Override
		public void onServiceRegistered(NsdServiceInfo arg0) {
			Log.d(TAG, "Service registered " + arg0);
			myServiceName = arg0.getServiceName();
			isRegistered = true;
			sendBroadcastStatus();
		}

		@Override
		public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
			Log.d(TAG, "Failed to register service " + arg0 + " error " + arg1);
		}
	}

	class ShareServer extends NanoHTTPD {
		private String listResource;
		private File rootDir;
		
		public ShareServer(File wwwroot, String listResource) throws IOException {
			super(0, wwwroot);
			rootDir = wwwroot;
			this.listResource = listResource;
		}
		
		public int getLocalPort() {
			return myServerSocket.getLocalPort();
		}

		@Override
		public Response serve(String uri, String method, Properties header,
				Properties parms, Properties files) {
			String rootDirPath = rootDir.getPath();
			// return super.serve(uri, method, header, parms, files);
			// Remove URL arguments
			String saneUri = uri.trim().replace( File.separatorChar, '/' );
			if ( saneUri.indexOf( '?' ) >= 0 )
				saneUri = saneUri.substring(0, saneUri.indexOf( '?' ));
			if (listResource.equals(uri)) {
				StringBuilder response = new StringBuilder();
				for (File f : listFiles(rootDir)) {
					String path = f.getPath();
					path = path.replace(rootDirPath, "");
					response.append(encodeUri(path));
					response.append(" ");
					response.append(f.length());
					response.append("\n");
				}
				return new Response(HTTP_OK, "text", response.toString());				
			} else {
				Log.v(TAG, "Download request for " + uri);
				return serveFile(uri, header, rootDir , false);
			}
		}
	
		private List<File> listFiles(File dir) {
			List<File> result = new ArrayList<File>();
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) {
					result.addAll(listFiles(f));
				} else if (f.isFile()) {
					result.add(f);
				}
			}
			return result;
		}
	}

	
	class LocalDeviceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			
		}
	}
	
	private void sendBroadcastStatus() {
		Intent i = new Intent(SHARE_SERVER_CHANGE_ACTION);
		i.putExtra(EXTRA_SHARE_SERVER_NAME, myServiceName);
		i.putExtra(EXTRA_SHARE_SERVER_PORT, server.getLocalPort());
		sendBroadcast(i);
	}
}
