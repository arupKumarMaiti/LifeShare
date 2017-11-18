package com.sloppy.lifeshare;

public class LocationStore
{
    String latitude;
    String longitude;
    double distance;

    public LocationStore()
    {
    }

    public LocationStore(String latitude, String longitude, double distance)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }

    public String getLatitude()
    {
        return latitude;
    }

    public String getLongitude()
    {
        return longitude;
    }

    public double getDistance()
    {
        return distance;
    }
}