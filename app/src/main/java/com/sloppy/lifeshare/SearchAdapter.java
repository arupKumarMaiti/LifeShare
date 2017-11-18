package com.sloppy.lifeshare;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SearchAdapter extends ArrayAdapter<UserDetails> implements ActivityCompat.OnRequestPermissionsResultCallback
{
    Activity context;
    List<UserDetails> details;
    List<String> distance;

    String phone_number;

    public SearchAdapter(Activity context, List<UserDetails> details, List<String> distance)
    {
        super(context, R.layout.results_row, details);
        this.context = context;
        this.details = details;
        this.distance = distance;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable final View convertView, @NonNull ViewGroup parent)
    {
        LayoutInflater layoutInflater = context.getLayoutInflater();
        View search_view = layoutInflater.inflate(R.layout.results_row, null, true);

        TextView name_field = (TextView) search_view.findViewById(R.id.name);
        TextView distance_field = (TextView) search_view.findViewById(R.id.distance);
        FloatingActionButton phone_number_field = (FloatingActionButton) search_view.findViewById(R.id.call);
        phone_number_field.setImageResource(R.drawable.ic_call_grey_24dp);

        UserDetails userDetails = details.get(position);
        name_field.setText(userDetails.getName());
        String get_distance = distance.get(position);
        distance_field.setText("Distance from you : " + get_distance+" m");

        phone_number = userDetails.getPhonenumber();
        phone_number_field.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                call_profile_selected();
            }
        });

        return search_view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case 12:
                call_profile_selected();
                break;
            default:
                break;
        }
    }

    void call_profile_selected()
    {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                context.requestPermissions(new String[]{Manifest.permission.CALL_PHONE},12);
            }
            return;
        }
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:+91"+phone_number));
        context.startActivity(callIntent);
    }
}