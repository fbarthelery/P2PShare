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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

public class FirstScreen extends Activity {

    private SharedPreferences prefs;
    private static final String FIRST_LAUNCH_KEY = "first_launch";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        setContentView(R.layout.activity_first_screen);
        if (!isFirstLaunch())
            moveToNext();
    }
    
    public void onNextClicked(View v) {
	setFirstLaunch(false);
	moveToNext();
    }
    
    private void moveToNext() {
	Intent i = new Intent(this, MainActivity.class);
	startActivity(i);
	finish();
    }
    
    private boolean isFirstLaunch() {
	return prefs.getBoolean(FIRST_LAUNCH_KEY, true);
    }
    
    private void setFirstLaunch(boolean first) {
	prefs.edit().putBoolean(FIRST_LAUNCH_KEY, first).apply();
    }
}
