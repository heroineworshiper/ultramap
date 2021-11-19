package com.ultramap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.google.android.gms.maps.MapsInitializer;
import com.ultramap.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class Map extends WindowBase implements OnMapClickListener, OnCameraChangeListener, OnMarkerDragListener, OnMarkerClickListener 
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Main.initialize(this);
        setContentView(R.layout.map);
        
        MapsInitializer.initialize(getApplicationContext());
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
        Log.v("Map", "onCreate map=" + map);
        if(map != null)
        {
	        map.setOnMapClickListener(this);
	        map.setOnCameraChangeListener(this);
	        map.setOnMarkerDragListener(this);
	        map.setOnMarkerClickListener(this);
	        map.getUiSettings().setMyLocationButtonEnabled(true);
	        map.getUiSettings().setZoomControlsEnabled(false);
	        map.setMyLocationEnabled(true);
	        map.setMapType(Settings.mapType);
	
	
	        map.moveCamera(CameraUpdateFactory.newCameraPosition(
	            	new CameraPosition(
	                	  	new LatLng(Settings.latitude, Settings.longitude), 
	                	   	(float) Settings.zoom, 
	                	   	0, 
	                	   	0)));
        }
        
        View view = findViewById(R.id.map_pause_layout);
        if(view != null) view.bringToFront();
        
        updateButtonText();
        Button button = (Button)findViewById(R.id.map_pause);
        if(button != null) 
        {
//        	button.setVisibility(View.GONE);
        }

        if(redMarker == null) redMarker = BitmapDescriptorFactory.fromResource(R.raw.marker32x32);
        if(greenMarker == null) greenMarker = BitmapDescriptorFactory.fromResource(R.raw.marker32x32green);
        if(headingBitmap == null) headingBitmap = BitmapDescriptorFactory.fromResource(R.raw.cursor);

        handleFileLoad();

        

//         if(Settings.route.size() > 0 && Settings.editRoute)
//        	{
//        		editRoute();
//        	}
    }

    
	public void onResume() {
		super.onResume();
        map.setMapType(Settings.mapType);
        updateButtonText();
        
        if(!handleFileLoad())
        {
            refresh();
        }
	}

// returns true if it loaded a file
    public boolean handleFileLoad()
    {
        boolean result = false;
// A route was selected
       Log.v("Map", "handleFileLoad Settings.currentRoute=" + Settings.currentRoute);
       Log.v("Map", "handleFileLoad FileSelect.success=" + FileSelect.success +
       		" FileSelect.selectedFile=" + FileSelect.selectedFile);
       if(FileSelect.success &&
       		FileSelect.selectedFile != null)
       {
            if(Settings.selectLoad)
            {
                result = loadRoute(Settings.dir + "/" + FileSelect.selectedFile);
            }
//        	else
//        	if(Settings.selectSave)
//        	{
//        		Main.saveRoute(Settings.dir + "/" + FileSelect.selectedFile);
//        	}
//        	else
//        	if(Settings.selectSaveLog)
//        	{
//        		Main.saveLog(Settings.dir + "/" + FileSelect.selectedFile);
//        	}
       }
       else
       {
// load default route
       	   if(Settings.currentRoute != null)
       	   {
       		   result = loadRoute(Settings.currentRoute);
       	   }
       }
       Settings.selectLoad = false;
//       Settings.selectSave = false;
//       Settings.selectSaveLog = false;


        return result;
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	this.menu = menu;
        getMenuInflater().inflate(R.menu.map_menu, menu);
        
        
//        if(Settings.editRoute)
//        {
//        	MenuItem item = menu.findItem(R.id.menu_editroute);
//        	item.setChecked(true);
//        }
        
//        if(Settings.recordRoute)
//        {
//        	MenuItem item = menu.findItem(R.id.menu_recordlog);
//        	item.setChecked(true);
//        }
        
        
        
        
        return true;
    }

    public void onClick(View view)
    {
    	
    	
        switch (view.getId()){
        case R.id.map_pause:
        	toggleRecording();
        	updateButtonText();
        	break;
        }
	}
	
	
