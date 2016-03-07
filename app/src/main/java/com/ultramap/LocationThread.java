package com.ultramap;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by x on 1/12/16.
 */
public class LocationThread implements
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks
{
    Location location;
    GoogleApiClient mGoogleApiClient;
    GPSLocationListener locationListener;
    Timer timer = new Timer();
    boolean isConnected = false;

    public LocationThread()
    {
        locationListener = new GPSLocationListener();

        mGoogleApiClient = new GoogleApiClient.Builder(Main.main)
                .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();

        mGoogleApiClient.connect();
        timer.start();
    }

    public void stop()
    {
        // Doesn't like disconnect if onConnected hasn't been called.
        // if this is called because lastLocationTimer timed out, but a new LocationThread
        // has just been created, it won't be connected or it might connect right after the call to stop(),
        // thereby never actually disconnecting & resetting the goog client.  lastLocationTimer would just time out
        // again & give it another go.
        if(isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                    locationListener);
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
// Only getting 1hz
        final int locationInterval = 1000;

        LocationRequest mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(locationInterval);
        mLocationRequest.setFastestInterval(locationInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest,
                locationListener);
        isConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
