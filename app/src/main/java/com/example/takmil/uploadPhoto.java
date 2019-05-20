package com.example.takmil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class uploadPhoto extends AppCompatActivity
{
    //key for Azure blob storage
    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=https;AccountName=yadavsrorageaccount01;AccountKey=PBotd4hx3Z1o3VOYDvohsAhKpypGKp8c5GwdOgge0gQZGEtlFvkbdTIVoPjhw0Dm7QUD/Gc/PwlI4DE1P9yfhg==;EndpointSuffix=core.windows.net";


    private ImageView imageView;
    private static File photoFile;
    Bitmap photo;
    Geocoder geocoder;
    List<Address> addressList;
    AppLocationService appLocationService;
    Location ntkLocation;
    Location gpsLocation;


    //codes for various permissions
    private static final int REQUEST_IMAGE_CAPTURE = 1888;
    private static final int RESULT_UPLOADED = 2404;
    private static final int EXTERNAL_STORAGE = 0;
    private static final int WRITE_REQUEST_CODE = 3;
    private static final int ACCESS_FINE_LOCATION_CODE = 4;

    //defining two timestamps in different formats
    String timeStamp;
    String timeStamp1;

    Intent info_intent;
    Toast toast;

    private LocationManager locationManager;
    private LocationListener locationListener;
    String locationLatitude;
    String locationLongitude;
    String  fullAddress;
    String addressStr;
    String areaStr;
    String cityStr;
    String countryStr;
    String postalcodeStr;
    String imageFilePath;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        appLocationService = new AppLocationService(uploadPhoto.this);



        info_intent = getIntent();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //request fine GPS location permissions from the user
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};

        //request storage write permissions from the user
        requestPermissions(permissions, WRITE_REQUEST_CODE);

        imageView = (ImageView) this.findViewById(R.id.photo);
        Button takePhoto = (Button) this.findViewById(R.id.button_takePhoto);

        //check's whether the gps and network is endabled

        gpsLocation=appLocationService.getLocation(LocationManager.GPS_PROVIDER);
        ntkLocation=appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);

        //If gps is enabled then do the action's below
        if(gpsLocation!=null ||ntkLocation!=null)
        {
            takePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    //Creating a internal file , then fileprovider uses the file's uri to load the full sized bitmap in teh file rather than loading it in memory.
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    if (!storageDir.exists())
                        storageDir.mkdirs();
                    imageFilePath = storageDir.getAbsolutePath() + "/camera_photo.jpg";
                    File imageFile = new File(imageFilePath);
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null)
                    {

                        String authorities=getPackageName();
                        try {

                            Context c = getApplicationContext();
                            Uri imageUri = FileProvider.getUriForFile(c, authorities, imageFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            configureLocation();
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                }
            });


            Button uploadPhoto = (Button) this.findViewById(R.id.upload);
            uploadPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UploadFileTask uploadFileTask = new UploadFileTask();
                    uploadFileTask.execute();

                /*
                this toast is displayed whenever the upload button is pressed,
                regardless of whether the image was uploaded successfully or not.
                need to set up exception detection in order to detect upload failures.
                */
                    Context context = getApplicationContext();
                    CharSequence text = "Upload successful!";
                    int duration = Toast.LENGTH_SHORT;

                    toast = Toast.makeText(context, text, duration);
                    toast.show();

                }
            });

            //uploadFileTask.execute();
        }
        else
        {
            showSettingsAlert("GPS");


        }
    }


    private void configureLocation()
    {

        Location finalLoc=null;

        gpsLocation=appLocationService.getLocation(LocationManager.GPS_PROVIDER);
        ntkLocation=appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLocation != null && ntkLocation != null)
        {

            //smaller the number more accurate result will
            if (gpsLocation.getAccuracy() > ntkLocation.getAccuracy())
                finalLoc = ntkLocation;
            else
                finalLoc = gpsLocation;

            // I used this just to get an idea (if both avail, its upto you which you want to take as I've taken location with more accuracy)

        }
        else
        {

            if (gpsLocation != null)
            {
                finalLoc = gpsLocation;

            }
            else if (ntkLocation != null)
            {
                finalLoc = ntkLocation;

            }

        }



        if (finalLoc != null)
        {
            locationLatitude = String.valueOf(finalLoc.getLatitude());
            locationLongitude = String.valueOf(finalLoc.getLongitude());

            try
            {
                //The Geocoder API gets the location address from location coordinates
                geocoder= new Geocoder(uploadPhoto.this, Locale.getDefault());

                addressList = geocoder.getFromLocation(finalLoc.getLatitude(),finalLoc.getLongitude(),1);
                addressStr = addressList.get(0).getAddressLine(0);
                areaStr = addressList.get(0).getLocality();
                cityStr = addressList.get(0).getAdminArea();
                countryStr = addressList.get(0).getCountryName();
                postalcodeStr = addressList.get(0).getPostalCode();

                fullAddress = addressStr + ", " + areaStr + ", " + cityStr + ", " + countryStr + ", " + postalcodeStr;

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

    }

    // An alert dialog for the user to go and change in the respective setting
    public void showSettingsAlert(String provider)
    {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                uploadPhoto.this);

        alertDialog.setTitle(provider + " SETTINGS");

        alertDialog.setMessage(provider
                + " is not enabled! Want to go to settings menu?");

        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        uploadPhoto.this.startActivity(intent);
                        finish();
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });

        alertDialog.show();
    }


    @Override // android recommended class to handle permissions
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("permission", "granted");
                }
                else
                {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.uujm
                    Toast.makeText(uploadPhoto.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();

                    //app cannot function without this permission for now so close it...
                    onDestroy();
                }
                return;
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //if the image is captured successfully
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {

            //Getting the size of the imageView control for scaling
            int imageViewWidth=imageView.getWidth();
            int imageViewHeight=imageView.getHeight();

            //Trying get the width and height of bitmap without loading in memory by setting inJustDecodeBounds=true
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds=true;

            int bitmapWidth=bmOptions.outWidth;
            int bitmapHeight=bmOptions.outHeight;

            //The sample size is the number of pixels in either dimension that correspond to a single pixel in the decoded bitmap.
            // For example, inSampleSize == 4 returns an image that is 1/4 the width/height of the original
            int scaleFactor=Math.min(bitmapWidth/imageViewWidth,bitmapHeight/imageViewHeight);
            bmOptions.inSampleSize=scaleFactor;

            //Finally we are loading the scaled bitmap into memory
            bmOptions.inJustDecodeBounds=false;

            Bitmap bmpImage = BitmapFactory.decodeFile(imageFilePath,bmOptions);
            imageView.setImageBitmap(BitmapFactory.decodeFile(imageFilePath,bmOptions));

            if (isExternalStorageWritable())
            {
                System.out.print("saving image");
                saveImage(bmpImage);
            }
            else
                {
                    toast = Toast.makeText(getApplicationContext(), "Not Able to save", Toast.LENGTH_LONG);
                    toast.show();
                //prompt the user or do something
            }
        }
    }

    private void saveImage(Bitmap  finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        System.out.print("FILEPATH::" + root);
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        //timestamp is used for forming the filename that will get uploaded to Azure
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //timestamp1 is used in the json file, and is parsed by PowerBI
        timeStamp1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String fname = "Shutta_" + timeStamp + ".jpg";

        String filePath = myDir + "\\" + fname;
        filePath.replaceAll("\\\\", "/");
        photoFile = new File(myDir, fname);

        try {

            FileOutputStream out = new FileOutputStream(photoFile);

            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public String getRealPath(Uri uri) {

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    //reference code from Azure's website
    class UploadFileTask extends AsyncTask<String, Void, Void> {

        URI uploadPhoto() {
            URI photoUri = null;
            try {
                // Parse the connection string and create a blob client to interact with Blob storage
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
                CloudBlobContainer container = blobClient.getContainerReference("takmilphoto");

                // Create the container if it does not exist with public access.
                System.out.println("Creating container: " + container.getName());
                container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(), new OperationContext());


                System.out.println("name:" + photoFile.getName());
                CloudBlockBlob blob = container.getBlockBlobReference(photoFile.getName());

                //Creating blob and uploading file to it

                System.out.println("absolute path " + photoFile.getAbsolutePath().substring(1));
                //blob.uploadFromFile(sourceFile.getAbsolutePath().substring(1));
                blob.upload(new FileInputStream(photoFile), photoFile.length());
                System.out.println("Uploaded the sample photo ");
                photoUri = blob.getUri();

                //upload successful toast

                System.out.println("Getting here");
                //i tried putting a toast here, but it crashed the app. something about not being able to read the context.

                System.out.println("URI of blob is: " + blob.getUri());
                /*// Download blob. In most cases, you would have to retrieve the reference
                // to cloudBlockBlob here. However, we created that reference earlier, and
                // haven't changed the blob we're interested in, so we can reuse it.
                // Here we are creating a new file to download to. Alternatively you can also pass in the path as a string into downloadToFile method: blob.downloadToFile("/path/to/new/file").
                File downloadedFile = new File(sourceFile.getParentFile(), "downloadedFile.txt");
                blob.downloadToFile(downloadedFile.getAbsolutePath());*/
            } catch (StorageException ex) {
                System.out.println(String.format("Error returned from the service. Http code: %d and error code: %s", ex.getHttpStatusCode(), ex.getErrorCode()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            return photoUri;
        }




        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(String... params) {
            File sourceFile = null, downloadedFile = null;
            System.out.println("Azure Blob storage quick start sample");

            CloudStorageAccount storageAccount;
            CloudBlobClient blobClient = null;
            CloudBlobContainer container = null;

            try {

                URI photoUri = uploadPhoto();
                // Parse the connection string and create a blob client to interact with Blob storage
                storageAccount = CloudStorageAccount.parse(storageConnectionString);
                blobClient = storageAccount.createCloudBlobClient();
                container = blobClient.getContainerReference("takmil");

                // Create the container if it does not exist with public access.
                System.out.println("Creating container: " + container.getName());
                container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(), new OperationContext());

                String schoolName = info_intent.getStringExtra("schoolName");
                String className = info_intent.getStringExtra("className");
                String teacherName = info_intent.getStringExtra("teacherName");


                String locationProvider = LocationManager.NETWORK_PROVIDER;
// Or use LocationManager.GPS_PROVIDER

                //String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
                //requestPermissions(permissions, ACCESS_FINE_LOCATION_CODE);
                //Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

                //define the json. TODO: implement real latlong
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("location", areaStr);
                jsonObject.put("latlong", locationLatitude + "\n" + locationLongitude);
                jsonObject.put("schoolName", schoolName);
                jsonObject.put("className", className);
                jsonObject.put("teacherName", teacherName);
                jsonObject.put("pictureURL", photoUri);
                jsonObject.put("pictureTimestamp", timeStamp1);


                //Creating a sample file
                sourceFile = File.createTempFile("Microsoft_" + timeStamp + "_", "_TAKMIL.json");
                System.out.println("Creating a sample json file at: " + sourceFile.toString());
                Writer output = new BufferedWriter(new FileWriter(sourceFile));
                output.write(jsonObject.toString());
                output.close();

                //Getting a blob reference
                CloudBlockBlob blob = container.getBlockBlobReference(sourceFile.getName());

                //Creating blob and uploading file to it
                System.out.println("Uploading the sample file ");
                blob.uploadFromFile(sourceFile.getAbsolutePath());
            } catch (StorageException ex) {
                System.out.println(String.format("Error returned from the service. Http code: %d and error code: %s", ex.getHttpStatusCode(), ex.getErrorCode()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }




}
