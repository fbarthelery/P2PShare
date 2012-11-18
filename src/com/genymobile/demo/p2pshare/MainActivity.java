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

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.genymobile.demo.p2pshare.FileListFragment.ServiceResolveListener;
import com.genymobile.demo.p2pshare.SharesListFragment.ShareSelectedListener;

public class MainActivity extends Activity implements ShareSelectedListener, ServiceResolveListener {
	private static final String TAG = MainActivity.class.getSimpleName(); 

	private WifiFragment wifiFragment;
	private ShareReceiver shareReceiver;
	private boolean isDualPane;
	private SharesListFragment sharesListFragment;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        isDualPane = getResources().getBoolean(R.bool.DualPaneUI);
        sharesListFragment = (SharesListFragment) getFragmentManager().findFragmentById(R.id.SharesListFragment); 
        wifiFragment = (WifiFragment) getFragmentManager().findFragmentById(R.id.wifiFragment);
        shareReceiver = new ShareReceiver();

    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	IntentFilter filter = new IntentFilter(ShareService.SHARE_SERVER_CHANGE_ACTION);
    	registerReceiver(shareReceiver, filter);
    	
    	startShareService();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	unregisterReceiver(shareReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop_share:
			stopShareService();
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
    }
    
    private void startShareService() {
    	Intent i = new Intent(this, ShareService.class);
		startService(i);
    }
    
    private void stopShareService() {
		Intent i = new Intent(this, ShareService.class);
		stopService(i);
    }

    @Override
    public void onShareSelected(NsdServiceInfo service) {
	if (isDualPane) {
	    FileListFragment ft = (FileListFragment) getFragmentManager().findFragmentByTag(service.getServiceName());
	    if (ft == null) {
    		ft = FileListFragment.newInstance(service);
    		ft.addServiceResolveListener(this);
	    } else
		ft.refresh();
	    FragmentTransaction transact = getFragmentManager().beginTransaction();
	    transact.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
	    transact.replace(R.id.fileListContainer, ft, service.getServiceName());
	    transact.commit();
	} else {
	    Intent filesIntent = new Intent(this, FilesListActivity.class);
	    filesIntent.putExtra(FileListFragment.ARG_SERVICE, service);
	    startActivity(filesIntent);
	}
		
    }
    
	class ShareReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int port = intent.getIntExtra(ShareService.EXTRA_SHARE_SERVER_PORT, -1);
			String name = intent.getStringExtra(ShareService.EXTRA_SHARE_SERVER_NAME);
			Log.i(TAG, "registered service name is " + name);
			if (name != null) {
				wifiFragment.registerService(name);
				sharesListFragment.setLocalServiceName(name);
			}

		}
	}

	@Override
	public void onServiceResolveFailed(NsdServiceInfo service) {
	    sharesListFragment.removeDevice(service.getServiceName());
	}

	
}