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
import java.util.Collection;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.util.Log;

public class WifiP2PHelper implements ChannelListener {
	private static final String TAG  = WifiP2PHelper.class.getSimpleName();
	private Context ctx;
	private WifiP2pManager wifiMgr;
	private WifiReceiver wifiReceiver;
	private DiscoveryListener discoveryListener;
	private Channel channel;
	private boolean isSupported = true;
	private boolean isEnabled;
	private boolean isConnected;
	private boolean isConnecting;
	private WifiP2pDevice localDevice;
	private boolean isServiceRequestRegistered;
	private boolean isLocalServiceRegistered;
	private List<ConnectionListener> listeners = new ArrayList<WifiP2PHelper.ConnectionListener>();
	private WifiP2pGroup group;
	
	public WifiP2PHelper(Context context) {
		ctx = context;
		wifiMgr = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
		channel = wifiMgr.initialize(ctx, ctx.getMainLooper(), this);
		wifiReceiver = new WifiReceiver();
		discoveryListener = new DiscoveryListener();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		ctx.registerReceiver(wifiReceiver, filter);
	}
	
	public void startWifiP2P() {
		if (isSupported) {
			if (!isConnected) {
				Log.i(TAG, "Not Connected to any wifi p2p. start wifi p2p discovery");
				startDiscovery();
			} else if (isConnected && isGroupOwner()) {
				Log.i(TAG, "Connected to wifi p2p and is master. start wifi p2p discovery");
				startDiscovery();
			}
		}
	}
	
	public boolean isGroupOwner() {
		if (group == null)
			return false;
		return group.isGroupOwner();
	}
	
	public String getNetworkName() {
		if (group == null)
			return null;
		return group.getNetworkName();
	}
	
	public String getPassphrase() {
		if (group == null)
			return null;
		return group.getPassphrase();
	}
	
	private void registerServiceRequest() {
		if (!isServiceRequestRegistered) {
			WifiP2pDnsSdServiceRequest req = WifiP2pDnsSdServiceRequest.newInstance(ShareService.SERVICE_TYPE);
			wifiMgr.addServiceRequest(channel, req, new TaggedActionListener("addServiceRequest"));
			isServiceRequestRegistered = true;
		}
	}
	
	private void unregisterServiceRequest() {
		wifiMgr.clearServiceRequests(channel, new TaggedActionListener("clearServiceRequest"));
		isServiceRequestRegistered = false;
	}
	
