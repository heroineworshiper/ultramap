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
