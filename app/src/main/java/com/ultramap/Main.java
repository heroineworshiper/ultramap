/*
 * Ultramap
 * Copyright (C) 2021-2023 Adam Williams <broadcast at earthling dot net>
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

// Example: https://robertohuertas.com/2019/06/29/android_foreground_services/





package com.ultramap;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera;
//import android.hardware.camera2.CameraAccessException;
//import android.hardware.camera2.CameraCaptureSession;
//import android.hardware.camera2.CameraCharacteristics;
//import android.hardware.camera2.CameraDevice;
//import android.hardware.camera2.CameraManager;
//import android.hardware.camera2.CameraMetadata;
//import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
//import android.util.Size;
import android.view.MenuItem;
//import android.view.Surface;


// GPS must be accessed by a Service instead of an IntentService
public class Main extends Service implements TextToSpeech.OnInitListener {
	static Main main;
	static Context context;
	static Settings settings;
	static PowerManager.WakeLock wakeLock;
    static Thread thread;
    
	static boolean haveGUI = false;
	static boolean haveLocation = false;
	static long locationTime = 0;
	static float locationAccuracy = 0;
	static Timer lastLocationTimer = new Timer();
	static int locationTimeout = 0;
	static Location location;
	// total location updates since restarting GPS
	static int totalLocations = 0;
	static ExternalClient externalClient;
	static TextToSpeech tts;
	static boolean ttsReady;
	static OutputStream logTemp;
	static int LOG_PACKET_SIZE = 32;
	static String bluetoothStatus = "Bluetooth status";
	static WebServer webServer;
	// The number of loation threads.  Set real low to disable the location pool
	static final int POOL_SIZE = 10;
	//	static final int POOL_SIZE = 1;
	// in ms.  Set real high to disable the location pool
	static final int MAX_AGE = 10000;
	//	static final int MAX_AGE = 1000000000;
	// GPS timeouts in ms
	// time to acquire the 1st signal
	static final int GPS_TIMEOUT1 = 300000;
	// time to give up after 1st signal is received
	static final int GPS_TIMEOUT2 = 30000;
	static LocationThread[] locationPool = new LocationThread[POOL_SIZE];
	static boolean haveLocationPool = false;
	static Metronome metronome = null;
	static int prevTempo = 0;
	static int prevSound = -1;
//	static boolean flashlightOn = false;
	static Camera cam = null;
//	static CameraManager cameraManager = null;
//	static CameraDevice cameraDevice = null;
//	static CameraCaptureSession cameraSession = null;
//	static CaptureRequest.Builder cameraBuilder = null;
	private static final int MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION = 100;


	public Main() {
		super();

//		Log.v("Main", "Main");
	}

	static public void requestPermission(Activity activity, String permission)
	{
		if (ContextCompat.checkSelfPermission(
				activity,
				permission) ==
				PackageManager.PERMISSION_GRANTED)
		{
			// You can use the API that requires the permission.
		}
		else
		if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
				permission))
		{
			// In an educational UI, explain to the user why your app requires this
			// permission for a specific feature to behave as expected. In this UI,
			// include a "cancel" or "no thanks" button that allows the user to
			// continue using your app without granting the permission.
		}
		else
		{
			// You can directly ask for the permission.
			ActivityCompat.requestPermissions(activity,
					new String[] { permission },
					MY_PERMISSIONS_REQUEST_READ_FINE_LOCATION);
		}




	}

	static public void initialize(Activity activity) {
		Main.context = activity;
		haveGUI = true;
Log.i("x", "Main.initialize 1");

		if (main == null) {
			lastLocationTimer.start();
//			Log.v("Main", "initialize");
			loadState(context);
//			updateFlashlight();


			requestPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
			requestPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
Log.i("x", "Main.initialize 2");

			Intent serviceIntent = new Intent(activity, Main.class);
			// android 26 requires this to be a foreground service to stay active
			activity.startForegroundService(serviceIntent);
			//activity.startService(serviceIntent);

		}

	}

	// recover where we were when shut down
	static void loadState(Context context) {
		if (settings == null) {
			settings = new Settings();
			Settings.load(context);
			Settings.loadState(context);

// recover log file
			FileInputStream fd = null;
			try {
				fd = context.openFileInput("logTemp");


				byte[] buffer = new byte[LOG_PACKET_SIZE * 50];
				boolean done = false;
				while (!done) {
					int bytes = fd.read(buffer, 0, buffer.length);
					for (int i = 0; i < bytes; i += LOG_PACKET_SIZE) {
						if (bytes - i >= LOG_PACKET_SIZE) {
							int offset = i;
							RoutePoint point = new RoutePoint();
							point.latitude = Settings.read_float32(buffer, offset);
							offset += 4;
							point.longitude = Settings.read_float32(buffer, offset);
							offset += 4;
							point.altitude = Settings.read_float32(buffer, offset);
							offset += 4;
							point.distance = Settings.read_float32(buffer, offset);
							offset += 4;
							point.time = Settings.read_int64(buffer, offset);
							offset += 8;
							point.relativeTime = Settings.read_int64(buffer, offset);
							offset += 8;
							Settings.log.add(point);

						}
					}

					if (bytes < LOG_PACKET_SIZE) done = true;
				}

				Settings.prevFineXYZ = null;
				Settings.prevCoarseXYZ = null;

				if (Settings.log.size() > 0) {
					RoutePoint lastPoint = Settings.log.get(Settings.log.size() - 1);
					Settings.prevFineXYZ = llh_to_xyz(
							lastPoint.latitude,
							lastPoint.longitude,
							lastPoint.altitude);

// find the last change in distance
					for (int i = Settings.log.size() - 2; i >= 0; i--) {
						RoutePoint point2 = Settings.log.get(i);
						if (lastPoint.distance != point2.distance ||
								i == 0) {
							RoutePoint point3 = Settings.log.get(i + 1);
							Settings.prevCoarseXYZ = llh_to_xyz(
									point3.latitude,
									point3.longitude,
									point3.altitude);
							break;
						}
					}
				}

				fd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}


		}
	}

//	boolean needGPS()
//	{
//		return (Settings.keepAlive ||
//				Settings.recordRoute ||
//				Settings.intervalActive ||
//				haveGUI);
//	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		String CHANNEL_ID = "my_channel_01";
		NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
				"Channel human readable title",
				NotificationManager.IMPORTANCE_DEFAULT);

		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("")
				.setContentText("").build();
		startForeground(1, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
Log.i("x", "Main.onStartCommand main=" + main);
        startService();
// 		if (intent != null) {
// 			if (intent.getAction() == Intent.ACTION_RUN) 
//                 startService();
// 			else if (intent.getAction() == Intent.ACTION_SHUTDOWN) 
//                 stopService();
// 			else
// 				Log.i("Main", "intent.action=" + intent.getAction());
// 		}

		return START_STICKY;
	}

	public void startService()
	{
		if(main != null) return;

		haveGUI = false;
		main = this;
		context = getApplicationContext();
		tts = new TextToSpeech(context, this);
Log.i("x", "Main.startService 1");
		wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"Main::wakeLock");
		wakeLock.acquire();

		loadState(context);

		webServer = new WebServer();
		webServer.start();
Log.i("x", "Main.startService 2");

		thread = new Thread(new Runnable(){
            @Override
            public void run()
            {
                while(true)
                {
Log.i("x", "Main.startService 3");



			        long minAge = 0x7fffffff;
			        int minIndex = -1;
			        long maxAge = -1;
			        int maxIndex = -1;
			        int totalThreads = 0;
			        if (!Settings.externalGPS) {
				        // Find most recent locationThread
				        for (int i = 0; i < POOL_SIZE; i++) {
					        if (locationPool[i] != null) {
						        totalThreads++;
						        long age = locationPool[i].timer.getTime();
						        if (age < minAge) {
							        minAge = age;
							        minIndex = i;
						        }

						        if (age > maxAge) {
							        maxAge = age;
							        maxIndex = i;
						        }
					        }
				        }


				        // time to create another locationThread
				        if (minAge > MAX_AGE) {
					        int index = -1;
					        if (totalThreads < POOL_SIZE) {
						        for (int i = 0; i < POOL_SIZE; i++) {
							        if (locationPool[i] == null) {
								        index = i;
								        break;
							        }
						        }
					        } else {
						        // replace oldest one
						        index = maxIndex;
					        }

					        if (locationPool[index] != null) {
						        locationPool[index].stop();
					        }

					        Log.v("Main", "onStartCommand new locationThread in slot " + index);
					        locationPool[index] = new LocationThread();
					        haveLocationPool = true;
				        }
			        } else if (externalClient == null) {
				        externalClient = new ExternalClient();
				        externalClient.start();
			        }


			        Location location = null;
			        if (!Settings.externalGPS) {
				        // Get location from most recent locationThread
				        if (minIndex < 0) {
					        minIndex = POOL_SIZE - 1;
				        }

				        for (int i = 0; i < POOL_SIZE; i++) {
					        if (locationPool[minIndex] != null) {
						        location = LocationServices.FusedLocationApi.getLastLocation(
								        locationPool[minIndex].mGoogleApiClient);
						        if (location != null) {
							        break;
						        }

						        minIndex--;
						        if (minIndex < 0) {
							        minIndex = POOL_SIZE - 1;
						        }
					        }
				        }


				        // Can't pull the location.  Have to push it from the GPSLocationListeners to this.location, then copy it.
				        //synchronized(this)
				        //{
				        //	if(this.location != null) {
				        //		location = new Location(this.location);
				        //	}
				        //}
			        } else if (Settings.externalGPS &&
					        externalClient != null) {
				        location = externalClient.getLastLocation();
			        }


			        // new location hasn't been received since restarting
			        locationTimeout = GPS_TIMEOUT1;
			        if (totalLocations > 0) {
				        // new location has been received since restarting
				        locationTimeout = GPS_TIMEOUT2;
			        }

        //			Log.i("Main", " Timer=" + lastLocationTimer.getTime() +
        //					" timeout=" + locationTimeout +
        //					" newtime=" + ((location == null ? -1 : location.getTime()) % 60000) +
        //					" prevtime=" + (locationTime % 60000) +
        //					" location=" + (location != null));


			        if (location != null) {
				        if (location.getTime() == locationTime) {
					        // if the location time hasn't changed in too long, restart
					        if (lastLocationTimer.getTime() > locationTimeout) {
						        haveLocation = false;
						        restartGPS();
					        }
				        } else {
					        // the location time has changed
					        totalLocations++;
					        lastLocationTimer.reset();
					        lastLocationTimer.start();
					        haveLocation = true;
				        }

				        locationTime = location.getTime();
				        locationAccuracy = location.getAccuracy();

				        if (!Settings.externalGPS) {
					        synchronized (this) {
						        Main.location = location;
					        }
				        }
			        } else {
				        haveLocation = false;
				        // if the location time hasn't changed in too long, restart
				        if (lastLocationTimer.getTime() > locationTimeout) {
					        restartGPS();
				        }
			        }


			        if (location != null &&
					        location.getAccuracy() < Settings.MIN_ACCURACY) {

				        if (Settings.fakeGPS) {
					        location.setLatitude(Settings.fakeLatitude);
					        location.setLongitude(Settings.fakeLongitude);
					        location.setAltitude(Settings.fakeAltitude);
					        Settings.fakeLatitude += Settings.latitudeStep;

					        double step = Settings.slowStep;
					        if (Settings.intervalState == Settings.WORK &&
							        Settings.intervalActive) {
						        if (Settings.fakeCounter < 10)
							        Settings.fakeCounter++;
						        if (Settings.fakeCounter >= 10)
							        step = Settings.fastStep;
					        } else {
						        if (Settings.fakeCounter > 0)
							        Settings.fakeCounter--;
						        if (Settings.fakeCounter > 0)
							        step = Settings.fastStep;
					        }


        //					Log.v("onStartCommand", "step=" + step);


					        Settings.fakeLongitude += step;
					        Settings.fakeAltitude += Settings.altitudeStep;

				        }

				        if (Settings.recordRoute) {
        //Log.v("Main", "onStartCommand location=" + location);

					        updateLog(location);
				        }

			        }

			        if (Settings.intervalActive) {

				        updateInterval(location);
			        }

			        if (Settings.needPace) updatePace();
        //Log.v("Main", "run Settings.intervalActive=" + Settings.intervalActive);


		            if ((prevTempo != Settings.beatsPerMinute ||
				            prevSound != Settings.sound) &&
				            metronome != null) {
			            metronome.stop2();
			            metronome = null;
		            }

		            if (Settings.metronome && metronome == null) {
			            metronome = new Metronome();
			            metronome.start();
			            prevTempo = Settings.beatsPerMinute;
			            prevSound = Settings.sound;
		            } else if (!Settings.metronome && metronome != null) {
			            metronome.stop2();
			            metronome = null;
		            }


		            Settings.saveState(context);
Log.i("x", "Main.startService 2");

                    try
                    {
                        Thread.sleep(1000);
                    }catch(Exception e)
                    {
                    }
                }
            }
        });
        thread.start();
        
	}




	public void stopService()
	{

	}

	void restartGPS() {
		// stop all the location objects
		for (int i = 0; i < POOL_SIZE; i++) {
			if (locationPool[i] != null) {
				locationPool[i].stop();
				locationPool[i] = null;
			}

		}
		lastLocationTimer.reset();
		lastLocationTimer.start();

		totalLocations = 0;
	}

// 	public static void updateFlashlight() {
// 		if (!flashlightOn && Settings.flashlight) {
// 			if (cam == null) {
// 				cam = Camera.open();
// 			}
// 
// 			Camera.Parameters p = cam.getParameters();
// 			p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
// 			cam.setParameters(p);
// 			cam.startPreview();
// 			flashlightOn = true;
// 
// //			if(cameraManager == null)
// //			{
// //				try {
// //					cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
// //					cameraManager.openCamera("0", new CameraDevice.StateCallback()
// //					{
// //
// //						@Override
// //						public void onOpened(CameraDevice camera) {
// //							cameraDevice = camera;
// //							try
// //							{
// //								cameraBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
// //								cameraBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
// //								List<Surface> list = new ArrayList<Surface>();
// //								SurfaceTexture surfaceTexture = new SurfaceTexture(1);
// //								Size size = getSmallestSize(cameraDevice.getId());
// //								surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
// //                				Surface surface = new Surface(surfaceTexture);
// //                				list.add(surface);
// //                				cameraBuilder.addTarget(surface);
// //                				camera.createCaptureSession(list,
// //									new CameraCaptureSession.StateCallback()
// //									{
// //										@Override
// //        								public void onConfigured(CameraCaptureSession session)
// //										{
// //            								cameraSession = session;
// //            								try {
// //                								cameraSession.setRepeatingRequest(cameraBuilder.build(), null, null);
// //            									flashlightOn = true;
// //											} catch (CameraAccessException e) {
// //                								e.printStackTrace();
// //            								}
// //        								}
// //
// //        								@Override
// //        								public void onConfigureFailed(CameraCaptureSession session)
// //										{
// //
// //        								}
// //									}, null);
// //							}catch(Exception e)
// //							{
// //
// //							}
// //						}
// //
// //						private Size getSmallestSize(String cameraId) throws CameraAccessException
// //						{
// //        					Size[] outputSizes = cameraManager.getCameraCharacteristics(cameraId)
// //                					.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
// //                					.getOutputSizes(SurfaceTexture.class);
// //        					if (outputSizes == null || outputSizes.length == 0) {
// //            					throw new IllegalStateException(
// //                    					"Camera " + cameraId + "doesn't support any outputSize.");
// //        					}
// //        					Size chosen = outputSizes[0];
// //        					for (Size s : outputSizes) {
// //            					if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
// //                					chosen = s;
// //            					}
// //        					}
// //        					return chosen;
// //    					}
// //
// //
// //						@Override
// //						public void onDisconnected(CameraDevice camera) {
// //
// //						}
// //
// //						@Override
// //						public void onError(CameraDevice camera, int error) {
// //
// //						}
// //
// //
// //
// //					}, null);
// //
// //
// //				}
// //				catch(Exception e)
// //				{
// //
// //				}
// //			}
// 		}


// 		if (flashlightOn && !Settings.flashlight) {
// 
// 			if (cam == null) {
// 				cam = Camera.open();
// 			}
// 
// 
// 			cam.stopPreview();
// 			cam.release();
// 			cam = null;
// 
// //			if(cameraSession != null)
// //			{
// //				cameraSession.close();
// //				cameraDevice.close();
// //				cameraDevice = null;
// //				cameraSession = null;
// //			}
// 			flashlightOn = false;
// 		}
// 
// 	}



	// start the recording if it isn't already going
	static void startRecording()
	{

		if(!Settings.recordRoute)
		{
			Settings.recordRoute = true;
			Main.startLog();
			Main.sayText("Recording started");
		}

	}


	static void toggleRecording()
	{
		Settings.recordRoute = !Settings.recordRoute;


		if(Settings.recordRoute)
		{
			Main.startLog();
			Main.sayText("Recording started");
		}
		else
		{
			// just stopped
			Settings.logTimer.stop();
			Main.sayText("Recording paused");
		}

// want pause to resume from previous point
//		Settings.prevFineXYZ = null;
//		Settings.prevCoarseXYZ = null;

		Settings.save(Main.context);
		Settings.saveState(Main.context);
	}

	static void resetIntervals()
	{
		Settings.db.getWritableDatabase().delete(IntervalDB.TABLE_NAME,
				null,
				null);

		Settings.intervalActive = false;
		Settings.intervalStart = -1;
		Settings.intervalTimer.reset();
		Settings.intervalState = Settings.WORK;
		Settings.saveState(Main.context);
		//Main.sayText("Reset");
	}



	static double getIntervalDistance()
	{
		double distance = 0;
		if(Settings.log.size() > 0 &&
				Settings.intervalStart >= 0)
		{
			distance = Settings.log.get(Settings.log.size() - 1).distance -
					Settings.log.get(Settings.intervalStart).distance;
		}
		return distance;
	}
	
	void startWork()
	{
		Settings.needPace = false;
		Settings.prevUpdate = 0;
		Settings.intervalState = Settings.WORK;
		Settings.intervalTimer.reset();
		Settings.intervalTimer.start();
		Settings.intervalStart = Settings.log.size() - 1;
		if(Settings.intervalStart < 0)
			Settings.intervalStart = 0;
		Settings.intervalState = Settings.WORK;
		sayText("Begin workout.");
	}
	
	void startRest()
	{
		double elapsed = Settings.intervalTimer.getTime();
		double distance = getIntervalDistance();
		Settings.intervalState = Settings.REST;
		Settings.intervalTimer.reset();
		Settings.intervalTimer.start();
		sayText("Begin rest.");
		
		
		Settings.needPace = true;
// Store estimate of pace in seconds per meter
		double pace = 0;
		if(distance > 0)
			pace = (elapsed / 1000) / distance;
		Settings.db.addInterval(elapsed / 1000, pace, distance);
// Make GUI update
		Settings.intervalDBChanged = true;
// Store current maximum speed
		Settings.peakIndex = Settings.log.size() - 1;

		
// No log
		if(Settings.peakIndex < 0) 
		{
			Settings.needPace = false;
		}
		else
		{
			double distance2 = Settings.log.get(Settings.peakIndex).distance;
			long time2 = Settings.log.get(Settings.peakIndex).time;
			double distance1 = 0; 
			for(int i = Settings.peakIndex - 1; i >= 0; i--)
			{
				distance1 = Settings.log.get(i).distance;
				distance = distance2 - distance1;
				if(distance >= Settings.intervalWork)
				{
					long time1 = Settings.log.get(i).time;
					Settings.peakDuration = time2 - time1;
					Settings.peakPace = Settings.peakDuration / distance;
					break;
				}
			}
		}
		
		Log.v("startRest", "peakIndex=" + Settings.peakIndex + 
				" peakDuration=" + Settings.peakDuration +
				" peakPace=" + Settings.peakPace);

// reset distances for resting based on distance
		Settings.intervalStart = Settings.log.size() - 1;
		if(Settings.intervalStart < 0)
			Settings.intervalStart = 0;
		Settings.prevUpdate = 0;

	}
	
	void updatePace()
	{
//		Log.v("updatePace", "size=" + Settings.log.size());
		int index = Settings.log.size() - 1;



// No log
		if(index < 0)
		{
			Settings.needPace = false;
			return;
		}

// Measure interval distance from current time
		double distance2 = Settings.log.get(index).distance;
		long time2 = Settings.log.get(index).time;
		for(int i = index - 1; i >= 0; i--)
		{
			if(Settings.workUnits == Settings.SECONDS)
			{
				double time1 = Settings.log.get(i).time;
				double duration = time2 - time1;
				if(duration >= Settings.intervalWorkTime)
				{
					double distance1 = Settings.log.get(i).distance;
					double distance = distance2 - distance1;
					double pace = 0;
					
					if(distance > 0)
					{
						pace = duration / distance;
					}

// got a new lower pace
					if(pace < Settings.peakPace)
					{
						Settings.peakDuration = duration;
						Settings.peakIndex = index;
						Settings.peakPace = pace;
					}
					else
					{
						time1 = Settings.log.get(Settings.peakIndex).time;
// no lower pace for last 10 seconds
						if(time2 - time1 > Settings.PACE_LAG)
						{
// seconds per mile
							pace = Main.miToM(Settings.peakPace);
							Settings.db.updateInterval(Settings.peakDuration, 
									Settings.peakPace,
									distance);
// Make GUI update
							Settings.intervalDBChanged = true;
							sayText(new Formatter().format("Total distance %.2f miles.  Split pace %d minutes %d seconds per mile.",
									Main.mToMi(distance),
									(int)(pace / 60),
									(int)(pace % 60)).toString());
							Settings.needPace = false;
						}
					}
					break;

				}
			}
			else
			{
				double distance1 = Settings.log.get(i).distance;
				double distance = distance2 - distance1;
				if(distance >= Settings.intervalWork)
				{
					long time1 = Settings.log.get(i).time;
					double duration = time2 - time1;
					double pace = 0;
					if(distance > 0)
					{
						pace = duration / distance;
					}

	//				Log.v("updatePace", " distance=" + distance +
	//						" duration=" + duration +
	//						" pace=" + pace);
	// got a new lower pace
					if(pace < Settings.peakPace)
					{
						Settings.peakDuration = duration;
						Settings.peakIndex = index;
						Settings.peakPace = pace;
					}
					else
					{
						if(Settings.peakIndex < Settings.log.size()) {
							time1 = Settings.log.get(Settings.peakIndex).time;
							// no lower pace for the last 10 seconds
							if (time2 - time1 > Settings.PACE_LAG) {
								// pace in seconds per mile
								pace = Main.miToM(Settings.peakPace);
								Settings.db.updateInterval(Settings.peakDuration,
										Settings.peakPace,
										distance);
// Make GUI update
								Settings.intervalDBChanged = true;
								sayText(new Formatter().format("%d seconds elapsed.  %d minutes %d seconds per mile.",
										(int) Settings.peakDuration,
										(int) (pace / 60),
										(int) (pace % 60)).toString());
								Settings.needPace = false;
							}
						}
					}

					break;
				}
			}
		}
		
		// seconds per mile
//				sayText(
//						new Formatter().format("Begin rest. Elapsed time %d seconds.", 
//						elapsed / 1000).toString());
	}
	

	// update interval training
	void updateInterval(Location location)
	{
		
		
		if(Settings.intervalState == Settings.COUNTDOWN)
		{
Log.v("x", "Main.updateInterval 1 " + Settings.intervalCountdown);
			if(Settings.intervalCountdown == 0)
			{
				startWork();
			}
			else
			{
//				Log.v("Main", "updateInterval 2 " + Settings.intervalCountdown);
				sayText(new Formatter().format("%d", 
						Settings.intervalCountdown).toString());
				Settings.intervalCountdown--;
// decrease every second
			}
		}
		else
		if(Settings.intervalState == Settings.WORK)
		{
			double distance = getIntervalDistance();
			int elapsed = (int) (Settings.intervalTimer.getTime() / 1000);

//			Log.v("Main", "updateInterval miles=" + 
//				miles + 
//				" time=" +
//				Settings.intervalTimer.getTime() / 1000);

// test for completion			
			if(((Settings.workUnits == Settings.MILES ||
					Settings.workUnits == Settings.METERS) && 
					distance >= Settings.intervalWork) ||
				(Settings.workUnits == Settings.SECONDS &&
					elapsed >= Settings.intervalWorkTime))
			{
				startRest();
			}
			else
			{
// call .5 miles
				switch(Settings.workUnits)
				{
				case Settings.MILES:
				{
					double miles = Main.mToMi(distance);
					int nextUpdate = Settings.prevUpdate + 5;
					Log.v("Main", "updateInterval 1 miles=" + 
							miles + 
							" nextUpdate=" +
							nextUpdate);
					if(nextUpdate > 0 &&
							(int)(miles * 100) >= nextUpdate)
					{
						Settings.prevUpdate = nextUpdate;
//						Log.v("Main", "updateInterval " + strippedFloat(miles));
						sayText(strippedFloat(miles));
					}
					break;
				}
				
				case Settings.METERS:
				{
					int nextUpdate = Settings.prevUpdate + 50;
					Log.v("Main", "updateInterval 1 meters=" + 
							distance + 
							" nextUpdate=" +
							nextUpdate);

					if(nextUpdate > 0 &&
						distance >= nextUpdate)
					{
						Settings.prevUpdate = nextUpdate;
						sayText(new Formatter().format("%d", (int)distance).toString());
					}
					break;
				}
				
				case Settings.SECONDS:
				{
					int nextUpdate = Settings.prevUpdate + 10;
					if(nextUpdate > 0 &&
						elapsed > nextUpdate)
					{
						Settings.prevUpdate = nextUpdate;
						sayText(new Formatter().format("%d", elapsed).toString());
					}
					break;
				}
				
				}
				
				
			}
		}
		else
// rest
		{
			if(Settings.restUnits == Settings.SECONDS)
			{

				long seconds = Settings.intervalTimer.getTime() / 1000;
	//			Log.v("Main", "updateInterval time=" + seconds);
				if(seconds >= Settings.intervalRest - 10 &&
					seconds < Settings.intervalRest)
				{
					sayText(new Formatter().format("%d", Settings.intervalRest - seconds).toString());
				}
				else
				if(seconds >= Settings.intervalRest)
				{
					startWork();
				}
			}
			else
			{
				double distance = getIntervalDistance();
				if(distance >= Settings.intervalRestDistance)
				{
					Settings.intervalCountdown = 10;
					Settings.intervalState = Settings.COUNTDOWN;
				}
				else
				{
// call .5 miles
//					int nextUpdate = Settings.prevUpdate + 5;
//					double miles = Main.mToMi(distance);
//
//					Log.v("Main", "updateInterval 2 miles=" + 
//							miles + 
//							" nextUpdate=" +
//							nextUpdate);
//					if(nextUpdate > 0 &&
//							(int)(miles * 100) >= nextUpdate)
//					{
//						Settings.prevUpdate = nextUpdate;
//						sayText(strippedFloat(miles));
//					}

				}
			}
		}
	}
	
	public void updateLog(Location location) {
		XYZ newXYZ = llh_to_xyz(
				location.getLatitude(), 
				location.getLongitude(), 
				location.getAltitude());
		double distanceDiff = 0;
		if(Settings.prevFineXYZ != null) 
			distanceDiff = Main.distance(newXYZ, Settings.prevFineXYZ);
		
		
		if(Settings.prevFineXYZ == null || 
			distanceDiff >= Settings.FINE_INCREMENT)
		{
			RoutePoint point1 = null;
			double distance = 0;
			if(Settings.log.size() > 0)
			{
				point1 = Settings.log.get(Settings.log.size() - 1);
				distance = point1.distance;
			}
			
			
			RoutePoint point = new RoutePoint();
			point.latitude = location.getLatitude();
			point.longitude = location.getLongitude();
			point.altitude = location.getAltitude();
// get time in UTC
			Calendar c = Calendar.getInstance();
			point.time = c.getTimeInMillis() / 1000;
			point.time -= c.getTimeZone().getRawOffset() / 1000;
			point.relativeTime = Settings.logTimer.getTime() / 1000;
			point.distance = distance;
			
			double distanceDiff2 = 0;
			if(Settings.prevCoarseXYZ != null) 
				distanceDiff2 = Main.distance(newXYZ, Settings.prevCoarseXYZ);
			
			if(Settings.prevCoarseXYZ == null || 
				distanceDiff2 >= Settings.COARSE_INCREMENT)
			{
				Settings.prevCoarseXYZ = newXYZ;
				point.distance = distance + distanceDiff2;

// voice feedback
				int voiceIntervalTotal = (int)(Settings.voicePosition / Settings.voiceInterval);
				double nextVoicePosition = Settings.voiceInterval * (voiceIntervalTotal + 1);

				Log.v("Main", "updateLog distance=" + 
				Main.mToMi(point.distance) +
				" nextVoicePosition=" + Main.mToMi(nextVoicePosition));

				if(point.distance >= nextVoicePosition)
				{
// get point 1 voiceInterval in the past
 					for(int i = Settings.log.size() - 1; i >= 0; i--)
 					{
 						double prevDistance = Settings.log.get(i).distance;
 						double distanceDiff3 = point.distance - prevDistance;
 						

 						if(distanceDiff3 >= Settings.voiceInterval)
 						{
 							long time1 = Settings.log.get(i).relativeTime;
 							double duration = point.relativeTime - time1;
 							double pace = duration / distanceDiff3;
 							
// pace in seconds per mile
 							pace = Main.miToM(pace);
 							String text = new Formatter().format("Total distance %.2f miles.  Split pace %d minutes %d seconds per mile.",
 									Main.mToMi(point.distance),
 									(int)(pace / 60),
 									(int)(pace % 60)).toString();
							Log.v("Main", "updateLog " + text);
 							if(Settings.voiceFeedback &&
 								!Settings.intervalActive)
 							{
 								sayText(text);
 							}
 							
 							break;
 						}
 					}

					Settings.voicePosition = point.distance;
					Settings.saveState(context);

				}
				
				
			}
			
//Log.v("Main", "updateLog " + c.getTimeZone().getRawOffset() / 1000);
			
			
			Settings.log.add(point);
			Settings.prevFineXYZ = newXYZ;
			
			
// append temp file
			if(logTemp == null)
			{
				try {
					logTemp = context.openFileOutput("logTemp", MODE_APPEND);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
			byte [] buffer = new byte[LOG_PACKET_SIZE];
			int offset = 0;
			offset = Settings.write_float32(buffer, offset, (float)point.latitude);
			offset = Settings.write_float32(buffer, offset, (float)point.longitude);
			offset = Settings.write_float32(buffer, offset, (float)point.altitude);
			offset = Settings.write_float32(buffer, offset, (float)point.distance);
			offset = Settings.write_int64(buffer, offset, point.time);
			offset = Settings.write_int64(buffer, offset, point.relativeTime);
			try {
				logTemp.write(buffer, 0, offset);
				logTemp.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}


	static void clearLog(Activity activity)
	{
		if(Settings.log.size() == 0) return;
    	Builder dialog = new AlertDialog.Builder(activity);
    	
    	dialog.setMessage("Really clear log?");
    	dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
    	{
            public void onClick(DialogInterface dialog, int which) 
            {
        		Settings.log.clear();
				Settings.logTimer.reset();
        		Settings.voicePosition = 0.0;
        		Settings.prevFineXYZ = null;
        		Settings.prevCoarseXYZ = null;
        		Settings.saveState(context);
        		
        		if(logTemp != null)
        		{
	        		try {
						logTemp.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
        		}
        		
        		logTemp = null;
        		try
        		{
        			context.openFileOutput("logTemp", 0);
        		}
        		catch (IOException e)
        		{
        			
        		}

        		Main.resetIntervals();
//        		refresh();

            }

        });
    	dialog.setNegativeButton("No", null);
    	dialog.show();
	}


// only called from the map
	static void clearRoute(Activity activity)
	{
		if(Settings.route.size() == 0) return;
    	
//     	Builder dialog = new AlertDialog.Builder(activity);
//     	
//     	dialog.setMessage("Really clear route?");
//     	dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
//     	{
//             public void onClick(DialogInterface dialog, int which) 
//             {
//                 Settings.route.clear();
//                 Settings.currentRoute = null;
//                 Settings.save(context);
// //                refresh();
//             }
// 
//         });
//     	dialog.setNegativeButton("No", null);
//     	dialog.show();

        Settings.route.clear();
        Settings.currentRoute = null;
        Settings.save(context);
        ((Map)activity).refresh();
		
	}
	
	static synchronized LatLng getPosition()
	{
		if(haveLocation /* && 
				(Settings.keepAlive || 
				Settings.followPosition ||
				Settings.recordRoute) */)
		{
			return new LatLng(location.getLatitude(), location.getLongitude());
		}
		else
		{
			return new LatLng(Settings.latitude, Settings.longitude);
			
		}
	}
	
	
	// not getting called
