package com.sloppy.lifeshare;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import static android.content.Context.LOCATION_SERVICE;

public class GetLocation implements ActivityCompat.OnRequestPermissionsResultCallback
{
    Activity useContext;
    private LocationManager locationManager;
    private LocationListener locationListener;
    double distance;
    final static double AVERAGE_RADIUS_OF_EARTH_IN_KM = 6371;
    LocationStore locationStore;

    GetLocation(Activity context)
    {
        useContext = context;
    }

    public LocationStore GetLocation_and_CalculateDistance(final double destLat, final double destLng)
    {
        locationManager = (LocationManager)useContext.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                if(useContext instanceof ResultsActivity)
                {
                    distance = CalculationByDistance(location.getLatitude(),location.getLongitude(),destLat,destLng);
                    locationStore = new LocationStore(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), distance);
                }
                else if(useContext instanceof RegisterActivity)
                {
                    locationStore = new LocationStore(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), 0);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
            }

            @Override
            public void onProviderEnabled(String provider)
            {
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                useContext.startActivity(intent);
            }
        };
        getMyLocation();
        return locationStore;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch(requestCode)
        {
            case 10:
                getMyLocation();
                break;
            default:
                break;
        }
    }

    public void getMyLocation()
    {
        if (ActivityCompat.checkSelfPermission(useContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(useContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                useContext.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.INTERNET},10);
            }
            return;
        }
        Looper looper = null;
        locationManager.requestSingleUpdate("gps",locationListener,looper);
    }

    double CalculationByDistance(double lat1, double lng1, double lat2, double lng2)
    {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = AVERAGE_RADIUS_OF_EARTH_IN_KM * c;

        return new Float(dist).floatValue();
    }
}