package com.ultramap;

import android.location.Location;
import android.util.Log;


public class Distance {
	void dump()
	{
		Log.v("Distance", "dump active=" + active + 
				" accum=" + accum +
				" prevPosition=" + prevPosition);
	}
	
	
	int loadState(byte [] buffer, int offset)
	{
	// restore timer
		active = (Settings.read_int32(buffer, offset) > 0) ? true : false;
		offset += 4;
		accum = Settings.read_float32(buffer, offset);
		offset += 4;
		boolean havePrev = (Settings.read_int32(buffer, offset) > 0) ? true : false;
		offset += 4;
		if(havePrev)
		{
			prevPosition = new XYZ();
			prevPosition.x = Settings.read_float32(buffer, offset);
			offset += 4;
			prevPosition.y = Settings.read_float32(buffer, offset);
			offset += 4;
			prevPosition.z = Settings.read_float32(buffer, offset);
			offset += 4;
		}
		else
		{
			prevPosition = null;
			offset += 12;
		}
		return offset;
	}
	

	int saveState(byte [] buffer, int offset)
	{
		offset = Settings.write_int32(buffer, offset, active ? 1 : 0);
		offset = Settings.write_float32(buffer, offset, (float)accum);
		offset = Settings.write_int32(buffer, offset, (prevPosition == null) ? 0 : 1);
		if(prevPosition == null)
		{
			offset = Settings.write_int32(buffer, offset, 0);
			offset = Settings.write_int32(buffer, offset, 0);
			offset = Settings.write_int32(buffer, offset, 0);
		}
		else
		{
			offset = Settings.write_float32(buffer, offset, (float)prevPosition.x);
			offset = Settings.write_float32(buffer, offset, (float)prevPosition.y);
			offset = Settings.write_float32(buffer, offset, (float)prevPosition.z);
		}
		return offset;
	}
	

// returns distance in m
	double getDistance()
	{
		return accum;
	}
	
	void setDistance(double distance)
	{
		accum = distance;
	}
	
	void start()
	{
		prevPosition = null;
		active = true;
	}
	
	void stop()
	{
		active = false;
	}
	
	void reset()
	{
		active = false;
		accum = 0;
		prevPosition = null;
	}
	
	void updatePosition(Location location)
	{
		if(active && 
			location != null && 
			location.getAccuracy() < Settings.MIN_ACCURACY)
		{
			XYZ nextPosition = Main.llh_to_xyz(
					location.getLatitude(), 
					location.getLongitude(), 
					location.getAltitude());
	
			if(prevPosition == null)
			{
				prevPosition = nextPosition;
			}
			else
			{
				double distance = Main.distance(prevPosition, nextPosition);
				if(distance > Settings.COARSE_INCREMENT)
				{
					accum += distance;
					prevPosition = nextPosition;
				}
			}
		}
	}
	
	XYZ prevPosition;
	double accum;
	boolean active;
}
