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

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.genymobile.demo.p2pshare.WifiP2PHelper.ConnectionListener;

public class WifiFragment extends Fragment implements OnClickListener, ConnectionListener {

	private static final String TAG = WifiFragment.class.getSimpleName();
	private Button connectBtn;
	private TextView wifiStatusTxt;
	private TextView wifiStatusDefailtsTxt;
	private WifiP2PHelper wifiHelper;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.wifi_fragment, container, false);
		connectBtn = (Button) v.findViewById(R.id.toggle_wifi_p2p);
		connectBtn.setOnClickListener(this);
		wifiStatusTxt = (TextView) v.findViewById(R.id.WifiStatus);
		wifiStatusDefailtsTxt = (TextView) v.findViewById(R.id.WifiStatusDetails);
		return v;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		wifiHelper = new WifiP2PHelper(getActivity());
		wifiHelper.addConnectionListener(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();
//		wifiHelper.startWifiP2P();
	}
	
	@Override
	public void onClick(View v) {
		if (v == connectBtn) {
			onConnectBtnClicked();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		wifiHelper.stopWifiP2P();
	}

	@Override
	public void onConnectionSucceed(String networkName, String passphrase) {
		connectBtn.setText("disconnect");
		wifiStatusTxt.setText(networkName);
    	if (passphrase != null) {
    		wifiStatusDefailtsTxt.setText("Passphrase : " + passphrase);
    	} else
    		wifiStatusDefailtsTxt.setText(null);
    	SharesListFragment df = (SharesListFragment) getFragmentManager().findFragmentById(R.id.SharesListFragment);
		if (df != null)
			df.setIsConnected(true);
	}

	@Override
	public void onConnectionLost() {
		connectBtn.setText("connect");
		wifiStatusTxt.setText("Not connected on Wifi Direct");
		wifiStatusDefailtsTxt.setText(null);
		SharesListFragment df = (SharesListFragment) getFragmentManager().findFragmentById(R.id.SharesListFragment);
		if (df != null)
			df.setIsConnected(false);
		Fragment fileFragment = getFragmentManager().findFragmentById(R.id.fileListContainer);
		if (fileFragment != null)
			getFragmentManager().beginTransaction().remove(fileFragment).commitAllowingStateLoss();
	}
	
	public void registerService(String name) {
		wifiHelper.unregisterServices();
		wifiHelper.registerService(name, ShareService.SERVICE_TYPE);
	}

	@Override
	public void onWifiP2PStateChanged(boolean enabled) {
		if (!enabled) {
		connectBtn.setText("Enable Wifi Direct");
		wifiStatusDefailtsTxt.setText(null);
		wifiStatusTxt.setText("Wifi Direct not enabled");
		} else {
			connectBtn.setText("Connect");
			wifiStatusDefailtsTxt.setText(null);
			wifiStatusTxt.setText("Not Connected on Wifi Direct");
		}
	}

	private void onConnectBtnClicked() {
		if (!wifiHelper.isEnabled()) {
			launchWifiSettings();
		} else if (wifiHelper.isConnected()) {
			wifiHelper.disconnect();
		} else {
			wifiHelper.startWifiP2P();
			wifiStatusTxt.setText("Connexion en cours");
			wifiStatusDefailtsTxt.setText(null);
		}
	}

	private void launchWifiSettings() {
		try {
			Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "Unable to launch wifi settings activity", e);
			Toast.makeText(getActivity(), "Please enable Wifi direct", Toast.LENGTH_SHORT).show();
		}
	}
}
