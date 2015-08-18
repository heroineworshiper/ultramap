package com.ultramap;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Compass implements SensorEventListener
{
	
	void initialize(Context context)
	{
		SensorManager mSensorManager = 
				(SensorManager) context.getSystemService(
						Context.SENSOR_SERVICE);

		Sensor sensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, 
				sensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		sensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, 
				sensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		
	}



	// accelerometer
	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	// http://www.damonkohler.com/2010/06/better-orientation-readings-in-android.html
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType())
		{
		case Sensor.TYPE_MAGNETIC_FIELD:
			magValues = event.values.clone();
			break;
		case Sensor.TYPE_ACCELEROMETER:
			accelValues = event.values.clone();
			break;
		}
		
		
		if(magValues != null && accelValues != null)
		{
	        float[] R = new float[16];
	        SensorManager.getRotationMatrix(R, 
	        		null, 
	        		accelValues, 
	        		magValues);
	        float[] orientation = new float[3];
	        SensorManager.getOrientation(R, orientation);
	        
	        synchronized(this)
	        {
	        	double newHeading = orientation[0];
	        	newHeading = newHeading * 360 / 2 / Math.PI;
//	        	Log.v("Compass", "onSensorChanged newHeading=" + newHeading +
//	        			" heading=" + heading);
	        	if(heading < -90 && newHeading > 90) newHeading -= 360;
	        	if(heading > 90 && newHeading < -90) newHeading += 360;
	        	
	        	double bandwidth = 0.1;
	        	heading = newHeading * bandwidth + heading * (1.0 -bandwidth);
	        	if(heading < -180) heading += 360;
	        	if(heading > 180) heading -= 360;
	        	pitch = orientation[1];
	        	roll = orientation[2];
	        }
	        
//	        Log.v("Compass", "onSensorChanged: " + heading * 360 / 2 / Math.PI);
		}
	}

	synchronized double getHeading()
	{
		return heading;
	}

	float [] magValues = null;
	float [] accelValues = null;
	double heading;
	float roll;
	float pitch;
}