// TODO: make menu change for editing a route
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch(item.getItemId())
    	{
    	
//    	case R.id.menu_deletepoint:
//    		deletePoint();
//    		break;
//
//        case R.id.menu_showroute:
//   	        Settings.selectLoad = true;
//    		Settings.selectSave = false;
//    		Settings.selectSaveLog = false;
//   		    FileSelect.nextWindow = Map.class;
//       	    startActivity( new Intent(Main.context, FileSelect.class));
//   		    break;

//    	case R.id.menu_saveroute:
//    		Settings.saveGPX = false;
//    		Settings.selectLoad = false;
//    		Settings.selectSave = true;
//    		Settings.selectSaveLog = false;
//    		FileSelect.nextWindow = Map.class;
//        	startActivity( new Intent(Main.context, FileSelect.class));
//
//    		break;

//    	case R.id.menu_savelog:
//			FileSelect.nextWindow = Map.class;
//			Main.saveRoute(false);
//    		break;

//    	case R.id.menu_savegpx:
//    		saveRoute(true);
//    		break;

//    	case R.id.menu_recordlog:
//    		toggleRecording();
//    		item.setChecked(Settings.recordRoute);
//    		break;
    		
    		
//    	case R.id.menu_editroute:
//    		Settings.editRoute = !Settings.editRoute;
//    		item.setChecked(Settings.editRoute);
//    		refresh();
//    		break;
//
    		
    	default:
    		return Main.onOptionsItemSelected(this, item);
    	}
    	
    }




	@Override
	public void onMapClick(LatLng point) {
		Log.v("Map", "onMapClick " + point.longitude + " " + point.latitude);
		
		if(Settings.editRoute)
		{
// append marker after current active one
			RoutePoint activePoint = null;
			activePoint = new RoutePoint();
			activePoint.latitude = point.latitude;
			activePoint.longitude = point.longitude;
			boolean gotIt = false;

			for(int i = 0; i < Settings.route.size() - 1; i++)
			{
				RoutePoint currentPoint = Settings.route.get(i);
				if(currentPoint.active)
				{
					currentPoint.active = false;
					Settings.route.add(i + 1, activePoint);
					gotIt = true;
					break;
				}
			}
			


			deselectAll();
			if(!gotIt)
			{
				Settings.route.add(activePoint);
			}
			activePoint.active = true;
			
			refresh();
		}
	}

	
	void toggleRecording()
	{
		Main.toggleRecording();
		refresh();
	}

	void updateButtonText()
	{
//		if(menu != null)
//		{
//	    	MenuItem item = menu.findItem(R.id.menu_recordlog);
//	    	if(item != null)
//	    	{
//		    	
//		    	if(Settings.recordRoute)
//		        {
//		        	item.setChecked(true);
//		        }
//		    	else
//		    	{
//		    		item.setChecked(false);
//		    	}
//	    	}
//		}

        
        Button button = (Button)findViewById(R.id.map_pause);
		if(button != null)
		{
			if(Settings.recordRoute)
			{
				button.setText("Pause");
			}
			else
			{
				button.setText("Record");
			}
		}
	}
	

	@Override
	public void onCameraChange(CameraPosition position) {
//		Log.v("Map", "onCameraChange");
		if(!Settings.followPosition || 
				Math.abs(Settings.zoom - position.zoom) > 0.001)
		{
			Settings.bearing = position.bearing;
			Settings.latitude = position.target.latitude;
			Settings.longitude = position.target.longitude;
			Settings.zoom = position.zoom;
			Settings.save(Main.context);
//			Settings.dump();
		}
	}

	@Override
	public void onMarkerDrag(Marker marker) 
	{
//		Log.v("Map", "onMarkerDrag " + 
//				marker.getPosition().longitude + " " +
//				marker.getPosition().latitude);
	}

	@Override
	public void onMarkerDragEnd(Marker marker) 
	{
		deselectAll();
		
		int index = -1;
		RoutePoint routePoint = null;
		for(int i = 0; i < markers.size(); i++)
		{
			if(markers.get(i).equals(marker))
			{
				index = i;
				routePoint = Settings.route.get(i);
				routePoint.active = true;
				routePoint.latitude = marker.getPosition().latitude;
				routePoint.longitude = marker.getPosition().longitude;
				refresh();
				break;
			}
		}
		
		
	}

	@Override
	public void onMarkerDragStart(Marker marker) 
	{
// Can't change any parameters until the drag operation is done
	}
	
	@Override
	public boolean onMarkerClick(Marker marker) {
		if(Settings.editRoute)
		{
			deselectAll();
			for(int i = 0; i < markers.size(); i++)
			{
				if(markers.get(i).equals(marker))
				{
					RoutePoint point = Settings.route.get(i);
					point.active = true;
					refresh();
					break;
				}
			}
		}
		
		return false;
	}

	
	// start editing existing route
	void editRoute()
	{
// find existing active marker
		boolean gotActive = false;
		for(int i = 0; i < Settings.route.size(); i++)
		{
			RoutePoint point = Settings.route.get(i);
			if(point.active) 
			{
				gotActive = true;
				break;
			}
		}

// default to last marker being active
		if(!gotActive)
		{
			if(Settings.route.size() > 0)
			{
				RoutePoint point = Settings.route.get(Settings.route.size() - 1);
				point.active = true;
			}
		}
		
		refresh();
	}
	
	// finished editing or creating route
	void finishRoute()
	{
		deselectAll();


		refresh();
	}

	
