package com.ultramap;
import com.google.android.gms.location.*;

import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class GPSLocationListener implements LocationListener, GpsStatus.Listener
{

	@Override
	public void onGpsStatusChanged(int event) {
//		Log.v("GPSLocationListener", "onGpsStatusChanged event=" + event);
		
	}

	@Override
	public void onLocationChanged(Location location) {
//Log.v("GPSLocationListener", "onLocationChanged location=" + location);
//		synchronized (Main.main) {
//			Main.location = new Location(location);
//		}
	}

}
