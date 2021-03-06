/**
* Copyright 2017 Davide Polonio <poloniodavide@gmail.com>, Federico Tavella
* <fede.fox16@gmail.com> and Marco Zanella <zanna0150@gmail.com>
* 
* This file is part of SpaceRace.
* 
* SpaceRace is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* SpaceRace is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with SpaceRace.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.augugrumi.spacerace;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.augugrumi.spacerace.pathCreator.PathCreator;
import com.augugrumi.spacerace.pathCreator.PathDrawer;
import com.augugrumi.spacerace.utility.CoordinatesUtility;
import com.augugrumi.spacerace.utility.LanguageManager;
import com.augugrumi.spacerace.utility.LoadingScreenFragment;
import com.augugrumi.spacerace.utility.SharedPreferencesManager;
import com.augugrumi.spacerace.utility.gameutility.ScoreCounter;
import com.augugrumi.spacerace.utility.gameutility.piece.PiecePicker;
import com.augugrumi.spacerace.utility.gameutility.piece.PieceShape;
import com.augugrumi.spacerace.utility.gameutility.piece.PieceSquareShape;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

import static com.augugrumi.spacerace.utility.Costants.KM_DISTANCE_HINT;
import static com.augugrumi.spacerace.utility.Costants.KM_DISTANCE_MARKER;
import static com.augugrumi.spacerace.utility.Costants.PIECE_SIZE;


public abstract class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = MapActivity.class.getSimpleName();
    public static final String MY_SCORE = "my_score_intent";
    public static final String OPPONENT_SCORE = "my_opponent_score_intent";
    private static final int piece = PiecePicker.pickRandomPieceResource();

    protected int myScore = -1;
    protected int opponentScore = -1;

    protected LatLng poi;


    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1500;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final float DEFAULT_ZOOM = 18.0F;
    private float zoom = DEFAULT_ZOOM;
    private static final int MAX_DIFFERENCE_UPDATE_POLYLINE = 15;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    protected Location initialPosition = null;

    private GoogleMap map;

    private Marker marker;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;


    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;


    private boolean mLocationPermissionGranted;
    private boolean isLocationEnabled;

    private LatLng mDefaultLocation = new LatLng(45.414380, 11.876797);

    private AbsHintFragment hf;

    protected Deque<PathCreator.DistanceFrom> path;
    private PathDrawer drawer;
    private LoadingScreenFragment lsf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        lsf = new LoadingScreenFragment();
        hf = new FirstHintFragment();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .add(R.id.hint_cont, lsf)
                .show(lsf)
                .commit();


        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        isLocationEnabled = false;

        mLocationPermissionGranted = checkPermissions();
        if(!mLocationPermissionGranted) {
            requestPermissions();
        } else {
            startLocationUpdates();
        }

        LanguageManager.languageManagement(this);

        keepScreenOn();
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
            updateUI(null);
        }
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if (mRequestingLocationUpdates) {

                    Log.i(TAG, "update event");
                    Location oldLocation = mCurrentLocation;

                    mCurrentLocation = locationResult.getLastLocation();

                    if (initialPosition == null) {
                        initialPosition = mCurrentLocation;

                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(initialPosition.getLatitude(),
                                        initialPosition.getLongitude()), DEFAULT_ZOOM));

                        Log.d("INITIAL_POSITION", initialPosition.getLatitude() + " " +
                                initialPosition.getLongitude());

                        createAndDrawPath();
                    }

                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                    updateUI(oldLocation);
                }

            }
        };
    }


    @NonNull
    private Marker placeMarker(@NonNull BitmapDescriptor draw, @NonNull LatLng pos) {
        if (marker == null) {

            return map.addMarker(new MarkerOptions()
                    .position(pos)
                    .icon(draw));
        } else {

            marker.setPosition(pos);
            return marker;
        }
    }

    @NonNull
    private Marker placeMarker(@NonNull BitmapDescriptor draw, @NonNull Location pos) {
        return placeMarker(draw, new LatLng(
                pos.getLatitude(),
                pos.getLongitude()
        ));
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        mLocationPermissionGranted = true;
                        if (map != null) {
                            mRequestingLocationUpdates = true;
                            isLocationEnabled = true;
                            showCurrentPlace();
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint({"MissingPermission", "StaticFieldLeak"})
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                        updateUI(null);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings " + ((ApiException) e).getStatusMessage());
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI(null);
                    }
                });
    }

    /**
     * Updates all UI fields.
     */
    private void updateUI(final Location oldLocation) {
        if (!isLocationEnabled && map != null) {
            mRequestingLocationUpdates = true;
            zoom = map.getCameraPosition().zoom;
            Log.i("CAMERA_ZOOM", "zoom:" + zoom);
            if (mCurrentLocation != null) {

                marker = placeMarker(PiecePicker.getPiece(new PieceSquareShape(PIECE_SIZE), piece), mCurrentLocation);
            }
        }

        if (mCurrentLocation != null) {
            if (oldLocation!= null) {
                // refresh ogni 2 sec -> record mondiale 8,33m/s => ~16 ogni 2 sec => 15
                // per essere sicuri
                if (CoordinatesUtility.distance(mCurrentLocation, oldLocation)<MAX_DIFFERENCE_UPDATE_POLYLINE) {

                    List<PatternItem> dashItems = new ArrayList<>();
                    // 5 and 10 pixel long dash
                    dashItems.add(new Dash(5));
                    dashItems.add(new Gap(25));
                    dashItems.add(new Dash(15));

                    map.addPolyline(new PolylineOptions()
                            .add(new LatLng(oldLocation.getLatitude(),
                                            oldLocation.getLongitude()),
                                    new LatLng(mCurrentLocation.getLatitude(),
                                            mCurrentLocation.getLongitude()))
                            .endCap(new RoundCap())
                            .startCap(new RoundCap())
                            .pattern(dashItems)
                            .width(30)
                            .color(Color.CYAN));
                    if (marker == null) {
                        PieceShape markerPic = new PieceSquareShape(PIECE_SIZE);
                        marker = placeMarker(PiecePicker.getPiece(markerPic, piece), mCurrentLocation);
                    }
                    marker.setPosition(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                }
                if (drawer != null) {

                    showHintIfNear();
                }
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mCurrentLocation.getLatitude(),
                            mCurrentLocation.getLongitude()), map.getCameraPosition().zoom));
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }
    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.



        if (checkPermissions()) {
            startLocationUpdates();
        } else {
            requestPermissions();
        }
        if (!isLocationEnabled && mLocationPermissionGranted && map != null) {
            if (mCurrentLocation != null) {
                marker.setVisible(true);
            }
            mRequestingLocationUpdates = true;
            showCurrentPlace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
        if (marker != null) {

            marker.setVisible(false);
        }
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MapActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                    mLocationPermissionGranted = true;
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private void popPoi () {
        poi = path.pop().getEnd();
        LatLng next = null;
        if (!path.isEmpty())
            next = path.getFirst().getEnd();
        hf.setPOI(poi, next);
    }

    private boolean hintShown = false;
    private boolean symbolShown = false;
    private void showHintIfNear() {

        if (lsf != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(lsf)
                    .remove(lsf)
                    .commitNow();
            lsf = null;
        }

        Log.i("FRAG_", "" + CoordinatesUtility.get2DDistanceInKm(
                new LatLng(
                        mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude()
                ),
                poi));

        LatLng currentLatLng = new LatLng(
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude()
        );

        if(CoordinatesUtility.get2DDistanceInKm(currentLatLng, poi)<KM_DISTANCE_MARKER
                && !symbolShown) {
            drawer.drawNext();
            symbolShown = true;
        }

        if (CoordinatesUtility.get2DDistanceInKm(
                currentLatLng,
                poi) < KM_DISTANCE_HINT
                && !hintShown) {

            stopLocationUpdates();



            Log.i("FRAG_", "show");
            SupportMapFragment mapFragment =
                    (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            getSupportFragmentManager().
                    beginTransaction()
                    .hide(mapFragment)
                    .show(hf)
                    .commit();

            Log.d("POI", poi.toString());
            LatLng next = null;
            if (!path.isEmpty())
                next = path.getFirst().getEnd();
            hf.setPOI(poi, next);

            /*if (path != null && !path.isEmpty())
                drawPath();*/

            hintShown = true;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, SharedPreferencesManager.getMapStyle()));
        map.setMinZoomPreference(8.0f);
    }

    private void showCurrentPlace() {
        if (map == null || !mLocationPermissionGranted) {
            return;
        }

        @SuppressLint("MissingPermission") Task locationResult =
                mFusedLocationClient.getLastLocation();
        locationResult.addOnCompleteListener(this, new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()) {
                    // Set the map's camera position to the current location of the device.
                    mCurrentLocation = (Location) task.getResult();
                    PieceShape markerPic = new PieceSquareShape(PIECE_SIZE);
                    if (mCurrentLocation!=null) {
                        marker = placeMarker(PiecePicker.getPiece(markerPic, piece), mCurrentLocation);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mCurrentLocation.getLatitude(),
                                        mCurrentLocation.getLongitude()), zoom));
                    } else {
                        marker = placeMarker(PiecePicker.getPiece(markerPic, piece), mDefaultLocation);
                    }
                } else {
                    Log.d("MAP", "Current location is null. Using defaults.");
                    Log.e("MAP", "Exception: %s", task.getException());
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, zoom));
                    map.getUiSettings().setMyLocationButtonEnabled(false);
                }


            }
        });
    }

    public void hideHintAndShowMap() {

        try {
            SupportMapFragment mapFragment =
                    (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(hf)
                    .show(mapFragment)
                    .commitNow();

            if (hf instanceof FirstHintFragment) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(hf)
                        .commitNow();
                hf = new HintFragment();

                popPoi();

                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.hint_cont, (HintFragment) hf)
                        .hide(hf)
                        .commitNow();
            } else {
                hf.setHintData();
                popPoi();
            }
            hintShown = false;
            symbolShown = false;

        } catch (Exception e) {
            Log.e("EXCEPTION_1", e.toString());
            e.printStackTrace();
        }

        startLocationUpdates();
    }

    protected abstract void createAndDrawPath();

    protected void drawPath() {
        PieceShape ps = new PieceSquareShape(125);

        drawer = new PathDrawer.Builder()
                .setMap(map)
                .setStartIcon(PiecePicker.getStartGoal(ps))
                .setMiddleIcon(PiecePicker.getPiece(ps, R.drawable.piece_gem_stone))
                .setEndIcon(PiecePicker.getPiece(ps, R.drawable.piece_direct_hit))
                .setPath(path)
                .build();


        if (drawer.hasNext()) {
            drawer.drawNext();
            poi = path.getFirst().getEnd();
            hf.setPOI(poi, poi);
        }
    }

    public ScoreCounter getTotalScore() {
        return (hf instanceof HintFragment)?((HintFragment)hf).getTotalScore():new ScoreCounter.Builder().build();
    }

    protected void hideLoadingScreen() {
        Log.d("LOADING_SCREEN", "Stopping loading screen");

        try {

            SupportMapFragment mapFragment =
                    (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(mapFragment)
                    .commit();

            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .hide(lsf)
                    .commitNow();

        } catch (Exception e) {
            Log.e("EXCEPTION_2", e.toString());
            e.printStackTrace();
        } finally {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .add(R.id.hint_cont, hf)
                    .show(hf)
                    .commitNow();
        }

        Log.d("LOADING_SCREEN", "Loading screen stopped");
    }

    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void launchEndMatchActivity() {
        Intent intent = new Intent(this, EndMatchActivity.class);
        intent.putExtra(MY_SCORE, myScore);
        intent.putExtra(OPPONENT_SCORE, opponentScore);
        startActivity(intent);
        finish();
    }
}