//    void saveRoute()
//	{
//// make the directory if it doesn't exist
//        try {
//            boolean result = Settings.dir.mkdirs();
//        }
//        catch(SecurityException e) {
//            Log.v("Settings", "loadInternal " + e.toString());
//        }
//        
//// Search for unused file
//        int i = 0;
//        File file = null;
//        for(i = 0; i < 255; i++)
//        {
//        	file = new File(Settings.dir.getAbsolutePath() + "/route" + i + ".kml");
//        	BufferedReader reader = null;
//    		try {
//    			reader = new BufferedReader(new FileReader(file));
//    		} catch (FileNotFoundException e) {
//               break;
//    		}
//        }
//        
//// new file found
//        if(i < 255)
//        {
//        	Log.v("Map", "startNewRoute currentRoute=" + file.getPath());
//        	Settings.currentRoute = file;
//        }
//	}
	
	
	void deselectAll()
	{
	// set all markers to inactive
		for(int i = 0; i < Settings.route.size(); i++)
		{
			RoutePoint point = Settings.route.get(i);
			point.active = false;
		}
		
		for(int i = 0; i < markers.size(); i++)
		{
			markers.get(i).setIcon(redMarker);
		}
	}
	
	void deletePoint()
	{
		if(Settings.editRoute)
		{
// get point to delete
			int index = -1;
			for(int i = 0; i < Settings.route.size(); i++)
			{
				RoutePoint point = Settings.route.get(i);
				if(point.active) index = i;
				point.active = false;
			}

			deselectAll();
			
			// delete the point
			if(index >= 0)
			{
				Settings.route.remove(index);
				
				// make new point active
				index--;
				
				if(index < Settings.route.size() && index >= 0)
				{
					RoutePoint point = Settings.route.get(index);
					point.active = true;
				}
				else
				{
					if(Settings.route.size() > 0)
					{
						RoutePoint point = Settings.route.get(Settings.route.size() - 1);
						if(point != null)
						{
							point.active = true;
						}
					}
				}
			}

			
			refresh();
		}
	}

