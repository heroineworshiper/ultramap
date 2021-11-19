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

// data is 9600 baud 4Hz NAV-SOL packets

// useful configuration commands for bluetooth:
// at+version
// AT+VERSION
// at+namemarcy2-px4flow
// at+namemarcy2-lidar
// AT+NAMEgps_bluetooth
// AT+NAMEsensor

// new devices start at 9600 baud.  Be sure to set the initial baud rate 
// to 9600, then 115200 after configuration.
// Some kind of delay in the terminal program is required to paste text in,
// but the entire command must be sent in under a second.

package com.ultramap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.os.Message;
import android.util.Log;

public class ExternalClient extends Thread
{
	public ExternalClient()
	{
	}
	
	public void run() 
	{
		initializeBluetooth();
		

    	while(true)
    	{
		    Callable<Integer> readTask = new Callable<Integer>() {
		        public Integer call() throws Exception {
		            return input.read(newData, 0, newData.length);
		        }
		    };
			

		    newBytes = 0;
		    Future<Integer> future = executor.submit(readTask);
		    try {
		    	newBytes = future.get(1000, 
		    			TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				e.printStackTrace();
			}

		    
//		    Log.v("ExternalClient", "run");
//		    Main.printBuffer(newData, 0, newBytes);


			for(int i = 0; i < newBytes; i++)
			{
				byte c = newData[i];
				final byte[] startCode = 
				{
					(byte) 0xb5,
					0x62,
					0x01,
					0x06,
					52,
					0x0
				};
				
				switch(bytes)
				{
					case 0:
					case 1:
					case 2:
					case 3:
					case 4:
					case 5:
						if(c == startCode[bytes])
						{
							packet[bytes++] = c;
						}
						break;
						
					default:
						packet[bytes++] = c;

						if(bytes >= 52 + 8)
						{
							double x = (double)Settings.read_int32(packet, 12 + 6) / 100;
							double y = (double)Settings.read_int32(packet, 16 + 6) / 100;
							double z = (double)Settings.read_int32(packet, 20 + 6) / 100;
							double accuracy = Settings.read_int32(packet, 24 + 6) / 100;
//							Log.v("ExternalClient", 
//								"run " + x_i + 
//								" " + y_i +
//								" " + z_i +
//								" " + accuracy_i);
							
							
							synchronized(this)
							{
								RoutePoint point = Main.xyz_to_llh(
										x, 
										y, 
										z);
								accum.longitude += point.longitude;
								accum.latitude += point.latitude;
								accum.altitude += point.altitude;
								this.accuracy += accuracy;
								totalReadings++;
//								Log.v("ExternalClient", 
//										"run totalReadings=" + totalReadings);

							}
							
							
							bytes = 0;
						}
						break;
				}
				
			}	
		}
	}
	
	
	public Location getLastLocation() 
	{
		Location result = new Location("");
		
		synchronized(this)
		{
			if(totalReadings >= 4)
			{
				result.setLatitude(accum.latitude / totalReadings);
				result.setLongitude(accum.longitude / totalReadings);
				result.setAltitude(accum.altitude / totalReadings);
				result.setAccuracy((float)accuracy / totalReadings);

				Log.v("ExternalClient", 
						"getLastLocation " +
						" " + totalReadings +
						" " + result.getLatitude() + 
						" " + result.getLongitude() +
						" " + result.getAltitude() +
						" " + result.getAccuracy());

				
				totalReadings = 0;
				accuracy = 0;
				accum.latitude = 0;
				accum.longitude = 0;
				accum.altitude = 0;
			}
			else
			{
				result = null;
			}
		}

		
		return result;
	}
	

	boolean initializeBluetooth()
	{
		 
		
		
    	boolean result = true;
    	
    	while(result == true)
    	{
    		result = false;
    		
    		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    		if (mBluetoothAdapter == null) 
			{
//    			alert("initialize: no bluetooth");
    			result = true;
    		    Log.v("initializeBluetooth", "no bluetooth");
				Main.updateBluetooth("no bluetooth");
    		}
    		
    		
    		
    		if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
//    			alert("initialize: bluetooth not enabled");
    			result = true;
    			Log.v("initializeBluetooth", "bluetooth not enabled");
    			Main.updateBluetooth("bluetooth not enabled");
    		}
    		else
    		if(mBluetoothAdapter != null)
    		{
    			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//    			Log.v("Copter", "initialize: pairedDevices.size()=" + pairedDevices.size());
    			// If there are paired devices
    			if (pairedDevices.size() > 0) {
    			    // Loop through paired devices
    			    for (BluetoothDevice device : pairedDevices) {
    			        // Add the name and address to an array adapter to show in a ListView
    			        Log.v("initializeBluetooth", device.getName());
    			        if(device.getName().equals(Settings.BLUETOOTH_ID))
    			        {
    			        	 BluetoothSocket tmp = null;
				        	try {
				                tmp = device.createRfcommSocketToServiceRecord(uuid);
				            } catch (IOException e) { }

    	
    			        	 mmSocket = tmp;
    			        }
    			    }
    			}
    			
    			
    			if(mmSocket != null)
    			{
//    				start();
    				result = false;
    			}
    			else
    			{
//    				alert("initialize: no bluetooth device found");
    				Log.v("initializeBluetooth", "no bluetooth device found");
    				Main.updateBluetooth("no bluetooth device found");

//    				Message message = Message.obtain(WindowBase.handler, 2, activity);
//    				message.getData().putString("text", "No bluetooth device found\n");
//    				WindowBase.handler.sendMessage(message);

    				result = true;
    			}
    		}

    		
    		
    		if(result == false)
	    	{
	
		        try {
		            // Connect the device through the socket. This will block
		            // until it succeeds or throws an exception
		            mmSocket.connect();
		        } catch (IOException connectException) {
		            // Unable to connect; close the socket and get out
		            try {
		                mmSocket.close();
		            } catch (IOException closeException) 
		            { 
		            	
		            }
		            
		            connectException.printStackTrace();
//					alert("run: couldn't connect to bluetooth device");
					Log.v("initializeBluetooth", "connect failed ");
					Main.updateBluetooth("connect failed");
//			        printAlert("Couldn't connect to bluetooth device\n");
			        result = true;
		        }
		 
		        
		        try {
					output = mmSocket.getOutputStream();
				} catch (IOException e) {
//					alert("run: couldn't get bluetooth ostream");
					Log.v("initializeBluetooth", "ostream failed");
					Main.updateBluetooth("ostream faile)d");
					e.printStackTrace();
					result = true;
				}
		        
		        try {
					input = mmSocket.getInputStream();
				} catch (IOException e) {
//					alert("run: couldn't get bluetooth istream");
					Log.v("initializeBluetooth", "istream failed");
					Main.updateBluetooth("istream failed");
					e.printStackTrace();
					result = true;
				}
	        
	    	}
    		
    		
    		if(result == true)
    		{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

    		}
    	}
    	
    	
        
      	Log.v("initializeBluetooth", "Got bluetooth connection\n");
      	Main.updateBluetooth("Got bluetooth connection");
	
		return false;
	}


	UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket mmSocket = null;
	InputStream input = null;
    OutputStream output = null;
	byte[] newData = new byte[1024];
	int newBytes = 0;
	byte[] packet = new byte[1024];
	int bytes = 0;
	RoutePoint accum = new RoutePoint();
	double accuracy;
	int totalReadings;
	ExecutorService executor = Executors.newFixedThreadPool(2);

}






