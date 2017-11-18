package com.sloppy.lifeshare;

import android.*;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.R.attr.maxHeight;
import static android.R.attr.maxWidth;

public class RegisterActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener
{
    private EditText name_field;
    private EditText phone_number_field;
    private EditText email_field;
    private EditText password_field;
    private Spinner select_gender;
    private Spinner select_blood_group;
    private TextView birthday_field;
    private TextView to_login_button;
    private Button register_button;
    private ImageView profile_pic;

    private Uri uri;
    private Uri sourceUri;
    private Uri destinationUri;
    static Uri camera_uri;

    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private LocationManager locationManager;
    private LocationListener locationListener;

    int init_year;
    int init_month;
    int init_day;
    int modified_year;
    int modified_month;
    int modified_day;
    String calculate_age;

    String name;
    String gender;
    String phoneNumber;
    String age;
    String bloodGroup;
    String email;
    String pass;
    String latitude;
    String longitude;

    private static final int IMAGE_SELECT_REQUEST_CODE = 111;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name_field = (EditText)findViewById(R.id.name);
        phone_number_field = (EditText)findViewById(R.id.phone_number);
        birthday_field = (TextView)findViewById(R.id.birthday);
        email_field = (EditText)findViewById(R.id.email);
        password_field = (EditText)findViewById(R.id.password);
        select_gender = (Spinner)findViewById(R.id.select_gender);
        select_blood_group = (Spinner)findViewById(R.id.select_blood_group);
        to_login_button = (TextView)findViewById(R.id.proceed_to_login);
        register_button = (Button)findViewById(R.id.register);
        profile_pic = (ImageView)findViewById(R.id.profile_pic);