// returns true if it loaded a route
	public boolean loadRoute(String file)
	{
    	Log.v("Map", "loadRoute " + file + " isDirectory=" + new File(file).isDirectory());

		boolean routeChanged = false;
		if (Settings.currentRoute == null ||
				Settings.currentRoute.contentEquals(file)) {
			routeChanged = true;
		}
		Settings.currentRoute = file;
		if (routeChanged) Settings.save(Main.context);


		Settings.route.clear();


		XmlPullParser xpp = Xml.newPullParser();
			boolean success = false;
		try {
			xpp.setInput(new FileReader(file));
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

			// XML parser can't detect a 0 length file, so it just hangs

		if(!success) return false;

		int eventType = xpp.END_DOCUMENT;
		try {
			eventType = xpp.getEventType();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String startText = "";
		boolean getCoordinates = true;
		boolean getAbsTime = false;
		boolean getRelTime = false;
		int currentPoint = 0;

		do {
			if (eventType == xpp.START_TAG) {
				startText = xpp.getName();
	//				Log.v("Map", "loadRoute 1 startText=" + startText);
			} else if (eventType == xpp.END_TAG) {

			} else if (eventType == xpp.TEXT) {
	// route
				if (startText.startsWith("coordinates") && getCoordinates) {
					String text = xpp.getText();
					String[] tokens = text.split("[\t\n ]+");
					for (int i = 0; i < tokens.length; i++) {
						String[] tokens2 = tokens[i].split("[,]");
						if (tokens2.length >= 2) {

	//							Log.v("Map", "loadRoute 2 " + tokens2[0] +
	//								" " +
	//								tokens2[1]);

							RoutePoint newPoint = new RoutePoint();
							newPoint.latitude = Float.parseFloat(tokens2[1]);
							newPoint.longitude = Float.parseFloat(tokens2[0]);
							newPoint.altitude = Float.parseFloat(tokens2[2]);


							Settings.route.add(newPoint);

						}
					}
				} else if (startText.startsWith("Folder")) {
					if (getCoordinates) {
						getCoordinates = false;
						getAbsTime = true;
						currentPoint = 0;
					} else if (getAbsTime) {
						getAbsTime = false;
						getRelTime = true;
						currentPoint = 0;
					}
				} else if (startText.startsWith("begin") && getAbsTime) {
					String text = xpp.getText();
					Calendar c = Calendar.getInstance();
	//					Log.v("Map", "loadRoute " + c.getTimeZone().getOffset(c.getTimeInMillis()) / 60 / 60 / 1000);
					int year = Integer.parseInt(text.substring(0, 4));
					int month = Integer.parseInt(text.substring(5, 7)) - 1;
					int day = Integer.parseInt(text.substring(8, 10));
					int hour = Integer.parseInt(text.substring(11, 13));
					int minute = Integer.parseInt(text.substring(14, 16));
					int second = Integer.parseInt(text.substring(17, 19));
	//					Log.v("Map", "loadRoute " + year +
	//							" " + month +
	//							" " + day +
	//							" " + hour +
	//							" " + minute +
	//							" " + second +
	//							" " + offsetHours);
					c.set(year, month, day, hour, minute, second);

	//					TimeZone timeZone = TimeZone.getDefault();
	//					timeZone.setRawOffset(0);
	//					c.setTimeZone(timeZone);
	//					Log.v("Map", "loadRoute " + hour +
	//							" " + c.get(Calendar.HOUR_OF_DAY));

					if (currentPoint < Settings.route.size()) {
						RoutePoint point = Settings.route.get(currentPoint);
						point.time = c.getTimeInMillis() / 1000;
						currentPoint++;
					}
				}
			}

			try {
				eventType = xpp.next();
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		} while (eventType != xpp.END_DOCUMENT);

		refresh();
        return true;
	}

// redraw route & markers
	void refresh()
	{
		map.clear();
		markers.clear();
		polyLine = null;
		logPolyLine = null;
		prevLogIndex = -1;
		cursor = null;
		
		// create markers
		if(Settings.editRoute)
		{
			for(int i = 0; i < Settings.route.size(); i++)
			{
				RoutePoint point = Settings.route.get(i);
				MarkerOptions markerOptions = new MarkerOptions();
				markerOptions.position(new LatLng(point.latitude, point.longitude));
				markerOptions.draggable(true);
				
				if(point.active)
					markerOptions.icon(greenMarker);
				else
					markerOptions.icon(redMarker);
				Marker marker = map.addMarker(markerOptions);
				markers.add(marker);
			}
		}

		
		
//		Log.v("Map", "onMapClick marker=" + marker);
		routeToPolyline(polyLine, Settings.route, Color.RED);
		routeToPolyline(logPolyLine, Settings.log, Color.BLUE);
		updateCursor();
	}
	
	// updates the existing polyline if it exists
	Polyline routeToPolyline(Polyline polyline, Vector<RoutePoint> route, int color)
	{
		if(route.size() == 0) return null;
		
		XYZ prev = new XYZ();
		if(this.polyLine == null)
		{
			PolylineOptions polyLineOptions = new PolylineOptions();
			polyLineOptions.color(color);
			polyLineOptions.width(5);
	   
			for(int i = 0; i < route.size(); i++)
			{
				RoutePoint newPoint = route.get(i);
				XYZ next = Main.llh_to_xyz(newPoint);

				if(i == 0 || Main.distance(next, prev) > 1)
				{
	
					polyLineOptions.add(
						new LatLng(
							newPoint.latitude, 
							newPoint.longitude));
					prev.x = next.x;
					prev.y = next.y;
					prev.z = next.z;
				}
			}
	
			return map.addPolyline(polyLineOptions);
		}
		else
		{
			List<LatLng> newPoints = new ArrayList<LatLng>();
			
			for(int i = 0; i < route.size(); i++)
			{
				RoutePoint routePoint = route.get(i);
				
				XYZ next = Main.llh_to_xyz(routePoint);

				if(i == 0 || Main.distance(next, prev) > Settings.FINE_INCREMENT)
				{
	
					newPoints.add(
						new LatLng(routePoint.latitude,
								routePoint.longitude));
					prev.x = next.x;
					prev.y = next.y;
					prev.z = next.z;
				}
			}
			
			
			polyLine.setPoints(newPoints);
			return polyLine;
		}
	}
	
	void updateLog()
	{
		if(Settings.recordRoute && 
				Settings.log.size() > 0 &&
				Settings.log.size() > prevLogIndex + 1)
		{
			XYZ prev = null;
			PolylineOptions polyLineOptions = new PolylineOptions();
			polyLineOptions.color(Color.BLUE);
			polyLineOptions.width(5);
			List<LatLng> oldPoints = null;

			
			if(prevLogIndex >= 0)
				prev = Main.llh_to_xyz(Settings.log.get(prevLogIndex));
			for(int i = prevLogIndex + 1; i < Settings.log.size(); i++)
			{
				XYZ next = Main.llh_to_xyz(Settings.log.get(i));
				double distance = 0;
				
				if(prevLogIndex >= 0)
					distance = Main.distance(prev, next);
				if(distance > 1.0 || prevLogIndex < 0)
				{
					if(logPolyLine == null)
					{
// add 1st point
						if(prevLogIndex < 0)
						{
							RoutePoint point = Settings.log.get(0);
							polyLineOptions.add(
								new LatLng(
									point.latitude, 
									point.longitude));
						}
						else
						{
// add new point
							RoutePoint point = Settings.log.get(i);
							polyLineOptions.add(
								new LatLng(
									point.latitude, 
									point.longitude));
						}
					}
					else
					{
						if(oldPoints == null)
							oldPoints = logPolyLine.getPoints();
						RoutePoint point = Settings.log.get(i);
						oldPoints.add(new LatLng(
								point.latitude, 
								point.longitude));
					}
					
					prevLogIndex = i;
					prev = next;
				}
			}
			
			
			if(logPolyLine == null)
				logPolyLine = map.addPolyline(polyLineOptions);
			else
			if(oldPoints != null)
				logPolyLine.setPoints(oldPoints);
		}
	}
	
	void updateCursor()
	{
		if(Settings.compassPointer)
		{
			if(compass == null)
			{
				Log.v("Map", "updateCursor");
				compass = new Compass();
				compass.initialize(Main.context);
			}
			
			double heading = compass.getHeading() -
					Settings.bearing;
//Log.v("Map", "updateCursor heading=" + heading);
			
			if(cursor == null)
			{
				if(map != null)
				{
					MarkerOptions markerOptions = new MarkerOptions();
					markerOptions.icon(headingBitmap);
					markerOptions.rotation((float)heading);
					markerOptions.position(Main.getPosition());
					
					cursor = map.addMarker(markerOptions);
				}
			}
			else
			{
				cursor.setRotation((float)heading);
				cursor.setPosition(Main.getPosition());
			}
		}
		else
		if(!Settings.compassPointer)
		{
			if(cursor != null)
			{
				cursor.remove();
				cursor = null;
			}
		}
	}
    
	public void updateGUI()
	{
		if(Settings.compassPointer)
		{
			updateCursor();
			updateLog();

			if(Settings.followPosition && !Settings.mapIsTouched)
			{
				LatLng position = Main.getPosition();
				Settings.latitude = position.latitude;
				Settings.longitude = position.longitude;
				
				map.moveCamera(CameraUpdateFactory.newLatLng(
						position));
		        

			}
		}
	}

    static GoogleMap map;
    static Compass compass;

    Menu menu;
    static BitmapDescriptor redMarker;
    static BitmapDescriptor greenMarker;
    static BitmapDescriptor headingBitmap;
 // polyline for route
    Polyline polyLine;
 // polyline for log
    Polyline logPolyLine;
    int prevLogIndex = -1;
    Vector<Marker> markers = new Vector<Marker>();
    Marker cursor;
}
