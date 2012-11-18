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

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.genymobile.demo.p2pshare.NsdHelper.NsdListener;

public class SharesListFragment extends ListFragment {

	private static final String TAG = SharesListFragment.class.getSimpleName();
	private ArrayAdapter<NsdServiceInfo> listAdapter;
	private Map<String, NsdServiceInfo> serviceMap;
	private ShareSelectedListener listener;
	private NsdHelper nsdHelper;
	private boolean isConnected;
	private String localServiceName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		serviceMap = new HashMap<String, NsdServiceInfo>();
		listAdapter = new DeviceAdapter(getActivity(), android.R.layout.simple_list_item_1);
        nsdHelper = new NsdHelper(getActivity());
        nsdHelper.addNsdListener(new MyNsdListener());
		setListAdapter(listAdapter);
	}
		
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (ShareSelectedListener) activity;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View contentView = inflater.inflate(R.layout.base_list_fragment, container, false);
		TextView title = (TextView) contentView.findViewById(R.id.Title);
		title.setText("Shares");
		return contentView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (isConnected)
			nsdHelper.startDiscovery();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (isConnected)
			nsdHelper.stopDiscovery();
	}
	
	public void setIsConnected(boolean connected) {
		isConnected = connected;
		if (isConnected)
			nsdHelper.startDiscovery();
		else
			nsdHelper.stopDiscovery();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		NsdServiceInfo info = listAdapter.getItem(position);
		listener.onShareSelected(info);
	}
	
	private void putDevice(final NsdServiceInfo info) {
		if (info.getServiceName().equals(localServiceName)) {
		    	Log.i(TAG, "Found our own service " + localServiceName);
			return;
		}
		if (!serviceMap.containsKey(info.getServiceName())) {
			serviceMap.put(info.getServiceName(), info);
			getActivity().runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					listAdapter.add(info);
				}
			});

		}
	}
	
	public void removeDevice(String serviceName) {
		final NsdServiceInfo info = serviceMap.remove(serviceName);
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				listAdapter.remove(info);
			}
		});
	}
	
	public void setLocalServiceName(String serviceName) {
		localServiceName = serviceName;
	}

	class DeviceAdapter extends ArrayAdapter<NsdServiceInfo> {
		private int textViewResId;
		
		public DeviceAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			textViewResId = textViewResourceId;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v = (TextView) convertView;
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = (TextView) inflater.inflate(textViewResId, null); 
			}
			NsdServiceInfo info = getItem(position);
			String deviceName = info.getServiceName();
			v.setText(deviceName);
			return v;
		}
	}
	
	interface ShareSelectedListener {
		void onShareSelected(NsdServiceInfo service);
	}
	
	private class MyNsdListener implements NsdListener {

		@Override
		public void onServiceFound(NsdServiceInfo info) {
			putDevice(info);		
		}

		@Override
		public void onServiceLost(NsdServiceInfo info) {
			removeDevice(info.getServiceName());
		}

	}
}