//	@Override
//	public void onGpsStatusChanged(int event) {
//		Log.v("Main", "onGpsStatusChanged event=" + event);
//		
//		
//		switch(event)
//		{
//        case GpsStatus.GPS_EVENT_FIRST_FIX:
//            Log.v("Main", "onGpsStatusChanged GPS_EVENT_FIRST_FIX");
//            break;
//
//        case GpsStatus.GPS_EVENT_STARTED:
//            Log.v("Main", "onGpsStatusChanged GPS_EVENT_STARTED");
//            break;
//
//        case GpsStatus.GPS_EVENT_STOPPED:
//            Log.v("Main", "onGpsStatusChanged GPS_EVENT_STOPPED");
//            break;
//
//        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
//			GpsStatus status = locationManager.getGpsStatus(null);
//
//            int maxSatellites = status.getMaxSatellites();
//
//            Iterator<GpsSatellite> it = status.getSatellites().iterator();
//            int count = 0;
//
//            while (it.hasNext() && count <= maxSatellites)
//            {
//                it.next();
//                count++;
//            }
//
////            Log.v("Main", "onGpsStatusChanged GPS_EVENT_SATELLITE_STATUS satelliteCount=" + satelliteCount);
//			break;
//		}
//	}
//

    static public boolean onOptionsItemSelected(Activity activity,
    		MenuItem item) 
    {
    	switch (item.getItemId()) 
    	{
//        case R.id.menu_settings: {
//			Intent i = new Intent(activity, SettingsWin.class);
//			//i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
//			activity.startActivity(i);
//			return true;
//		}

        case R.id.menu_map:
        	activity.startActivity(new Intent(activity, Map.class));
        	return true;

        case R.id.menu_interval: {
			Intent i = new Intent(activity, IntervalTraining.class);
			activity.startActivity(i);
			return true;
		}

		case R.id.menu_savelog:
			FileSelect.nextWindow = SettingsWin.class;
			Main.saveLog();
			break;

    	case R.id.menu_clearlog:
    		clearLog(activity);
    		break;

// only called from the Map
		case R.id.menu_load:
   	        Settings.selectLoad = true;
			FileSelect.nextWindow = Map.class;
       	    activity.startActivity( new Intent(Main.context, FileSelect.class));
			break;

// only called from the Map
   	    case R.id.menu_clearroute:
   		    clearRoute(activity);
   		    break;

        default:
        	return false;
    	}
    	
    	return false;
    }
    
    
    static void handleIntervalButton()
    {
		if(Settings.intervalActive)
		{
	//pause
			if(Settings.intervalState == Settings.REST)
			{
				Main.sayText("Rest paused");
			}
			else
			{
				Main.sayText("Workout paused");
			}
			
			Settings.intervalActive = false;
			Settings.intervalTimer.stop();
			Settings.saveState(Main.context);
		}
		else
		{
			Log.v("IntervalTraining", "onClick db size=" + 
					Settings.db.totalIntervals() + " timer=" +
					Settings.intervalTimer.getTime());
			// start recording log
			startRecording();

			if(Settings.intervalState == Settings.COUNTDOWN ||
					Settings.db.totalIntervals() == 0 &&
					Settings.intervalTimer.getTime() < 1000 &&
					Main.getIntervalDistance() < 0.01)
			{
	//begin 1st workout

	    		Log.i("IntervalTraining", "onClick COUNTDOWN");
				Settings.intervalState = Settings.COUNTDOWN;
				Settings.intervalCountdown = 10;
			}
			else
			{
	    		if(Settings.intervalState == Settings.REST)
	    		{
	//resume rest
	    			Main.sayText("Resume rest");
	    		}
	    		else
	    		{
	//resume workout
	    			Main.sayText("Resume workout");
	    		}
			}
			
			Settings.intervalActive = true;
			Settings.intervalTimer.start();
//			Settings.enableService = true;
//			Settings.needRestart = true;
			Settings.save(Main.context);
			Settings.saveState(Main.context);
		}
    }
    
    
    
    static double mToMi(double m)
    {
    	return m * 0.621364 / 1000;
    }

    static double miToM(double mi)
    {
    	return mi * 1000 / 0.621364;
    }
    
    static double sqr(double x)
    {
    	return x * x;
    }
    
    // print float with no leading or trailing 0's
    String strippedFloat(double miles)
    {
// text to speech can't parse it without the leading 0
    	String result = new Formatter().format("%.2f", 
			miles).toString();
    	
    	return result;
    	
//    	int start = 0;
//    	int end = result.length();
//		for(int i = 0; i < end; i++)
//		{
//			if(result.charAt(i) == '0') 
//				start = i + 1;
//			else
//				break;
//		}
//		for(int i = end - 1; i >= 0; i--)
//		{
//			if(result.charAt(i) == '0')
//				end = i;
//			else
//				break;
//		}
//		
//		if(start >= end - 1)
//			return "0";
//		
//		return result.substring(start, end);
    }
    
    
    static double distance(XYZ point1, XYZ point2)
    {
    	return Math.sqrt(sqr(point1.x - point2.x) +
    			sqr(point1.y - point2.y) +
    			sqr(point1.z - point2.z));
    			
    }
    
    
    
    
