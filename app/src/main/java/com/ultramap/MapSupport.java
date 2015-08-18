package com.ultramap;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;

public class MapSupport extends MapFragment 
{
	  public View mOriginalContentView;
	  public TouchableWrapper mTouchView;
	  
	  public class TouchableWrapper extends FrameLayout 
	  {

		  public TouchableWrapper(Context context) {
		    super(context);
		  }

		  @Override
		  public boolean dispatchTouchEvent(MotionEvent event) {
		    switch (event.getAction()) {
		      case MotionEvent.ACTION_DOWN:
		            Settings.mapIsTouched = true;
		            break;
		      case MotionEvent.ACTION_UP:
		    	  	Settings.mapIsTouched = false;
		            break;
		    }
		    return super.dispatchTouchEvent(event);
		  }
		}

	  @Override
	  public View onCreateView(LayoutInflater inflater, 
			  ViewGroup parent, 
			  Bundle savedInstanceState) 
	  {
	    mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);    
	    mTouchView = new TouchableWrapper(getActivity());
	    mTouchView.addView(mOriginalContentView);
	    return mTouchView;
	  }

	  @Override
	  public View getView() {
	    return mOriginalContentView;
	  }
}
