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


