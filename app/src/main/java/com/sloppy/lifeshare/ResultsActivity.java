package com.sloppy.lifeshare;

import android.app.ProgressDialog;
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
import android.support.v7.app.AppCompatActivity;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ResultsActivity extends AppCompatActivity
{
    String search_query;
    double latitude;
    double longitude;
    double distance;
    final static double AVERAGE_RADIUS_OF_EARTH_IN_KM = 6371;

    TextView blood_details;
    ListView search_results;
    List<UserDetails> userList;
    List<String> distanceList;

    public ProgressDialog progressDialog;

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        progressDialog = new ProgressDialog(ResultsActivity.this);

        Bundle to_be_searched_data = getIntent().getExtras();
        if(to_be_searched_data == null)
        {
            return;
        }
        search_query = to_be_searched_data.getString("to_be_searched");

        blood_details = (TextView)findViewById(R.id.blood_details);
        blood_details.setText("Showing Results for : "+search_query);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        search_results = (ListView)findViewById(R.id.search_results);

        userList = new ArrayList<>();
        distanceList = new ArrayList<>();
        final String current_key[] = firebaseAuth.getCurrentUser().getEmail().toString().split("@");

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                databaseReference.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        userList.clear();
                        for(DataSnapshot userSnapshot : dataSnapshot.getChildren())
                        {
                            UserDetails details = userSnapshot.getValue(UserDetails.class);
                            if(!(userSnapshot.getKey().equals(current_key[0])))
                            {
                                if((search_query.equals(details.getBloodgroup())) && (Integer.parseInt(details.getAge())>=18))
                                {
                                    distance = CalculationByDistance(latitude,longitude,Double.parseDouble(details.getLocationlatitude()),Double.parseDouble(details.getLocationlongitude()));
                                    if(distance<1.2)
                                    {
                                        userList.add(details);
                                        distanceList.add(String.format("%.2f", (distance*1000)));
                                    }
                                }
                            }
                        }
                        ListAdapter listAdapter = new SearchAdapter(ResultsActivity.this, userList, distanceList);
                        search_results.setAdapter(listAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        Toast.makeText(getApplicationContext(),"Data Fetch Error",Toast.LENGTH_LONG).show();
                    }
                });
                progressDialog.dismiss();
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
                startActivity(intent);
            }
        };

        progressDialog.setMessage("Populating List...");
        progressDialog.show();

        display_result();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case 10:
                display_result();
                break;
            default:
                break;
        }
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

    public void display_result()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.INTERNET},10);
            }
            return;
        }
        Looper looper = null;
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,locationListener,looper);
    }
}