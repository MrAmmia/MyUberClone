package com.thebookofcode.www.myuberclone;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    Button callUberButton;
    Boolean callARide = false;
    LoadingDialog loadingDialog;
    Handler handler = new Handler();
    //TextView infoTextView;
    Boolean driverActive = true;
    Boolean isConnected;
    ConnectivityManager cm;

    public void checkForUpdates() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null && objects.size() > 0) {

                    driverActive = true;

                    ParseQuery<ParseUser> query = ParseUser.getQuery();

                    query.whereEqualTo("username", objects.get(0).getString("driverUsername"));

                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {

                            if (e == null && objects.size() > 0) {

                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");

                                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                    if (lastKnownLocation != null) {

                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                        Double distanceInMiles = driverLocation.distanceInMilesTo(userLocation);

                                        if (distanceInMiles < 0.01) {
                                            Snackbar.make(callUberButton, "Your driver is here!", Snackbar.LENGTH_LONG).show();
                                            //infoTextView.setText("Your driver is here!");

                                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {

                                                    if (e == null) {

                                                        for (ParseObject object : objects) {

                                                            object.deleteInBackground();

                                                        }


                                                    }

                                                }
                                            });

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    //infoTextView.setText("");
                                                    callUberButton.setVisibility(View.VISIBLE);
                                                    callUberButton.setText("Call An Uber");
                                                    callARide = false;
                                                    driverActive = false;

                                                }
                                            }, 5000);

                                        } else {

                                            Double distanceOneDP = (double) Math.round(distanceInMiles * 10) / 10;
                                            Snackbar.make(callUberButton, "Your driver is \" + distanceOneDP.toString() + \" miles away!", Snackbar.LENGTH_LONG).show();
                                            //infoTextView.setText("Your driver is " + distanceOneDP.toString() + " miles away!");

                                            LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());

                                            LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                            ArrayList<Marker> markers = new ArrayList<>();

                                            mMap.clear();

                                            markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Driver Location")));
                                            markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationLatLng).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();


                                            int padding = 60; // offset from edges of the map in pixels
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                            mMap.animateCamera(cu);


                                            callUberButton.setVisibility(View.INVISIBLE);

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {

                                                    checkForUpdates();

                                                }
                                            }, 2000);

                                        }

                                    } else {
                                        checkLocation();
                                    }

                                }

                            }

                        }
                    });


                }


            }
        });


    }

    public void callUber(View view) {

        if (callARide) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("email", ParseUser.getCurrentUser().getEmail());
            loadingDialog.startAlertDialog();
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    loadingDialog.dismissDialog();
                    if (e == null && objects.size() > 0) {

                        for (ParseObject object : objects) {
                            object.deleteInBackground();
                        }

                        callARide = false;
                        callUberButton.setText("CALL AN UBER");
                    }
                }
            });
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    ParseObject request = new ParseObject("Request");
                    request.put("email", ParseUser.getCurrentUser().getEmail());
                    request.put("username", ParseUser.getCurrentUser().getUsername());
                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    request.put("location", parseGeoPoint);
                    loadingDialog.startAlertDialog();
                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            loadingDialog.dismissDialog();
                            if (e == null) {
                                Snackbar.make(callUberButton, "Request Successful", Snackbar.LENGTH_LONG).show();
                                callUberButton.setText("CANCEL UBER");
                                callARide = true;
                                checkForUpdates();
                            } else {
                                e.printStackTrace();
                            }
                        }
                    });

                } else {
                    checkLocation();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        updateMap(lastKnownLocation);
                    }/* else {
                        checkLocation();
                    }*/
                }

            }
        }
    }

    public void updateMap(Location location) {
        /*if (driverActive == false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
        }*/
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                }

                @Override
                public void onLosing(@NonNull Network network, int maxMsToLive) {
                    super.onLosing(network, maxMsToLive);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    checkConnection();
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                }

                @Override
                public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                }

                @Override
                public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
                    super.onBlockedStatusChanged(network, blocked);
                }
            });
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            callUberButton = findViewById(R.id.callUberButton);
            loadingDialog = new LoadingDialog(RiderActivity.this);
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("email", ParseUser.getCurrentUser().getEmail());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null && objects.size() > 0) {
                        callARide = true;
                        callUberButton.setText("CANCEL UBER");
                        checkForUpdates();
                    }
                }
            });
        } else {
            checkConnection();
        }

    }

    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this).setTitle("Exit")
                .setMessage("Are you sure you want to exit")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finishAffinity();
                    }
                }).setNegativeButton("NO", null).show();


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (Build.VERSION.SDK_INT < 23) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    updateMap(lastKnownLocation);
                } /*else {
                    checkLocation();
                }*/
            }
        }

    }

    public void logout(View view) {
        new AlertDialog.Builder(this).setTitle("Alert")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ParseUser.logOut();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                }).setNegativeButton("NO",null).show();

    }

    public void restartActivity() {
        Intent intent = new Intent(this, RiderActivity.class);
        startActivity(intent);
    }

    public void checkConnection() {
        try {
            new AlertDialog.Builder(this).setTitle("Alert")
                    .setMessage("No Internet Connection Detected")
                    .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            restartActivity();
                        }
                    }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finishAffinity();
                }
            }).show();
        } catch (Exception e) {
            Snackbar.make(callUberButton, e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    public void checkLocation() {
        new AlertDialog.Builder(this).setTitle("Alert")
                .setMessage("Could not get your location....Please check your location settings and try again").setNeutralButton("OK", null)
                .show();
    }
}