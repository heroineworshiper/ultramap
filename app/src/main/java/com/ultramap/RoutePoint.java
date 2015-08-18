package com.ultramap;


import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

public class RoutePoint {
// lon/lat points along current route
    double latitude;
    double longitude;
    double altitude;
// calendar time in seconds point was recorded
    long time;
// relative time in seconds point was recorded
    long relativeTime;
	
// total distance since start of route in increments of DISTANCE_INCREMENT
// used for distane
	double distance;
	
    boolean active = false;
}
