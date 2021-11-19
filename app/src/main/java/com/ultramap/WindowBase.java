/*
 * Ultramap
 * Copyright (C) 2021 Adam Williams <broadcast at earthling dot net>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */


package com.ultramap;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.OnEditorActionListener;

public class WindowBase extends Activity implements Runnable {

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
// get rid of title bar
//        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }
	
	public void onResume() {
		super.onResume();
		startThread();
	}

	public void onPause()
	{
		stopThread();
		super.onPause();
	}


	public void onStop()
	{
		stopThread();
		super.onStop();
	}

	public void startThread()
	{
        thread = new Thread(this);
        thread.start();
	}

	public void stopThread()
	{
        thread.interrupt();
        try {
			thread.join();
		} catch (InterruptedException e) {
		}
  	}

	public void updateGUI()
	{
		
	}

	public void run() {
		while(true)
		{
			if(!handler.hasMessages(0)) handler.sendMessage(
					Message.obtain(handler, 0, this));
			
			
			try {
				Thread.sleep(DT);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	Thread thread;

	static Handler handler = new Handler()
	{
		@Override
        public void handleMessage(Message msg) {
			if(msg.obj != null)
			{
				((WindowBase) msg.obj).updateGUI();
			}
        }
	};
	
	
// the window update period in ms
	static int DT = 200;
}


