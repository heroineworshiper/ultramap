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
Log.v("GPSLocationListener", "onLocationChanged location=" + location);
//		synchronized (Main.main) {
//			Main.location = new Location(location);
//		}
	}

}
