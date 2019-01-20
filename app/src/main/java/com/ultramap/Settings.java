package com.ultramap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;

import com.google.android.gms.maps.GoogleMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class Settings {
	static void dump()
	{
		Log.v("Settings", "dump bearing=" + bearing + 
				" latitude=" + latitude + 
				" longitude=" + longitude +
				" zoom=" + zoom
				);
	}
	
	static void load(Context context)
	{
		SharedPreferences file = context.getSharedPreferences("settings", 0);

		beatsPerMinute = file.getInt("beatsPerMinute", beatsPerMinute);
		metronome = file.getBoolean("metronome", metronome);
		bearing = file.getFloat("bearing", (float)bearing);
		latitude = file.getFloat("latitude", (float)latitude);
		longitude = file.getFloat("longitude", (float)longitude);
		zoom = file.getFloat("zoom", (float)zoom);
		enableService = file.getBoolean("enableService", enableService);
		externalGPS = file.getBoolean("externalGPS", externalGPS);
//		voiceFeedback = file.getBoolean("voiceFeedback", voiceFeedback);
		currentRoute = file.getString("currentRoute", currentRoute);
//		compassPointer = file.getBoolean("compassPointer", compassPointer);
		followPosition = file.getBoolean("followPosition", followPosition);
		mapType = file.getInt("mapType", mapType);
		intervalRest = file.getInt("intervalRest", intervalRest);
		intervalRestDistance = file.getFloat("intervalRestDistance", intervalRest);
		restUnits = file.getInt("restUnits", restUnits);
		intervalWork = file.getFloat("intervalWork", (float) intervalWork);
		intervalWorkTime = file.getInt("intervalWorkTime", intervalWorkTime);
		workUnits = file.getInt("workUnits", workUnits);
		sound = file.getInt("sound", sound);
		flashlight = file.getBoolean("flashlight", flashlight);
		cutoffTime = file.getInt("cutoffTime", cutoffTime);
		cutoffDistance = file.getInt("cutoffDistance", cutoffDistance);
	}
	
	
	static void save(Context context)
	{
		SharedPreferences file2 = context.getSharedPreferences("settings", 0);
		SharedPreferences.Editor file = file2.edit();

		file.putInt("beatsPerMinute",  beatsPerMinute);
		file.putBoolean("metronome",  metronome);
		file.putBoolean("enableService",  enableService);
		file.putBoolean("externalGPS",  externalGPS);
//		file.putBoolean("voiceFeedback",  voiceFeedback);
		file.putFloat("bearing", (float)bearing);
		file.putFloat("latitude", (float)latitude);
		file.putFloat("longitude", (float)longitude);
		file.putFloat("zoom", (float)zoom);
		file.putString("currentRoute", currentRoute);
//		file.putBoolean("compassPointer",  compassPointer);
		file.putBoolean("followPosition",  followPosition);
		file.putInt("mapType",  mapType);
		file.putInt("intervalRest", intervalRest);
		file.putFloat("intervalRestDistance", (float)intervalRestDistance);
		file.putFloat("intervalWork", (float)intervalWork);
		file.putInt("intervalWorkTime", intervalWorkTime);
		file.putInt("restUnits", restUnits);
		file.putInt("workUnits", workUnits);
		file.putInt("sound", sound);
		file.putBoolean("flashlight", flashlight);
		file.putInt("cutoffTime", (int)cutoffTime);
		file.putInt("cutoffDistance", (int)cutoffDistance);

		file.commit();
		
	}
	
	static void loadState(Context context)
	{

		
		try {
			FileInputStream is = context.openFileInput("state");
			is.read(prevState, 0, prevState.length);
			int offset = 0;
		

			intervalState = read_int32(prevState, offset);
			offset += 4;
			intervalCountdown = read_int32(prevState, offset);
			offset += 4;
			prevUpdate = read_int32(prevState, offset);
			offset += 4;
			intervalActive = (read_int32(prevState, offset) > 0) ? true : false;
			offset += 4;
			recordRoute = (read_int32(prevState, offset) > 0) ? true : false;
			offset += 4;
			intervalStart = read_int32(prevState, offset);
			offset += 4;
//			offset = intervalDistance.loadState(prevState, offset);
			offset = intervalTimer.loadState(prevState, offset);
			offset = logTimer.loadState(prevState, offset);
			needRestart = read_int32(prevState, offset) > 0 ? true : false;
			offset += 4;
			needPace = read_int32(prevState, offset) > 0 ? true : false;
			offset += 4;
			peakIndex = read_int32(prevState, offset);
			offset += 4;
			peakDuration = read_float32(prevState, offset);
			offset += 4;
			peakPace = read_float32(prevState, offset);
			offset += 4;
			voicePosition = read_float32(prevState, offset);
			offset += 4;

//			intervalDistance.dump();
			intervalTimer.dump();
			

			intervalDBChanged = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	
	
		if(db == null) db  = new IntervalDB(context);
		intervalDBChanged = true;
	}
	
	
	static void saveState(Context context)
	{
		byte [] buffer = new byte[1024];
		int offset = 0;
		

		offset = write_int32(buffer, offset, intervalState);
		offset = write_int32(buffer, offset, intervalCountdown);
		offset = write_int32(buffer, offset, prevUpdate);
		offset = write_int32(buffer, offset, intervalActive ? 1 : 0);
		offset = write_int32(buffer, offset, recordRoute ? 1 : 0);
		offset = write_int32(buffer, offset, intervalStart);
//		offset = intervalDistance.saveState(buffer, offset);
		offset = intervalTimer.saveState(buffer, offset);
		offset = logTimer.saveState(buffer, offset);
		offset = write_int32(buffer, offset, needRestart ? 1 : 0);
		offset = write_int32(buffer, offset, needPace ? 1 : 0);
		offset = write_int32(buffer, offset, peakIndex);
		offset = write_float32(buffer, offset, (float)peakDuration);
		offset = write_float32(buffer, offset, (float)peakPace);
		offset = write_float32(buffer, offset, (float)voicePosition);

		boolean needWrite = false;
		for(int i = 0; i < offset; i++)
		{
			if(buffer[i] != prevState[i])
			{
				needWrite = true;
				break;
			}
		}
		
		if(needWrite)
		{
			try {
				FileOutputStream os = context.openFileOutput("state", 0);
//				Log.v("Settings", "saveState path=" + os.toString());
				
				os.write(buffer, 0, offset);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.arraycopy(buffer, 0, prevState, 0, offset);
		}
		
	}
	

	public static int write_int32(byte[] data, int offset, int value) 
	{
		data[offset++] = (byte)(value & 0xff);
		data[offset++] = (byte)((value >> 8) & 0xff);
		data[offset++] = (byte)((value >> 16) & 0xff);
		data[offset++] = (byte)((value >> 24) & 0xff);
		return offset;
	}

	public static int write_int64(byte[] data, int offset, long value) 
	{
		data[offset++] = (byte)(value & 0xff);
		data[offset++] = (byte)((value >> 8) & 0xff);
		data[offset++] = (byte)((value >> 16) & 0xff);
		data[offset++] = (byte)((value >> 24) & 0xff);
		data[offset++] = (byte)((value >> 32) & 0xff);
		data[offset++] = (byte)((value >> 40) & 0xff);
		data[offset++] = (byte)((value >> 48) & 0xff);
		data[offset++] = (byte)((value >> 56) & 0xff);
		return offset;
	}

	static public int read_int32(byte[] data, int offset)
	{
		return (data[offset] & 0xff) | 
			((data[offset + 1] & 0xff) << 8) | 
			((data[offset + 2] & 0xff) << 16) | 
			((data[offset + 3]) << 24);
	}

	static public long read_int64(byte[] data, int offset)
	{
		return (long)(data[offset] & 0xff) | 
			((long)(data[offset + 1] & 0xff) << 8) | 
			((long)(data[offset + 2] & 0xff) << 16) | 
			((long)(data[offset + 3] & 0xff) << 24) | 
			((long)(data[offset + 4] & 0xff) << 32) | 
			((long)(data[offset + 5] & 0xff) << 40) | 
			((long)(data[offset + 6] & 0xff) << 48) | 
			((long)(data[offset + 7] & 0xff) << 56);
	}

	// from http://www.captain.at/howto-java-convert-binary-data.php
	static public float read_float32(byte[] data, int offset)
	{
		int i = 0;
		int len = 4;
		int out = 0;
		int in = 0;
		byte[] tmp = new byte[len];
		for (in = offset; in < (offset + len); in++) 
		{
			tmp[out] = data[in];
			out++;
		}
		int accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) 
		{
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return Float.intBitsToFloat(accum);
	}
	
	static public int write_float32(byte[] data, int offset, float value)
	{
		return write_int32(data, offset, Float.floatToIntBits(value));
	}

	
	static public String getSaveExtension()
	{
		if(!saveGPX)
			return "kml";
		else
			return "gpx";
	}

	static int soundToFile(int sound)
	{
		switch(sound) {
			case 0:
				return R.raw.hi;
			case 1:
				return R.raw.cimbal;
			case 2:
				return R.raw.cowbell;
			case 3:
				return R.raw.dino;
		}
		return R.raw.hi;
	}



// meters before a new point is considered valid
    static double FINE_INCREMENT = 1.0;
//    static double FINE_INCREMENT = 0.0;
// in meters
	static double COARSE_INCREMENT = 10.0;
// meters of accuracy required 
	static float MIN_ACCURACY = 200.0f;

	// generate fake GPS coordinates for testing
	static boolean fakeGPS = false;
	static double fakeLatitude = 37.81;
	static double fakeLongitude = -122.36;
	static double fakeAltitude = 0;
// fastStep is used when intervals are active
	static double fastStep = 0.00004;
// for testing intervals, the slowStep is significantly slower than
// the fast step
//	static double slowStep = 0.00002;
	static double slowStep = 0.00003;
	static double latitudeStep = 0.000;
	static double altitudeStep = 0;
	static int fakeCounter = 0;

	// enable the ultramap service
	static boolean enableService = true;
	static boolean externalGPS = false;
	static double bearing = 0;
	static double latitude = 0;
	static double longitude = 0;
	static double zoom = 1;
	static boolean editRoute = false;
	static boolean recordRoute = false;
	static boolean compassPointer = true;
	static boolean followPosition = true;
	static boolean voiceFeedback = true;

// seconds
	static int cutoffTime = 0;
// meters
	static int cutoffDistance = 0;
	
// last voice readout
	static double voicePosition = 0.0;
// distance between voice positions, in meters
	static double voiceInterval = Main.miToM(0.25);
	static boolean mapIsTouched = false;
	static int mapType = GoogleMap.MAP_TYPE_HYBRID;

	static boolean metronome = false;
	static int beatsPerMinute = 60;
	static int sound = 0;

// units
	static final int SECONDS = 0;
	static final int MILES = 1;
	static final int METERS = 2;

// time for rest in seconds, if the units are a time
	static int intervalRest = 60;
// rest distance in m, if the units are a distance
	static double intervalRestDistance = Main.miToM(0.25);
	static int restUnits = SECONDS;
// distance for work in meters, if the units are a distance
	static double intervalWork = Main.miToM(0.25);
// distance in seconds, if the units are time
	static int intervalWorkTime = 60;
	static int workUnits = MILES;


	static String BLUETOOTH_ID = "gps_bluetooth";
//	static File dir = new File(Environment.getExternalStorageDirectory() + "//ultramap//");
//	static File dir = new File("//storage//external_SD//ultramap//");
	static File dir = new File("//sdcard//ultramap//");
//	static File camDir = new File("//storage//external_SD//DCIM//Camera//");
//	static File camDir = new File("//storage//sdcard1//DCIM//100KYCRA//");
	static File camDir = new File("//sdcard//DCIM//Camera//");
//	static File camDir = new File("//sdcard//DCIM//OpenCamera//");
// web server parameters
//	static String wwwhome = "/storage/external_SD/html/";
	static String wwwhome = "//sdcard//ultramap//html//";

	// web server parameters
	static int port = 8080;
// filename of route being viewed or edited
	static String currentRoute = null;
// points on current route
	static Vector<RoutePoint> route = new Vector<RoutePoint>();
// current point being edited
	static RoutePoint editPoint1 = null;
// point after current point being edited
	static RoutePoint editPoint2 = null;
// points on recorded route
	static Vector<RoutePoint> log = new Vector<RoutePoint>();
// times
	static IntervalDB db;
	static boolean intervalDBChanged = false;
	
	
	static int WORK = 0;
	static int REST = 1;
	static int COUNTDOWN = 2;
	
	
	static int intervalState = WORK;
	static boolean intervalActive = false;
	static int intervalCountdown;
	static Timer intervalTimer = new Timer();
// start index of interval in log
	static int intervalStart = -1;
//	static Distance intervalDistance = new Distance();
	// need the pace from the last interval
	static boolean needPace = false;
// location in log of current peak pace
	static int peakIndex = 0;
// current peak duration in seconds
	static double peakDuration = 0;
// current peak pace in seconds/meters
	static double peakPace = 0;
// time in seconds to wait for peak
	public static final long PACE_LAG = 10;

// last interval training voice update in miles * 100 or seconds
	static int prevUpdate;
// relative time of log points
	static Timer logTimer = new Timer();
		
	// file select operations
	// want to load a file
	static boolean selectLoad = false;
	// want to save a file
	static boolean selectSave = false;
	// want to save a log file
	static boolean selectSaveLog = false;
	static boolean saveGPX = false;
// update interval for GPS in ms
	static int DT = 1000;
	static byte [] prevState = new byte[1024];
// previous position for log recording.  Not saved with state.
// get still get off if the program restarts a long way from
// where it stopped.
	static XYZ prevFineXYZ;
	static XYZ prevCoarseXYZ;
// need to restart GPS
	static boolean needRestart = true;
	static boolean flashlight = false;
}