// http://www.oc.nps.edu/oc2902w/coord/llhxyz.htm
 	static void radcur(double EARTH_A, double EARTH_B, double[] rrnrm, double lati)
 	/*
 	   compute the radii at the geodetic latitude lat (in degrees)
 	   input:  lat       geodetic latitude in degrees
 	   output: rrnrm     an array 3 long
 	                     r,  rn,  rm   in km
 	*/
 	{
 		double dtr   = Math.PI / 180.0;

 		double  a, b, lat;
 		double  asq, bsq, eccsq, ecc, clat, slat;
 		double  dsq, d, rn, rm, rho, rsq, r, z;

 	   //        -------------------------------------

 		a     = EARTH_A;
 		b     = EARTH_B;

 		asq   = a * a;
 		bsq   = b * b;
 		eccsq  =  1.0 - bsq / asq;
 		ecc = Math.sqrt(eccsq);

 		lat   =  lati;

 		clat  =  Math.cos(dtr * lat);
 		slat  =  Math.sin(dtr * lat);

 		dsq   =  1.0 - eccsq * slat * slat;
 		d     =  Math.sqrt(dsq);

 		rn    =  a / d;
 		rm    =  rn * (1.0 - eccsq ) / dsq;

 		rho   =  rn * clat;
 		z     =  (1.0 - eccsq ) * rn * slat;
 		rsq   =  rho * rho + z * z;
 		r     =  Math.sqrt( rsq );

 		rrnrm[0]  =  r;
 		rrnrm[1]  =  rn;
 		rrnrm[2]  =  rm;


 	//printf("radcur %d %f\n", __LINE__, EARTH_B);


 	}






 // All units in meters.  Latitude, longitude in degrees
 	static XYZ llh_to_xyz(
 			RoutePoint point)
 	{
 		return llh_to_xyz(point.latitude, 
 				point.longitude, 
 				point.altitude);
 		
 	}
 	
 	static XYZ llh_to_xyz(
 		double latitude, 
 		double longitude, 
 		double altitude)
 	{
 		XYZ result = new XYZ();
// convert altitude to km from m
// 		altitude /= 1000;
// ignore altitude for distance calculations
		altitude = 0;
 		
 		// used by distance calculation	
 		double EARTH_A;
 		double EARTH_B;
 		double EARTH_F;
 		double EARTH_Ecc;
 		double EARTH_Esq;

 		
 	// Earth constants
 	    double wgs84a, wgs84b, wgs84f;

 	    wgs84a = 6378.137;
 	    wgs84f = 1.0 / 298.257223563;
 	    wgs84b = wgs84a * ( 1.0 - wgs84f );


 		double  f, ecc, eccsq, a, b;

 	    a        =  wgs84a;
 	    b        =  wgs84b;

 	    f        =  1 - b / a;
 	    eccsq    =  1 - b * b / (a * a);
 	    ecc      =  Math.sqrt(eccsq);

 	    EARTH_A  =  a;
 	    EARTH_B  =  b;
 	    EARTH_F  =  f;
 	    EARTH_Ecc=  ecc;
 	    EARTH_Esq=  eccsq;

 		
 	    double dtr =  Math.PI / 180.0;
 	    double flat, flon;
 	    double clat, clon, slat, slon;
 	    double[] rrnrm = new double[3];
 	    double rn, esq;

 	    clat = Math.cos(dtr * latitude);
 	    slat = Math.sin(dtr * latitude);
 	    clon = Math.cos(dtr * longitude);
 	    slon = Math.sin(dtr * longitude);

 	    radcur (EARTH_A, EARTH_B, rrnrm, latitude);
 	    rn = rrnrm[1];
 	    double re = rrnrm[0];

 	    ecc = EARTH_Ecc;
 	    esq = ecc * ecc;

 	//printf("llh_to_xyz %d %f\n", __LINE__, rn);


 	    result.y = (rn + altitude) * 
 			clat * 
 			slon;
 	    result.x = (rn + altitude) * 
 			clat * 
 			clon;
 	    result.z = ( (1 - esq) * rn + altitude ) * slat;

 	    result.x *= 1000;
 	    result.y *= 1000;
 	    result.z *= 1000;
 	    return result;
 	}
 	

	static public double toRad(double angle)
	{
		return angle * 2 * Math.PI / 360;
	}
	
	static public double fromRad(double angle)
	{
		return angle * 360 / Math.PI / 2;
	}
	
 	static RoutePoint xyz_to_llh(
		double x, 
		double y, 
		double z)
	{
 		RoutePoint result = new RoutePoint();
 		double EMAJOR = ((double)6378137.0);
 		double EFLAT = ((double)0.00335281068118 );
 		double ZERO = ((double)0.0);
 		double ONE = ((double)1.0);
 		double TWO = ((double)2.0);
 		double THREE = ((double)3.0);
 		double FOUR = ((double)4.0);
 		double FIVE = ((double)5.0);

 		
		double B = EMAJOR * (ONE - EFLAT);
		if(z < 0) B = -B;
	
	
	    double r = Math.sqrt( x * x + y * y );
	    double e = ( B * z - (EMAJOR * EMAJOR - B * B) ) / ( EMAJOR * r );
	    double f = ( B * z + (EMAJOR * EMAJOR - B * B) ) / ( EMAJOR * r );
	
	    double p = (FOUR / THREE) * (e * f + ONE);
	    double q = TWO * (e * e - f * f);
	    double d = p * p * p + q * q;
	
		double v;
	    if( d >= ZERO ) 
		{
	            v = Math.pow( (Math.sqrt( d ) - q), (ONE / THREE) )
	            	- Math.pow( (Math.sqrt( d ) + q), (ONE / THREE) );
	    } 
		else 
		{
	            v = TWO * Math.sqrt( -p )
	            	* Math.cos( Math.acos( q/(p * Math.sqrt( -p )) ) / THREE );
	    }
	
	    if( v * v < Math.abs(p) ) 
		{
	            v = -(v * v * v + TWO * q) / (THREE * p);
	    }
	
	    double g = (Math.sqrt( e * e + v ) + e) / TWO;
	    double t = Math.sqrt( g * g  + (f - v * g) / (TWO * g - e) ) - g;
	
	    result.latitude = Math.atan( (EMAJOR * (ONE - t * t)) / (TWO * B * t) );
	    result.altitude = (r - EMAJOR * t) * 
	    	Math.cos(result.latitude) + 
	    	(z - B) * 
	    	Math.sin(result.latitude);
	
	    double zlong = Math.atan2( y, x );
	
	    result.longitude = zlong;
	    result.latitude = fromRad(result.latitude);
	    result.longitude = fromRad(result.longitude);
	    return result;
	}

 	
 	// distance between 2 points in meters
 	double getDistance(RoutePoint point1, RoutePoint point2)
 	{
 		XYZ result1 = llh_to_xyz(point1.latitude,
				point1.longitude,
				point1.altitude);
 		XYZ result2 = llh_to_xyz(point2.latitude, 
 				point2.longitude, 
 				point2.altitude);

 		return Math.sqrt(Main.sqr(result1.x - result2.x) +
 				Main.sqr(result1.y - result2.y) +
 				Main.sqr(result1.z - result2.z));
 				
 	}

 	// returns distance of the route in meters
 	static double getDistance(Vector<RoutePoint> route)
 	{
 		if(route.size() < 2) return 0;

// No distance field for the user edited route
// 		return route.get(route.size() - 1).distance;

 		double total = 0;
 		if(route.size() < 2) return 0;
 		RoutePoint routePoint = route.get(0);
 		XYZ prev = llh_to_xyz(routePoint.latitude, 
					routePoint.longitude, 
					routePoint.altitude);
 		
 		for(int i = 1; i < route.size(); i++)
 		{
 			routePoint = route.get(i);
 			
 			XYZ next = llh_to_xyz(routePoint.latitude, 
 					routePoint.longitude, 
 					routePoint.altitude);
 			double increment = distance(prev, next);
 			
 			if(increment >= Settings.COARSE_INCREMENT)
 			{
 				total += increment;
 				prev.copy(next);
 			}
 		}
 		
 		return total;
 	}

	static void saveRoute(String path, Vector<RoutePoint> route)
	{
        BufferedWriter fd = null;

        Log.v("Main", "saveRoute " + path);
        
        if(!Settings.saveGPX)
        {
	        if(path.indexOf(".kml") < 0)
	        {
	        	path = path + ".kml";
	        }
        }
        else
        {
	        if(path.indexOf(".gpx") < 0)
	        {
	        	path = path + ".gpx";
	        }
        }

        
        
        
		try {
			fd = new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(fd != null)
		{
			String header = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
"<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" + 
"<Document>\n" + 
"	<StyleMap id=\"msn_ylw-pushpin\">\n" + 
"		<Pair>\n" + 
"			<key>normal</key>\n" + 
"			<styleUrl>#sn_ylw-pushpin</styleUrl>\n" + 
"		</Pair>\n" + 
"		<Pair>\n" + 
"			<key>highlight</key>\n" + 
"			<styleUrl>#sh_ylw-pushpin</styleUrl>\n" + 
"		</Pair>\n" + 
"	</StyleMap>\n" + 
"	<Style id=\"sh_ylw-pushpin\">\n" + 
"		<IconStyle>\n" + 
"			<scale>1.3</scale>\n" + 
"			<Icon>\n" + 
"				<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>\n" + 
"			</Icon>\n" + 
"			<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>\n" + 
"		</IconStyle>\n" + 
"		<LineStyle>\n" + 
"			<color>ff00ffff</color>\n" + 
"			<width>3</width>\n" + 
"		</LineStyle>\n" + 
"	</Style>\n" + 
"	<Style id=\"sn_ylw-pushpin\">\n" + 
"		<IconStyle>\n" + 
"			<scale>1.1</scale>\n" + 
"			<Icon>\n" + 
"				<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>\n" + 
"			</Icon>\n" + 
"			<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>\n" + 
"		</IconStyle>\n" + 
"		<LineStyle>\n" + 
"			<color>ff0000ff</color>\n" + 
"			<width>3</width>\n" + 
"		</LineStyle>\n" + 
"	</Style>\n" + 
"	<Style id=\"dot\">\n" + 
"	<IconStyle>\n" + 
"	  <Icon>\n" + 
"	    <href>http://heroinewarrior.com/dot.png</href>\n" + 
"	  </Icon>\n" + 
"	</IconStyle>\n" + 
"	</Style>\n" + 
"	<Placemark>\n" + 
"	<name>route</name>\n" +
"		<styleUrl>#msn_ylw-pushpin</styleUrl>\n" + 
"		<LineString>\n" + 
"			<tessellate>1</tessellate>\n" + 
"				<coordinates>\n" +
"\n";
			
			String footer = 
"					</coordinates>\n" +
"			</LineString>\n" +
"		</Placemark>\n";
			
			String header2 = 
"<Folder>\n" +
"<name>Real time</name>\n" +
"<open>0</open>\n";

			String header3 = 
"<Folder>\n" +
"<name>Elapsed time</name>\n" +
"<open>0</open>\n";

			String footer2 = 
"</Folder>\n";

			String footer3 = 
"</Document>\n" +
"</kml>\n" +
"\n";
				
			String gpx_header = 
				"<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
				"<gpx>\n" +
                "<trk>\n" + 
                "<name><![CDATA[route]]></name>\n" +
                "<desc><![CDATA[This is track no: 1]]></desc>\n" +
                "<number>1</number>\n" +
                "<trkseg>\n";
			
			String gpx_footer =
				"</trkseg>\n" +
                "</trk>\n" +
                "</gpx>\n";

			try
			{
				if(!Settings.saveGPX) fd.write(header);
				for(int i = 0; i < route.size(); i++)
				{
					Formatter format = new Formatter();
					RoutePoint point = route.get(i);
					format.format("%.9f,%.9f,%.1f\n",
							point.longitude,
							point.latitude,
							point.altitude);
							
					fd.write(format.toString());
				}
				fd.write(footer);

				
				fd.write(header2);
				
				Calendar begin = Calendar.getInstance();
				Calendar end = Calendar.getInstance();

				for(int i = 0; i < route.size(); i++)
				{
					Formatter format = new Formatter();
					RoutePoint point = route.get(i);
					
					begin.setTimeInMillis(point.time * 1000);
					if(i < route.size() - 1)
					{
						RoutePoint point2 = route.get(i + 1);
						end.setTimeInMillis(point2.time * 1000);
					}
					else
					{
						end.setTimeInMillis(point.time * 1000 + 1000);
					}
					
					
					format.format("<Placemark><TimeSpan><begin>%04d-%02d-%02dT%02d:%02d:%02dZ</begin>" +
							"<end>%04d-%02d-%02dT%02d:%02d:%02dZ</end></TimeSpan><styleUrl>#dot</styleUrl>",
							begin.get(Calendar.YEAR),
							begin.get(Calendar.MONTH) + 1,
							begin.get(Calendar.DAY_OF_MONTH),
							begin.get(Calendar.HOUR_OF_DAY),
							begin.get(Calendar.MINUTE),
							begin.get(Calendar.SECOND),
							end.get(Calendar.YEAR),
							end.get(Calendar.MONTH) + 1,
							end.get(Calendar.DAY_OF_MONTH),
							end.get(Calendar.HOUR_OF_DAY),
							end.get(Calendar.MINUTE),
							end.get(Calendar.SECOND));
//					format.format("<Placemark><TimeSpan><begin>%d</begin><end>%d</end></TimeSpan><styleUrl>#dot</styleUrl>",
//							point.time,
//							point.time + 1);
					format.format("<Point><coordinates>%f,%f,%f</coordinates></Point>",
							point.longitude,
							point.latitude,
							point.altitude);
					format.format("<Elapsed>%d</Elapsed>", point.relativeTime);
					
					format.format("</Placemark>\n");
					
					fd.write(format.toString());
				}


				fd.write(footer2);
		        fd.write("\n");
				
// save interval training	
		        SQLiteDatabase db = Settings.db.getWritableDatabase();
		        Cursor cursor = db.rawQuery("SELECT * FROM " + IntervalDB.TABLE_NAME, null);
		        int total = 1;
		        if (cursor.moveToFirst()) 
				{
					fd.write("<Intervals>\n#\tdist\tsec\tmin/mile\n");
		        	do 
					{
		        		double duration = cursor.getFloat(1);
		        		double distance = cursor.getFloat(3);
//		        		Log.v("IntervalTraining", "dbToList duration=" + duration);
		        		double pace = cursor.getFloat(2);
		        		// convert to seconds per mile
		        		pace = Main.miToM(pace);
		        		Formatter format = new Formatter();
		        		
		        		format.format("%d\t%d\t%d\t%d:%d\n",
		        				total,
								(int)distance,
		        				(int)duration,
		        				(int)(pace / 60),
		        				(int)(pace % 60));
		        		fd.write(format.toString());
		        		
		        		total++;
		            } while (cursor.moveToNext());
					fd.write("</Intervals>\n");
		        }

		        fd.write("\n");

		        fd.write("\n");
				
			
				fd.write(footer3);
		        fd.write("\n");
		        fd.write("\n");
		        fd.write("\n");

				
				
				fd.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			

		}
	}


/// entry point from GUI
//	static void saveRoute(boolean isGPX)
//	{
//
//		Settings.saveGPX = isGPX;
//		Settings.selectLoad = false;
//		Settings.selectSave = false;
//		Settings.selectSaveLog = true;
//// create a default filename
//		if(FileSelect.selectedFile == null ||
//				FileSelect.selectedFile.length() == 0)
//		{
//			if(Settings.log.size() > 0)
//			{
//				RoutePoint point = Settings.log.get(Settings.log.size() - 1);
//				Calendar end = Calendar.getInstance();
//				end.setTimeInMillis(point.time * 1000);
//				Formatter format = new Formatter();
//				format.format("%04d_%02d_%02dT%02d_%02d_%02dZ.%s",
//						end.get(Calendar.YEAR),
//						end.get(Calendar.MONTH) + 1,
//						end.get(Calendar.DAY_OF_MONTH),
//						end.get(Calendar.HOUR_OF_DAY),
//						end.get(Calendar.MINUTE),
//						end.get(Calendar.SECOND),
//						Settings.getSaveExtension());
//				String filename = format.toString();
//				FileSelect.selectedFile = filename;
//			}
//		}
//
//
//		Intent i = new Intent(Main.context, FileSelect.class);
//		i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
//		Main.context.startActivity(i);
//	}

//	static void saveRoute(String path)
//    {
//		saveRoute(path, Settings.route);
//	}




// entry point for saving the log
	static void saveLog() {
		sayText("Saving workout");

// create the filename from the last timestamp
		RoutePoint point = Settings.log.get(Settings.log.size() - 1);
		Calendar end = Calendar.getInstance();
		end.setTimeInMillis(point.time * 1000);
		Formatter format = new Formatter();
		format.format("%04d_%02d_%02dT%02d_%02d_%02dZ.%s",
				end.get(Calendar.YEAR),
				end.get(Calendar.MONTH) + 1,
				end.get(Calendar.DAY_OF_MONTH),
				end.get(Calendar.HOUR_OF_DAY),
				end.get(Calendar.MINUTE),
				end.get(Calendar.SECOND),
				Settings.getSaveExtension());
		String filename = Settings.dir + "/" + format.toString();
// save it
		saveRoute(filename, Settings.log);
    	sayText("workout saved");

//		sayText("Saving workout");
//    	saveRoute(path, Settings.log);
//    	sayText("workout saved");
	}
    
	public static void startLog() 
	{
		// just started
//		Settings.needRestart = true;
//		Settings.enableService = true;
		FileSelect.selectedFile = null;
		Settings.logTimer.start();
		
		
// want pause to resume from previous point
//		Settings.prevFineXYZ = null;
//		Settings.prevCoarseXYZ = null;

	}

 	static void sayText(String text)
 	{
 		if(ttsReady)
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
//Log.i("sayText", text);
			tts.speak(text, TextToSpeech.QUEUE_ADD, params);
		}
 	}



	@Override
	public void onInit(int status) 
	{
		// text to speech
		if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
			ttsReady = true;
 
        }
	}

	static public void printBuffer(byte[] buffer, int offset, int bytes)
	{
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		for(int i = 0; i < bytes; i++) 
		{
		formatter.format("%02x ", buffer[i + offset]);
		}
		Log.v("printBuffer", sb.toString());
	}

	static void updateBluetooth(String text)
	{
		bluetoothStatus = text;
	}
	
}