	private void startDiscovery() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			registerServiceRequest();
			wifiMgr.setDnsSdResponseListeners(channel, discoveryListener, null);
			wifiMgr.discoverServices(channel, new TaggedActionListener("discoverService"));
		} else {
			wifiMgr.discoverPeers(channel, new TaggedActionListener("discoverPeers"));
		}
	}
	
	private void stopDiscovery() {
		Log.i(TAG, "stop wifi p2p discovery");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			wifiMgr.stopPeerDiscovery(channel, new TaggedActionListener("stopPeerDiscovery"));
		}
	}
	
	public void registerService(String serviceName, String serviceType) {
		if (!isEnabled)
			return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && !isLocalServiceRegistered) {
			Log.d(TAG, "Register service "+ serviceName + " type " + serviceType);
			WifiP2pDnsSdServiceInfo servInfo = WifiP2pDnsSdServiceInfo.newInstance(serviceName, serviceType , null);
			wifiMgr.addLocalService(channel, servInfo, new TaggedActionListener("addLocalService"));
			isLocalServiceRegistered = true;
		}
	}
	
	public void unregisterServices() {
		if (!isEnabled)
			return;
		wifiMgr.clearLocalServices(channel, new TaggedActionListener("clearLocalServices"));
		isLocalServiceRegistered = false;
	}
	
	public void disconnect() {
		if (isConnected)
			wifiMgr.removeGroup(channel, new TaggedActionListener("removeGroup"));
	}
	
	public boolean isConnected() {
		return isConnected;
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void stopWifiP2P() {
		if (!isConnected){
			unregisterServiceRequest();
			stopDiscovery();
		}
		ctx.unregisterReceiver(wifiReceiver);
	}

	private void refreshPeers() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			Log.i(TAG, "Request peers");
			wifiMgr.requestPeers(channel, discoveryListener);
		}
	}
	
	@Override
	public void onChannelDisconnected() {
		Log.e(TAG, "Error Channel disconnected");
	}

	private class WifiReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				onStateChanged(intent);
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				onConnectionChanged(intent);
			} else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
				onDiscoveryChanged(intent);
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				onConnectionChanged(intent);
			} else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				onPeersChanged(intent);
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				onLocalDeviceChanged(intent);
			}
		}

		private void onLocalDeviceChanged(Intent intent) {
			Log.i(TAG, "Local Wifi p2p device has changed ");
			localDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			if (localDevice.status == WifiP2pDevice.CONNECTED) 
				Log.i(TAG, "Local device is connected");
			else 
				Log.i(TAG, "Local device is not connected");
		}
		
		private void onPeersChanged(Intent intent) {
			Log.i(TAG, "WifiP2p Peers has changed");
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
				Log.i(TAG, "Do not listen to peer changes but only service changes");
			}
			refreshPeers();
		}
		
		private void onConnectionChanged(Intent intent) {
			WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
			NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
			Log.i(TAG, "Wifi p2p connection changed " + netInfo);
			isConnected = netInfo.isConnected();
			Log.i(TAG, "Wifip2p connection is connected ? " + isConnected);
			if (isConnected) {
			wifiMgr.requestConnectionInfo(channel, new ConnectionInfoListener() {
				
				@Override
				public void onConnectionInfoAvailable(WifiP2pInfo info) {
						Log.i(TAG, "Wifi p2p connection is formed ? " + info.groupFormed);
						wifiMgr.requestGroupInfo(channel, new GroupInfoListener() {
							
							@Override
							public void onGroupInfoAvailable(WifiP2pGroup group) {
								if (group == null)
									return;
								WifiP2PHelper.this.group = group;
								Log.i(TAG, "Wifi p2p connection group is  " + group.getNetworkName());
								Log.i(TAG, "Group size " + group.getClientList().size());
								fireOnConnectionSucceed(group.getNetworkName(), group.getPassphrase());

							}
						});
					if (isConnected && !info.isGroupOwner) {
						// normally stopped already
				//		stopDiscovery();
					} else {
						// TODO if is master relaunch a discovery later ?
						startDiscovery();
					}
		
				}
			});
		
			} else {
				group = null;
				fireOnConnectionLost();
			}
				
		}
		
		private void onDiscoveryChanged(Intent intent) {
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
				Log.i(TAG, "Wifi P2P discovery started");
			} else {
				Log.i(TAG, "Wifi P2P discovery stopped");
			}
		}
		
		private void onStateChanged(Intent intent) {
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				Log.i(TAG, "WifiP2P is enabled");
				isEnabled = true;
				fireOnWifiP2PStatechanged(true);
			} else {
				Log.i(TAG, "WifiP2P is disabled. state " + state);
				isEnabled = false;
				isConnected = false;
				isConnecting = false;
				group = null;
				fireOnWifiP2PStatechanged(false);
			}
			
		}
	}

	private class DiscoveryListener implements PeerListListener, DnsSdServiceResponseListener {
		@Override
		public void onDnsSdServiceAvailable(String arg0, String arg1,
				WifiP2pDevice device) {
			Log.i(TAG, "on DnsSdService available");
			connect(device);
		}
		
		@Override
		public void onPeersAvailable(WifiP2pDeviceList peers) {
			Log.i(TAG, "on peers available");
			Collection<WifiP2pDevice> devices = peers.getDeviceList();
			// try to connect to any available
			for (WifiP2pDevice device: devices) {
				connect(device);
			}
		}
		
		private void connect(WifiP2pDevice device) {
			Log.i(TAG, "Wifi device " + device.deviceName + "[" + device.deviceAddress +  "] status is " + device.status );
			Log.i(TAG, "Wifi device " + device.deviceName + "[" + device.deviceAddress +  "] service discovery capable " + device.isServiceDiscoveryCapable() );
			if (device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.INVITED || device.status == WifiP2pDevice.FAILED) {
				Log.i(TAG, "Trying to connect to " + device.deviceName + "[" + device.deviceAddress +  "]");
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
				config.wps.setup = WpsInfo.PBC;
				if (!isConnecting) {
				wifiMgr.connect(channel, config, new TaggedActionListener("connect") {
					@Override
					public void onFailure(int reason) {
						super.onFailure(reason);
						isConnecting = false;
					}
					
					public void onSuccess() {
						super.onSuccess();
						isConnecting = false;
					}
				});
				isConnecting = true;
				}
			}
		}

	}
	

	class TaggedActionListener implements ActionListener {

		private final String tag;
		
		public TaggedActionListener(String tag) {
			this.tag = tag;
		}
		
		@Override
		public void onFailure(int reason) {
			Log.e(TAG, "Error during Wifi P2P operation. operation " + tag + " error " +  reason);
			if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
				Log.e(TAG, "Wifi P2P not supported");
				isSupported = false;
				isEnabled = false;
			}
						
		}

		@Override
		public void onSuccess() {
			Log.d(TAG, "Wifi P2P operation " + tag + " success");
		}
		
	}

	public void addConnectionListener(ConnectionListener listener) {
		listeners.add(listener);
	}
	
	public void removeConnectionListener(ConnectionListener listener) {
		listeners.remove(listener);
	}
	
	private void fireOnConnectionSucceed(String networkName, String passphrase) {
		for (ConnectionListener l : listeners) {
			l.onConnectionSucceed(networkName, passphrase);
		}
	}

	private void fireOnConnectionLost() {
		for (ConnectionListener l : listeners) {
			l.onConnectionLost();
		}
	}
	
	private void fireOnWifiP2PStatechanged(boolean enabled) {
		for (ConnectionListener l : listeners) {
			l.onWifiP2PStateChanged(enabled);
		}
	}
	
	interface ConnectionListener {
		void onWifiP2PStateChanged(boolean enabled);
		void onConnectionSucceed(String networkName, String passphrase);
		void onConnectionLost();
	}
}

