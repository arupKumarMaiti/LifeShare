package com.sloppy.lifeshare;

public class UserDetails
{
    private String name;
    private String gender;
    private String phonenumber;
    private String age;
    private String bloodgroup;
    private String locationlatitude;
    private String locationlongitude;

    public UserDetails()
    {
    }

    public UserDetails(String name, String gender, String phonenumber, String age, String bloodgroup, String locationlatitude, String locationlongitude)
    {
        this.name = name;
        this.gender = gender;
        this.phonenumber = phonenumber;
        this.age = age;
        this.bloodgroup = bloodgroup;
        this.locationlatitude = locationlatitude;
        this.locationlongitude = locationlongitude;
    }

    public String getName()
    {
        return name;
    }

    public String getGender()
    {
        return gender;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public String getAge()
    {
        return age;
    }

    public String getBloodgroup()
    {
        return bloodgroup;
    }

    public String getLocationlatitude()
    {
        return locationlatitude;
    }

    public String getLocationlongitude()
    {
        return locationlongitude;
    }
}