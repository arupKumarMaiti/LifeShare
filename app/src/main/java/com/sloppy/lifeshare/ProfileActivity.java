package com.sloppy.lifeshare;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity
{
    String received_key;
    String latitude;
    String longitude;
    UserDetails user_details_matched;

    private TextView details;
    Spinner get_blood_groups;
    ImageView profile_pic;
    ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        details = (TextView)findViewById(R.id.details);

        final FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user == null)
        {
            finish();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        }
        else
        {
            progressBar = (ProgressBar)findViewById(R.id.image_progress_bar);
            progressBar.setVisibility(View.VISIBLE);
            String split_result[] = user.getEmail().toString().split("@");
            received_key = split_result[0];

            display_details();

            storageReference = FirebaseStorage.getInstance().getReference().child(received_key);
            profile_pic = (ImageView)findViewById(R.id.profile_image);
            Glide.with(this).using(new FirebaseImageLoader()).load(storageReference).listener(new RequestListener<StorageReference, GlideDrawable>()
            {
                @Override
                public boolean onException(Exception e, StorageReference model, Target<GlideDrawable> target, boolean isFirstResource)
                {
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, StorageReference model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource)
                {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }
            }).into(profile_pic);

            get_blood_groups = (Spinner)findViewById(R.id.choose_blood_group);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,getResources().getStringArray(R.array.blood_groups));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            get_blood_groups.setAdapter(adapter);

            FloatingActionButton search_button = (FloatingActionButton)findViewById(R.id.search);
            search_button.setImageResource(R.drawable.ic_search_grey_24dp);

            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location)
                {
                    latitude = String.valueOf(location.getLatitude());
                    longitude = String.valueOf(location.getLongitude());
                    String putkey[] = user.getEmail().split("@");
                    String lat = "locationlatitude";
                    String lon = "locationlongitude";
                    databaseReference.child(putkey[0]).child(lat).setValue(latitude);
                    databaseReference.child(putkey[0]).child(lon).setValue(longitude);
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

            startLocation();

            search_button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    String search_query = get_blood_groups.getSelectedItem().toString();
                    Intent intent = new Intent(getApplicationContext(), ResultsActivity.class);
                    intent.putExtra("to_be_searched",search_query);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case 10:
                startLocation();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        LinearLayout profile = (LinearLayout)findViewById(R.id.profile_activity);
        firebaseAuth.signOut();
        finish();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        return true;
    }

    public void display_details()
    {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                for(DataSnapshot userSnapshot : dataSnapshot.getChildren())
                {
                    String key = userSnapshot.getKey();
                    if(key.equalsIgnoreCase(received_key))
                    {
                        user_details_matched = userSnapshot.getValue(UserDetails.class);
                        get_current_city(Double.valueOf(user_details_matched.getLocationlatitude()), Double.valueOf(user_details_matched.getLocationlongitude()), user_details_matched.getName());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Toast.makeText(getApplicationContext(),"Data Fetch Error",Toast.LENGTH_LONG).show();
            }
        });
    }

    void get_current_city(double lat, double lon, String name)
    {
        String current_city = "";
        Geocoder geocoder = new Geocoder(ProfileActivity.this, Locale.getDefault());
        List<Address> addressList;
        try
        {
            addressList = geocoder.getFromLocation(lat, lon, 1);
            if(addressList.size() > 0)
            {
                current_city = addressList.get(0).getLocality();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        details.setText(user_details_matched.getName()+"\n"+current_city);
    }

    void startLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.INTERNET},10);
            }
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,60000,0,locationListener);
    }
}