        progressDialog = new ProgressDialog(this);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                latitude = String.valueOf(location.getLatitude());
                longitude = String.valueOf(location.getLongitude());
                startRegister();
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

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        if(firebaseAuth.getCurrentUser() != null)
        {
            finish();
            Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        ArrayAdapter<String> adapter_gender = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.gender));
        adapter_gender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        select_gender.setAdapter(adapter_gender);

        ArrayAdapter<String> adapter_blood_group = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.blood_groups));
        adapter_blood_group.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        select_blood_group.setAdapter(adapter_blood_group);

        Calendar calendar = Calendar.getInstance();
        init_year = calendar.get(Calendar.YEAR);
        init_month = calendar.get(Calendar.MONTH);
        init_day = calendar.get(Calendar.DAY_OF_MONTH);
        modified_year = init_year;
        modified_month = init_month;
        modified_day = init_day;

        birthday_field.setOnClickListener(new View.OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v)
            {
                DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this, RegisterActivity.this, modified_year, modified_month, modified_day);
                datePickerDialog.show();
            }
        });

        to_login_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }
        });

        register_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                registerUser();
            }
        });

        profile_pic.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showFileChooser();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case 10:
                startLocation();
                break;
            case 11:
                showFileChooser();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
    {
        int month_difference;
        int year_difference;

        month_difference=init_month-month;
        year_difference=init_year-year;

        if(month_difference<0)
            year_difference=year_difference-1;

        calculate_age = String.valueOf(year_difference);

        birthday_field.setText(dayOfMonth+"/"+(month+1)+"/"+year);

        modified_year = year;
        modified_month = month;
        modified_day = dayOfMonth;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            uri = data.getData();
            sourceUri = uri;
            File filedir = new File(Environment.getExternalStorageDirectory()+"/LifeShare/Images");

            if(!filedir.exists())
            {
                filedir.mkdirs();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = dateFormat.format(new Date());

            File file = new File(Environment.getExternalStorageDirectory()+"/LifeShare/Images", "image-"+currentDateTime+".png");
            destinationUri = Uri.fromFile(file);
            UCrop.of(sourceUri, destinationUri).withAspectRatio(1,1).withMaxResultSize(maxWidth, maxHeight).start(this);
        }
        else if(requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == -1)
        {
            sourceUri = camera_uri;
            File filedir = new File(Environment.getExternalStorageDirectory()+"/LifeShare/Images");

            if(!filedir.exists())
            {
                filedir.mkdirs();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = dateFormat.format(new Date());

            File file = new File(Environment.getExternalStorageDirectory()+"/LifeShare/Images", "image-"+currentDateTime+".png");
            destinationUri = Uri.fromFile(file);
            UCrop.of(sourceUri, destinationUri).withAspectRatio(1,1).withMaxResultSize(maxWidth, maxHeight).start(this);
        }
        else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP)
        {
            destinationUri = UCrop.getOutput(data);
            Bitmap bitmap = null;
            try
            {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), destinationUri);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            profile_pic.setImageBitmap(bitmap);
        }
        else
        {
            Toast.makeText(getApplicationContext(),"Something Went Wrong",Toast.LENGTH_SHORT).show();
        }
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
        Looper looper = null;
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,locationListener,looper);
    }

    void registerUser()
    {
        name = name_field.getText().toString().trim();
        gender = select_gender.getSelectedItem().toString().trim();
        phoneNumber = phone_number_field.getText().toString().trim();
        age = calculate_age;
        bloodGroup = select_blood_group.getSelectedItem().toString().trim();
        email = email_field.getText().toString().trim();
        pass = password_field.getText().toString().trim();

        if(TextUtils.isEmpty(name))
        {
            Toast.makeText(getApplicationContext(),"Name Cannot Be Blank",Toast.LENGTH_SHORT).show();
            return;
        }

        if(TextUtils.isEmpty(phoneNumber))
        {
            Toast.makeText(getApplicationContext(),"PhoneNumber Cannot Be Blank",Toast.LENGTH_SHORT).show();
            return;
        }

        if(phoneNumber.length() != 10)
        {
            Toast.makeText(getApplicationContext(),"Invalid PhoneNumber",Toast.LENGTH_SHORT).show();
            return;
        }

        if(birthday_field.getText().toString().trim().equals("Birthday"))
        {
            Toast.makeText(getApplicationContext(),"Invalid Date-Of-Birth",Toast.LENGTH_SHORT).show();
            return;
        }

        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(getApplicationContext(),"Email Cannot Be Blank", Toast.LENGTH_SHORT).show();
            return;
        }

        if(TextUtils.isEmpty(pass))
        {
            Toast.makeText(getApplicationContext(),"Password Cannot Be Blank",Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        startLocation();
    }

    void startRegister()
    {
        firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
        {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task)
            {
                if(task.isSuccessful())
                {
                    UserDetails userDetails = new UserDetails(name,gender,phoneNumber,age,bloodGroup,latitude,longitude);
                    String putKey[] = email.split("@");
                    store_in_database(putKey[0],userDetails);
                    progressDialog.dismiss();
                    upload_file(putKey[0]);

                    finish();
                    Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                else
                {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Please Try Again",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void store_in_database(String key,UserDetails userDetails)
    {
        databaseReference.child(key).setValue(userDetails);
    }

    void showFileChooser()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},11);
            }
            return;
        }
        startActivityForResult(getPickImageChooserIntent(getApplicationContext()), IMAGE_SELECT_REQUEST_CODE);
    }

    private void upload_file(String name)
    {
        progressDialog.setMessage("Uploading...");

        StorageReference store_ref = storageReference.child(name);

        if(destinationUri == null)
        {
            destinationUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                    + "://" + getResources().getResourcePackageName(R.drawable.default_profile_pic)
                    + '/' + getResources().getResourceTypeName(R.drawable.default_profile_pic)
                    + '/' + getResources().getResourceEntryName(R.drawable.default_profile_pic));
        }

        store_ref.putFile(destinationUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                    {
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception exception)
                    {
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(),"Error Uploading Image",Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                    {
                        @SuppressWarnings("VisibleForTests")
                        double progress = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                        progressDialog.setMessage(((int)progress) + "% Uploaded...");
                    }
                });
    }

    public static Uri getCaptureImageOutputUri(@NonNull Context context)
    {
        camera_uri = null;
        File filedir = new File(Environment.getExternalStorageDirectory()+"/LifeShare/Images");

        if(!filedir.exists())
        {
            filedir.mkdirs();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDateTime = dateFormat.format(new Date());

        File file = new File(Environment.getExternalStorageDirectory()+"/LifeShare/Images", "image-"+currentDateTime+".png");
        camera_uri = Uri.fromFile(file);
        return camera_uri;
    }

    public static List<Intent> getCameraIntents(@NonNull Context context,@NonNull PackageManager packageManager)
    {
        List<Intent> allIntents = new ArrayList<>();
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);

        Uri outputFileUri = getCaptureImageOutputUri(context);

        for (ResolveInfo res : listCam)
        {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null)
            {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        return allIntents;
    }

    public static List<Intent> getGalleryIntents(@NonNull PackageManager packageManager, String action)
    {
        List<Intent> intents = new ArrayList<>();
        Intent galleryIntent = action == Intent.ACTION_GET_CONTENT ? new Intent(action) : new Intent(action, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);

        for (ResolveInfo res : listGallery)
        {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            intents.add(intent);
        }

        for (Intent intent : intents)
        {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity"))
            {
                intents.remove(intent);
                break;
            }
        }
        return intents;
    }

    public static Intent getPickImageChooserIntent(@NonNull Context context)
    {
        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        allIntents.addAll(getCameraIntents(context,packageManager));

        List<Intent> galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT);
        if (galleryIntents.size() == 0)
        {
            galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_PICK);
        }
        allIntents.addAll(galleryIntents);

        Intent target;

        if (allIntents.isEmpty())
        {
            target = new Intent();
        }
        else
        {
            target = allIntents.get(allIntents.size() - 1);
            allIntents.remove(allIntents.size() - 1);
        }

        Intent chooserIntent = Intent.createChooser(target,"Select an Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }
}