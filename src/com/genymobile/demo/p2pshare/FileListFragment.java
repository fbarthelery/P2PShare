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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.DownloadManager;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileListFragment extends ListFragment {
	private final static String TAG = FileListFragment.class.getSimpleName();
	public static final String ARG_SERVICE = "service";
	private ArrayAdapter<FileInfo> listAdapter;
	private NsdServiceInfo service;
	private TextView title;
	private DownloadManager downloadMgr;
	private NsdHelper nsdHelper;
	private List<ServiceResolveListener> listeners = new ArrayList<FileListFragment.ServiceResolveListener>();
	private boolean isServiceResolved;

	public static FileListFragment newInstance(NsdServiceInfo service) {
		FileListFragment fl = new FileListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_SERVICE, service);
		fl.setArguments(args);
		return fl;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nsdHelper = new NsdHelper(getActivity());
		listAdapter = new ArrayAdapter<FileInfo>(getActivity(), android.R.layout.simple_list_item_1);
		downloadMgr = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		setListAdapter(listAdapter);
		parseArguments();
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View contentView = inflater.inflate(R.layout.base_list_fragment, container, false);
		title = (TextView) contentView.findViewById(R.id.Title);
		title.setText("Files on " + service.getServiceName());
		return contentView;
	}

	private void parseArguments() {
		Bundle args = getArguments();
		NsdServiceInfo service = args.getParcelable(ARG_SERVICE);
		initService(service);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FileInfo fi = listAdapter.getItem(position);

		Uri uri = Uri
				.parse(String.format("http://%s:%d%s", fi.service.getHost()
						.getHostAddress(), fi.service.getPort(), fi.filename));
		Log.i(TAG, "Download file " + uri);
		DownloadManager.Request req = new DownloadManager.Request(uri);
		req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		File sharedir = new File(Environment.getExternalStorageDirectory(),
				"P2PShare");
		req.setDestinationUri(Uri.fromFile(FileUtils.getOuputFile(sharedir,
				fi.filename)));
		long dlid = downloadMgr.enqueue(req);
		Toast.makeText(getActivity(), "download started", Toast.LENGTH_SHORT);
	}
	
	private void initService(NsdServiceInfo info) {
		service = info;
		isServiceResolved = false;
		refresh();
	}
	
	public void refresh() {
		if (isServiceResolved) {
			GetFileAsyncTask task = new GetFileAsyncTask();
			task.execute(service);
		} else {
			nsdHelper.resolveService(service, new ResolveNsdListener() );
		}
	}
	
	public void addServiceResolveListener(ServiceResolveListener listener) {
		listeners.add(listener);
	}
	
	public void removeServiceResolveListener(ServiceResolveListener listener) {
		listeners.remove(listener);
	}

    class FileInfo {
    	String filename;
    	int size;
    	NsdServiceInfo service;
    	
    	@Override
    	public String toString() {
    		String decodedFile = URLDecoder.decode(filename);
    		int idx = decodedFile.lastIndexOf('/');
    		if (idx != -1)
    			decodedFile = decodedFile.substring(idx);
    		if (decodedFile.startsWith("/"))
    			decodedFile = decodedFile.substring(1);
    		return decodedFile;
    	}
    }
    
    private void fireOnServiceResolveFailed(NsdServiceInfo service) {
    	for (ServiceResolveListener l : listeners) {
    		l.onServiceResolveFailed(service);
    	}
    }
    
    interface ServiceResolveListener {
    	void onServiceResolveFailed(NsdServiceInfo service);
    }
     
    private class ResolveNsdListener implements NsdManager.ResolveListener {

		@Override
		public void onServiceResolved(NsdServiceInfo info) {
				service = info;
				isServiceResolved = true;
				refresh();
		}

		@Override
		public void onResolveFailed(NsdServiceInfo info, int arg1) {
				isServiceResolved = false;
				fireOnServiceResolveFailed(info);
		}
    	
    }
    
    
    private class GetFileAsyncTask extends AsyncTask<NsdServiceInfo, Void, List<FileInfo> > {
    	
    	@Override
    	protected void onPostExecute(List<FileInfo> result) {
    		for (FileInfo fi : result) {
    			Log.i(TAG, "file found " + fi);
    		}
    		listAdapter.clear();
			listAdapter.addAll(result);
    	}
    	
	@Override
	protected List<FileInfo> doInBackground(NsdServiceInfo... params) {
	    String host = params[0].getHost().getHostAddress();
	    int port = params[0].getPort();
	    String url = String.format("http://%s:%d/list", host, port);

	    try {
		return getFileInfo(params[0], new URL(url));
	    } catch (MalformedURLException e) {
		Log.e(TAG, "Malformed url " + url, e);
	    } catch (IOException e) {
		isServiceResolved = false;
		fireOnServiceResolveFailed(params[0]);
		Log.e(TAG, "Unable to get file info", e);
	    }
	    return Collections.emptyList();
	}

	private List<FileInfo> getFileInfo(NsdServiceInfo service, URL url) throws IOException {
	    HttpURLConnection connection = null;
	    BufferedReader in = null;
	    try {
		connection = (HttpURLConnection) url.openConnection();
		in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return parseFileInfo(service, in);
	    } finally {
		if (in != null)
		    in.close();
		if (connection != null)
		    connection.disconnect();
	    }
	}

	private List<FileInfo> parseFileInfo(NsdServiceInfo service, BufferedReader in) throws IOException {
	    List<FileInfo> result = new ArrayList<FileInfo>();
	    String line;
	    while ((line = in.readLine()) != null) {
		String elems[] = line.split(" ");
		FileInfo fi = new FileInfo();
		fi.filename = elems[0];
		fi.service = service;
		try {
		    fi.size = Integer.parseInt(elems[1]);
		} catch (NumberFormatException e) {
		    fi.size = -1;
		}
		result.add(fi);
	    }
	    return result;
	}
    }
    
}
