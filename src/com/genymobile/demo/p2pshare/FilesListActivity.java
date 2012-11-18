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
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;

public class FilesListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);

        if (savedInstanceState == null) {
            NsdServiceInfo info =   getIntent().getParcelableExtra(FileListFragment.ARG_SERVICE);
            Bundle arguments = new Bundle();
            arguments.putParcelable(FileListFragment.ARG_SERVICE,
                 info );
            FileListFragment fragment = new FileListFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.fileListContainer, fragment)
                    .commit();
        }
    }
}
