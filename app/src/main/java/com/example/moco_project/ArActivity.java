package com.example.moco_project;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.moco_project.helpers.DisplayRotationHelper;
import com.example.moco_project.helpers.LocationPermissionHelper;
import com.example.moco_project.helpers.TapHelper;
import com.example.moco_project.helpers.TrackingStateHelper;
import com.example.moco_project.rendering.BackgroundRenderer;
import com.example.moco_project.rendering.Framebuffer;
import com.example.moco_project.rendering.IndexBuffer;
import com.example.moco_project.rendering.Mesh;
import com.example.moco_project.rendering.ObjectRenderer;
import com.example.moco_project.rendering.PlaneRenderer;
import com.example.moco_project.rendering.PointCloudRenderer;
import com.example.moco_project.rendering.SampleRender;
import com.example.moco_project.rendering.Shader;
import com.example.moco_project.rendering.Texture;
import com.example.moco_project.rendering.VertexBuffer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Earth;
import com.google.ar.core.Frame;
import com.google.ar.core.FutureState;
import com.google.ar.core.GeospatialPose;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.ResolveAnchorOnTerrainFuture;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.StreetscapeGeometry;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.VpsAvailability;
import com.google.ar.core.VpsAvailabilityFuture;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArActivity extends AppCompatActivity  implements SampleRender.Renderer{

    /**
     * VARIABLES
     */

    /*
     requestInstall(Activity, true) will trigger installation of Google Play Services for AR if
     necessary.
     */

    // ARCore session
    private Session session;
    private boolean installRequested;

    // ARCore shared camera instance, obtained from ARCore session that supports sharing.
    private SharedCamera sharedCamera;
    // Camera ID for the camera used by ARCore.
    private String cameraId;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    // Reference to the camera system service.
    private CaptureRequest.Builder previewCaptureRequestBuilder;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    // A list of CaptureRequest keys that can cause delays when switching between AR and non-AR modes.
    private List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified;

    // Camera capture session. Used by both non-AR and AR modes.
    private CameraCaptureSession captureSession;

    // Whether the app is currently in AR mode. Initial value determines initial state.
    private boolean arMode = true;

    // Whether ARCore is currently active.
    private boolean arcoreActive;

    private BackgroundRenderer backgroundRenderer;

    // Prevent any changes to camera capture session after CameraManager.openCamera() is called, but
    // before camera device becomes active.
    private boolean captureSessionChangesPossible = true;

    // Ensure GL surface draws only occur when new frames are available.
    private final AtomicBoolean shouldUpdateSurfaceTexture = new AtomicBoolean(false);

    // GL Surface used to draw camera preview image.
    private GLSurfaceView surfaceView;

    // Whether the GL surface has been created.
    private boolean surfaceCreated;

    // Whether the app has just entered non-AR mode.
    private final AtomicBoolean isFirstFrameWithoutArcore = new AtomicBoolean(true);

    // A check mechanism to ensure that the camera closed properly so that the app can safely exit.
    private final ConditionVariable safeToExitApp = new ConditionVariable();

    private PlaneRenderer planeRenderer;

    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    private DisplayRotationHelper displayRotationHelper;

    private final ArrayList<Anchor> anchors = new ArrayList<>();

    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

    // Whether an error was thrown during session creation.
    private boolean errorCreatingSession = false;

    // Text view for displaying on screen status message.
    private TextView statusTextView;

    private Earth earth;

    enum State {
        /** The Geospatial API has not yet been initialized. */
        UNINITIALIZED,
        /** The Geospatial API is not supported. */
        UNSUPPORTED,
        /** The Geospatial API has encountered an unrecoverable error. */
        EARTH_STATE_ERROR,
        /** The Session has started, but {@link Earth} isn't  yet. */
        PRETRACKING,
        /**
         * {@link Earth} is {, but the desired positioning confidence
         * hasn't been reached yet.
         */
        LOCALIZING,
        /**
         * {@link Earth} is  and the desired positioning confidence has
         * been reached.
         */
        LOCALIZED
    }

    private State state = State.UNINITIALIZED;

    // The thresholds that are required for horizontal and orientation accuracies before entering into
    // the LOCALIZED state. Once the accuracies are equal or less than these values, the app will
    // allow the user to place anchors.
    private static final double LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS = 10;
    private static final double LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES = 15;

    // Once in the LOCALIZED state, if either accuracies degrade beyond these amounts, the app will
    // revert back to the LOCALIZING state.
    private static final double LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS = 10;
    private static final double LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES = 10;

    private ProgressBar hungerBar;
    private Timer timer;

    private final Object anchorsLock = new Object();

    private final Set<Anchor> terrainAnchors = new HashSet<>();

    private boolean mushroomsAnchorPlaced = false;

    private boolean mushroomsPlaced = false;

    private boolean hasSetTextureNames = false;

    private SampleRender render;

    // Provides device location.
    private FusedLocationProviderClient fusedLocationClient;
    private Framebuffer virtualSceneFramebuffer;
    // Virtual object (ARCore geospatial)
    private Mesh virtualObjectMesh;
    private Shader geospatialAnchorVirtualObjectShader;
    // Virtual object (ARCore geospatial terrain)
    private Shader terrainAnchorVirtualObjectShader;

    // Point Cloud
    private VertexBuffer pointCloudVertexBuffer;
    private Mesh pointCloudMesh;
    private Shader pointCloudShader;
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16]; // view x model
    private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private long lastPointCloudTimestamp = 0;
    private final float[] identityQuaternion = {0, 0, 0, 1};

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000f;
    private boolean mySpotanchored = false;

    // Locks needed for synchronization
    private final Object singleTapLock = new Object();
    @GuardedBy("singleTapLock")
    private MotionEvent queuedSingleTap;

    private static final int MAXIMUM_ANCHORS = 100;

    enum AnchorType {
        // Set WGS84 anchor.
        GEOSPATIAL,
        // Set Terrain anchor.
        TERRAIN,
        // Set Rooftop anchor.
        ROOFTOP
    }
    private AnchorType anchorType = AnchorType.TERRAIN;

    private Button clearAnchorsButton;

    private Integer clearedAnchorsAmount = null;
    private SharedPreferences sharedPreferences;
    private static final String SHARED_PREFERENCES_SAVED_ANCHORS = "SHARED_PREFERENCES_SAVED_ANCHORS";

    private GestureDetector gestureDetector;

    private static final String ALLOW_GEOSPATIAL_ACCESS_KEY = "ALLOW_GEOSPATIAL_ACCESS";
    /** ---------------------------------------------------------------------------------
     * METHODE AREA
     */


    /**
     * Copied and modified from the example projects geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * and shared_camera_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/shared_camera_java">Git-Hub</a>
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("ARCore:", "Creating ARActivity");
        super.onCreate(savedInstanceState);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_check_ar_availability);
        surfaceView = findViewById(R.id.surfaceview);

        //Sets up timer for decreasing hunger meter;
        hungerBar = findViewById(R.id.hungerBar);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                hungerBar.setProgress(GameData.getHunger());
                GameData.decrementHunger(); //decrement hunger
            }
        }, 0, GameData.getHungerDecreaseInterval());

        displayRotationHelper = new DisplayRotationHelper(this);

        // Set up renderer
        render = new SampleRender(surfaceView, this, getAssets());

        installRequested = false;

        statusTextView = findViewById(R.id.text_view);

        // Set up touch listener.
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                synchronized (singleTapLock) {
                                    queuedSingleTap = e;
                                }
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });
        surfaceView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(/* context= */ this);


        // Switch to allow pausing and resuming of ARCore.
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch arcoreSwitch = findViewById(R.id.arcore_switch);
        // Ensure initial switch position is set based on initial value of `arMode` variable.
        Log.i("GameData:", String.valueOf(GameData.getIsArActivity()));
        arcoreSwitch.setChecked(GameData.getIsArActivity());

        arcoreSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {

                Log.i("ARCore:", "Switching to " + (checked ? "AR" : "non-AR") + " mode.");
                //Update the switch state
                if (compoundButton.isChecked()) {
                    statusTextView.setText("ARCore is activated");
                    Log.i("ARCore:", "ARCore is activated");
                    //resumeARCore();
                } else {
                    statusTextView.setText("ARCore was paused");
                    Log.i("ARCore:", "ARCore was paused");
                    //pauseARCore();
                    GameData.setIsArActivity(false);
                    startActivity(new Intent(ArActivity.this, MapActivity.class));
                }
            });
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session.close();
            session = null;
        }
        timer.cancel();
        super.onDestroy();
    }

    /**
     * Copied and modified from the example shared_camera_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/shared_camera_java">Git-Hub</a>
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (sharedPreferences.getBoolean(ALLOW_GEOSPATIAL_ACCESS_KEY, /* defValue= */ false)) {
            createSession();
        }else {
            Log.i(TAG, "on Resume no Session created");
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
        statusTextView.setText("Session is resuming");
        /*
        BTW the markers have IDs in their ".title" keys. IDs are given to markers when they are
        first generated. They are numbers starting from 0 and their type is String.
        When markers are clicked on the map, GameData.deleteMarkerByTitle(String title) is called.
        This function deletes the correct instance from the markerdata list.
        You can also get the last user location with GameData.getUserLocation()
         */
    }

   /** Create a terrain anchor at a specific geodetic location using a EUS quaternion. */
    private void createTerrainAnchor(Earth earth, double latitude, double longitude, float[] quaternion) {
        final ResolveAnchorOnTerrainFuture future = earth.resolveAnchorOnTerrainAsync(
            latitude,
            longitude,
            /* altitudeAboveTerrain= */ 0.0f,
            quaternion[0],
            quaternion[1],
            quaternion[2],
            quaternion[3],
            (anchor, state) -> {
                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                    synchronized (anchorsLock) {
                        anchors.add(anchor);
                        terrainAnchors.add(anchor);
                        Log.i("Shroomy:", "TerrainAnchor was created");
                    }
                } else {
                    Log.i("Shroomy:", "The anchor failed to resolve");
                }
        });
    }

    /**
     * Copied from ARCores example geospatial_java ->
     * <a href="https://github.com/google-ar/arcore-android-sdk/blob/master/samples/geospatial_java/app/src/main/java/com/google/ar/core/examples/java/geospatial/GeospatialActivity.java">GitHub</a>
     * @param earth An image of the real world
     */
    private void updateGeospatialState(Earth earth) {
        if (earth.getEarthState() != Earth.EarthState.ENABLED) {
            state = State.EARTH_STATE_ERROR;
            return;
        }
        if (earth.getTrackingState() != TrackingState.TRACKING) {
            state = State.PRETRACKING;
            return;
        }
        if (state == State.PRETRACKING) {
            updatePretrackingState(earth);
        } else if (state == State.LOCALIZING) {
            updateLocalizingState(earth);
        } else if (state == State.LOCALIZED) {
            updateLocalizedState(earth);
        }
    }

    /**
     * TODO REMOVE IF NOT NEEDED
     * Checks the state of our Visual Positioning System (VPS).
     * @param future
     */
    private void checkVpsState(VpsAvailabilityFuture future)  {
        if(future.getState() != FutureState.DONE) {
            Log.i("VpsAvailability:", "FutureState is not DONE yet.");
            if(future.getState() == FutureState.PENDING) {
                Log.i("VpsAvailability:", "FutureState is PENDING. Trying to resolve.");
                //wait(10000);
            } else if(future.getState() == FutureState.CANCELLED) {
                Log.i("VpsAvailability:", "FutureState is CANCELLED.");
            }
        }
    }


    /**
     * Copied and modified from ARCores example geospatial_java ->
     * <a href="https://github.com/google-ar/arcore-android-sdk/blob/master/samples/geospatial_java/app/src/main/java/com/google/ar/core/examples/java/geospatial/GeospatialActivity.java">GitHub</a>
     *
     * Handles the updating for @link State.PRETRACKING. In this state, wait for {@link Earth} to
     * have @link TrackingState.TRACKING. If it hasn't been enabled by now, then we've encountered
     * an unrecoverable @link State.EARTH_STATE_ERROR.
     */
    private void updatePretrackingState(Earth earth) {
        if (earth.getTrackingState() == TrackingState.TRACKING) {
            state = State.LOCALIZING;
            return;
        }
        Log.i("Earth State:", "Earth is not tracked.");
    }

    /**
     * Copied and modified from ARCores example geospatial_java ->
     * <a href="https://github.com/google-ar/arcore-android-sdk/blob/master/samples/geospatial_java/app/src/main/java/com/google/ar/core/examples/java/geospatial/GeospatialActivity.java">GitHub</a>
     *
     * Handles the updating for @link State.LOCALIZED. In this state, check the accuracy for
     * degradation and return to @link State.LOCALIZING if the position accuracies have dropped too
     * low.
     */
    private void updateLocalizedState(Earth earth) {
        GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
        // Check if either accuracy has degraded to the point we should enter back into the LOCALIZING
        // state.
        if (geospatialPose.getHorizontalAccuracy()
                > LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
                + LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS
                || geospatialPose.getOrientationYawAccuracy()
                > LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES
                + LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES) {
            // Accuracies have degenerated, return to the localizing state.
            state = State.LOCALIZING;
        }
    }

    /**
     * Copied and modified from ARCores example geospatial_java ->
     * <a href="https://github.com/google-ar/arcore-android-sdk/blob/master/samples/geospatial_java/app/src/main/java/com/google/ar/core/examples/java/geospatial/GeospatialActivity.java">GitHub</a>
     *
     * Handles the updating for @link State.LOCALIZING. In this state, wait for the horizontal and
     * orientation threshold to improve until it reaches your threshold.
     */
    private void updateLocalizingState(Earth earth) {
        GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
        if (geospatialPose.getHorizontalAccuracy() <= LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
                && geospatialPose.getOrientationYawAccuracy()
                <= LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES) {
            state = State.LOCALIZED;
            synchronized (anchorsLock) {
                final int anchorNum = anchors.size();
                if (anchorNum == 0) {
                    createAnchorFromSharedPreferences(earth);
                }
            }
        }
    }

    private void createAnchorFromSharedPreferences(Earth earth) {
        Set<String> anchorParameterSet =
                sharedPreferences.getStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, null);
        if (anchorParameterSet == null) {
            return;
        }

        for (String anchorParameters : anchorParameterSet) {
            AnchorType type = AnchorType.GEOSPATIAL;
            if (anchorParameters.contains("Terrain")) {
                type = AnchorType.TERRAIN;
                anchorParameters = anchorParameters.replace("Terrain", "");
            } else if (anchorParameters.contains("Rooftop")) {
                type = AnchorType.ROOFTOP;
                anchorParameters = anchorParameters.replace("Rooftop", "");
            }
            String[] parameters = anchorParameters.split(",");
            if (parameters.length != 7) {
                Log.d(
                        TAG, "Invalid number of anchor parameters. Expected four, found " + parameters.length);
                continue;
            }
            double latitude = Double.parseDouble(parameters[0]);
            double longitude = Double.parseDouble(parameters[1]);
            double altitude = Double.parseDouble(parameters[2]);
            float[] quaternion =
                    new float[] {
                            Float.parseFloat(parameters[3]),
                            Float.parseFloat(parameters[4]),
                            Float.parseFloat(parameters[5]),
                            Float.parseFloat(parameters[6])
                    };
            switch (type) {
                case TERRAIN:
                    createTerrainAnchor(earth, latitude, longitude, quaternion);
                    break;
            }
        }
    }

    /**
     * Copied from the example shared_camera_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/shared_camera_java">Git-Hub</a>
     */
    @Override
    public void onPause() {
        surfaceView.onPause();
        displayRotationHelper.onPause();
        if (GameData.getIsArActivity()) {
            pauseARCore();
        }
        closeCamera();
        super.onPause();
    }

    /**
     * Verifies that ARCore is installed and using the current version.
     */
    public boolean checkARCoreInstallationStatus() {
        // Make sure ARCore is installed and supported on this device.
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        switch (availability) {
            case SUPPORTED_INSTALLED:
                break;
            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                try {
                    ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(this, true);
                    switch (installStatus) {
                        case INSTALL_REQUESTED:
                            Log.i(TAG, "Installation of ARCore requested.");
                            return false;
                        case INSTALLED:
                            break;
                    }
                } catch (UnavailableException e) {
                    Log.e(TAG, "ARCore is not installed", e);
                    finish();
                    return false;
                }
                break;
            case UNSUPPORTED_DEVICE_NOT_CAPABLE:
            case UNKNOWN_CHECKING:
            case UNKNOWN_ERROR:
            case UNKNOWN_TIMED_OUT:
                Log.e(
                    TAG,
                    "ARCore is not supported on this device, ArCoreApk.checkAvailability() returned "
                            + availability);
                return false;
        }
        return true;
    }

    /**
     * Checks, if the camera permission has been granted before or not
     * @return true --> was granted; false --> wasn't  granted
     */
    public boolean checkIfCameraPermissionHasBeenGranted() {
        Log.i(TAG, "PERMISSION has been granted before.");
        return (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Requests to use the camera. A pop up window will appear and asks for usage permission
     */
    public void requestCameraPermission() {
        Log.i(TAG, "REQUESTING PERMISSION.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
    }

    /**
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * @param latitude
     * @param longitude
     */
    private void checkVpsAvailability(double latitude, double longitude) {
        final VpsAvailabilityFuture future =
                session.checkVpsAvailabilityAsync(
                        latitude,
                        longitude,
                        availability -> {
                            if (availability != VpsAvailability.AVAILABLE) {
                               Log.i("VPS:", "Vps is not available.");
                            }
                        });
    }

    /**
     * Creates a new ARCore Session, in case ARCore is installed
     */
    public void createSession() {
        if(session == null) {
            // Checks if ARCore is installed
            if (!checkARCoreInstallationStatus()) {
                return;
            }

            // Checks our camera permissions
            if (checkIfCameraPermissionHasBeenGranted()) {
                requestCameraPermission();
                return;
            }

            if(!LocationPermissionHelper.hasFineLocationPermission(this)) {
                LocationPermissionHelper.requestFineLocationPermission(this);
                return;
            }

            try {
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                errorCreatingSession = true;
                Log.i(TAG, "Please install ARCore");
                return;
            } catch (UnavailableApkTooOldException e) {
                errorCreatingSession = true;
                Log.i(TAG, "Please update ARCore");
                return;
            } catch (UnavailableSdkTooOldException e) {
                errorCreatingSession = true;
                Log.i(TAG, "Please update this app");
                return;
            } catch (UnavailableDeviceNotCompatibleException e) {
                errorCreatingSession = true;
                Log.i(TAG, "This device does not support AR");
                return;
            } catch (Exception e) {
                errorCreatingSession = true;
                Log.e(TAG, "Failed to create ARCore session that supports camera sharing", e);
                return;
            }

            errorCreatingSession = false;

          /*  // Check VPS availability before configure and resume session.
            if (session != null) {
                getLastLocation();
            }*/

            try {
                // Earth mode may not be supported on this device due to insufficient sensor quality.
                if (!session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
                    state = State.UNSUPPORTED;
                    return;
                }else {
                    Log.i("Geospatial:", "Device supports GeospatialMode.");
                }

                // Enable auto focus mode while ARCore is running.
                Config sessionConfig = session.getConfig();
                sessionConfig = sessionConfig.setGeospatialMode(Config.GeospatialMode.ENABLED);

                /*
                Enables Geospatial capabilities. Needed for placing mushrooms.
                ENABLED -> App can gain geo information from the Visual Positioning System (VPS)
                 */

                session.configure(sessionConfig);
                state = State.PRETRACKING;
                 session.resume();
            } catch (CameraNotAvailableException e) {
                Log.i(TAG, "Camera not available. Try restarting the app.");
            }
        }
    }


    // Close the camera device.
    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
    }

    private void resumeARCore() {
        // Ensure that session is valid before triggering ARCore resume. Handles the case where the user
        // manually uninstalls ARCore while the app is paused and then resumes.
        if (session == null) {
            return;
        }

        if (!arcoreActive) {
            try {
                // To avoid flicker when resuming ARCore mode inform the renderer to not suppress rendering
                // of the frames with zero timestamp.
              //  backgroundRenderer.suppressTimestampZeroRendering(false);
                // Resume ARCore.
                session.resume();
                arcoreActive = true;

            } catch (CameraNotAvailableException e) {
                Log.e(TAG, "Failed to resume ARCore session", e);
                return;
            }
        }
    }

    private void pauseARCore() {
        if (arcoreActive) {
            // Pause ARCore.
            session.pause();
            isFirstFrameWithoutArcore.set(true);
            arcoreActive = false;
        }
    }

    /**
     * Copied and modified from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * @param render
     */
    @Override
    public void onSurfaceCreated(SampleRender render) {
        // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
        // an IOException.
        try {
            planeRenderer = new PlaneRenderer(render);
            backgroundRenderer = new BackgroundRenderer(render);
            virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

            // Virtual object to render (ARCore geospatial)
            Texture virtualObjectTexture =
                    Texture.createFromAsset(
                            render,
                            "models/spatial_marker_baked.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB);

            virtualObjectMesh = Mesh.createFromAsset(render, "models/geospatial_marker.obj");
            geospatialAnchorVirtualObjectShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/ar_unlit_object.vert",
                                    "shaders/ar_unlit_object.frag",
                                    /* defines= */ null)
                            .setTexture("u_Texture", virtualObjectTexture);

            // Virtual object to render (Terrain anchor marker)
            Texture terrainAnchorVirtualObjectTexture =
                    Texture.createFromAsset(
                            render,
                            "models/spatial_marker_yellow.png",
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB);
            terrainAnchorVirtualObjectShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/ar_unlit_object.vert",
                                    "shaders/ar_unlit_object.frag",
                                    /* defines= */ null)
                            .setTexture("u_Texture", terrainAnchorVirtualObjectTexture);

            backgroundRenderer.setUseDepthVisualization(render, false);
            backgroundRenderer.setUseOcclusion(render, false);

            // Point cloud
            pointCloudShader =
                    Shader.createFromAssets(
                                    render,
                                    "shaders/point_cloud.vert",
                                    "shaders/point_cloud.frag",
                                    /* defines= */ null)
                            .setVec4(
                                    "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
                            .setFloat("u_PointSize", 5.0f);
            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer =
                    new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
            final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
            pointCloudMesh =
                    new Mesh(
                            render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }



    }

    /**
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     *  surface changed callback.
     */
    @Override
    public void onSurfaceChanged(SampleRender render, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        virtualSceneFramebuffer.resize(width, height);
    }


    // Draw frame when in AR mode.
    @Override
    public void onDrawFrame(SampleRender render) {
        if (!arcoreActive) {
            // ARCore not yet active, so nothing to draw yet.
            return;
        }

        if (errorCreatingSession) {
            // Session not created, so nothing to draw.
            return;
        }

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(
                    new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
            hasSetTextureNames = true;
        }

        displayRotationHelper.updateSessionIfNeeded(session);

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e("Shroomy:", "Camera not available during onDrawFrame", e);
            return;
        }
        Camera camera = frame.getCamera();

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        earth = session.getEarth();
        if(earth != null) {
            updateGeospatialState(earth);
            checkVpsAvailability(earth.getCameraGeospatialPose().getLatitude(), earth.getCameraGeospatialPose().getLongitude());
        }


        //TODO REMOVE TEST EXAMPLE
        // Anchors camera position. Keeps tracking the camera. Means anchor is moving along
        if(!mySpotanchored) {
            createTerrainAnchor(earth, 53.057609, 8.800950, identityQuaternion);
            mySpotanchored = true;
        }

        // Places all the anchors for mushroom locations
        if(!mushroomsAnchorPlaced) {
            for(MarkerOptions marker : GameData.getMarkerData()) {
                createTerrainAnchor(earth, marker.getPosition().latitude, marker.getPosition().longitude, identityQuaternion);
            }
            mushroomsAnchorPlaced = true;
        }

        //TODO REMOVE AFTER TESTING
        // Handle user input.
        handleTap(frame, camera.getTrackingState());


        /*
        STARTING HERE BACKGROUND IS DRAWN!!
         */

        if (frame.getTimestamp() != 0) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render);
        }

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() != TrackingState.TRACKING || state != State.LOCALIZED) {
            return;
        }

        /*
         * STARTING HERE VIRTUAL OBJECTS ARE DRAWN!!
         */

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0);

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.getPoints());
                lastPointCloudTimestamp = pointCloud.getTimestamp();
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(pointCloudMesh, pointCloudShader);
        }

        // Visualize planes. -> The white mesh where anchors can be placed on top
        planeRenderer.drawPlanes(
                render,
                session.getAllTrackables(Plane.class),
                camera.getDisplayOrientedPose(),
                projectionMatrix);

        //TODO visualize Shroomy anchors extra?

        // Visualize anchors created by touch.
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);

        synchronized (anchorsLock) {
            for (Anchor anchor : anchors) {

                //TODO NEED TO REMOVE THIS MENTIONED UPDATING. SHROOMY SHOULD STAY WHERE IT WAS PLACED!

                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                // Only render resolved Terrain & Rooftop anchors and Geospatial anchors.
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                anchor.getPose().toMatrix(modelMatrix, 0);
                float[] scaleMatrix = new float[16];
                Matrix.setIdentityM(scaleMatrix, 0);
                float scale = getScale(anchor.getPose(), camera.getDisplayOrientedPose());
                scaleMatrix[0] = scale;
                scaleMatrix[5] = scale;
                scaleMatrix[10] = scale;
                Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
                // Rotate the virtual object 180 degrees around the Y axis to make the object face the GL
                // camera -Z axis, since camera Z axis faces toward users.
                float[] rotationMatrix = new float[16];
                Matrix.setRotateM(rotationMatrix, 0, 180, 0.0f, 1.0f, 0.0f);
                float[] rotationModelMatrix = new float[16];
                Matrix.multiplyMM(rotationModelMatrix, 0, modelMatrix, 0, rotationMatrix, 0);
                // Calculate model/view/projection matrices
                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, rotationModelMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

                // Update shader properties and draw
                if (terrainAnchors.contains(anchor)) {
                    terrainAnchorVirtualObjectShader.setMat4(
                            "u_ModelViewProjection", modelViewProjectionMatrix);

                    render.draw(virtualObjectMesh, terrainAnchorVirtualObjectShader, virtualSceneFramebuffer);
                    //TODO NEED TO REMOVE GEO ANCHORS
                } else {
                    geospatialAnchorVirtualObjectShader.setMat4(
                            "u_ModelViewProjection", modelViewProjectionMatrix);
                    render.draw(
                            virtualObjectMesh, geospatialAnchorVirtualObjectShader, virtualSceneFramebuffer);
                }
            }
        }
        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
    }

    /**
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * @param anchorPose
     * @param cameraPose
     * @return
     */
    // Return the scale in range [1, 2] after mapping a distance between camera and anchor to [2, 20].
    private float getScale(Pose anchorPose, Pose cameraPose) {
        double distance =
                Math.sqrt(
                        Math.pow(anchorPose.tx() - cameraPose.tx(), 2.0)
                                + Math.pow(anchorPose.ty() - cameraPose.ty(), 2.0)
                                + Math.pow(anchorPose.tz() - cameraPose.tz(), 2.0));
        double mapDistance = Math.min(Math.max(2, distance), 20);
        return (float) (mapDistance - 2) / (20 - 2) + 1;
    }

    /**
     * TODO REMOVE THIS AFTER TESTING
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * Handles the most recent user tap.
     *
     * <p>We only ever handle one tap at a time, since this app only allows for a single anchor.
     *
     * @param frame the current AR frame
     * @param cameraTrackingState the current camera tracking state
     */
    private void handleTap(Frame frame, TrackingState cameraTrackingState) {
        // Handle taps. Handling only one tap per frame, as taps are usually low frequency
        // compared to frame rate.
        synchronized (singleTapLock) {
            synchronized (anchorsLock) {
                if (queuedSingleTap == null
                        || anchors.size() >= MAXIMUM_ANCHORS
                        || cameraTrackingState != TrackingState.TRACKING) {
                    queuedSingleTap = null;
                    return;
                }
            }
            earth = session.getEarth();
            if (earth == null || earth.getTrackingState() != TrackingState.TRACKING) {
                queuedSingleTap = null;
                return;
            }

            for (HitResult hit : frame.hitTest(queuedSingleTap)) {
                if (shouldCreateAnchorWithHit(hit)) {
                    Pose hitPose = hit.getHitPose();
                    GeospatialPose geospatialPose = earth.getGeospatialPose(hitPose);
                    createAnchorWithGeospatialPose(earth, geospatialPose);
                    break; // Only handle the first valid hit.
                }
            }
            queuedSingleTap = null;
        }
    }

    /**
     * TODO REMOVE THIS AFTER TESTING
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * Creates anchor with the provided GeospatialPose, either from camera or HitResult.
     */
    private void createAnchorWithGeospatialPose(Earth earth, GeospatialPose geospatialPose) {
        double latitude = geospatialPose.getLatitude();
        double longitude = geospatialPose.getLongitude();
        double altitude = geospatialPose.getAltitude();
        float[] quaternion = geospatialPose.getEastUpSouthQuaternion();
        switch (anchorType) {
            case TERRAIN:
                createTerrainAnchor(earth, latitude, longitude, identityQuaternion);
                storeAnchorParameters(latitude, longitude, 0, identityQuaternion);
                break;
        }
        runOnUiThread(
                () -> {
                    clearAnchorsButton.setVisibility(View.VISIBLE);
                });
        if (clearedAnchorsAmount != null) {
            clearedAnchorsAmount = null;
        }


    }

    /**
     * TODO REMOVE THIS AFTER TESTING
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * Helper function to store the parameters used in anchor creation in {@link SharedPreferences}.
     */
    private void storeAnchorParameters(
            double latitude, double longitude, double altitude, float[] quaternion) {
        Set<String> anchorParameterSet =
                sharedPreferences.getStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, new HashSet<>());
        HashSet<String> newAnchorParameterSet = new HashSet<>(anchorParameterSet);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        String type = "";
        switch (anchorType) {
            case TERRAIN:
                type = "Terrain";
                break;
            case ROOFTOP:
                type = "Rooftop";
                break;
            default:
                type = "";
                break;
        }
        newAnchorParameterSet.add(
                String.format(
                        type + "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                        latitude,
                        longitude,
                        altitude,
                        quaternion[0],
                        quaternion[1],
                        quaternion[2],
                        quaternion[3]));
        editor.putStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, newAnchorParameterSet);
        editor.commit();
    }


    /**
     * TODO REMOVE THIS AFTER TESTING
     * Copied from example project geospatial_java -> <a href="https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java">Git-Hub</a>
     * Returns {@code true} if and only if the hit can be used to create an Anchor reliably. */
    private boolean shouldCreateAnchorWithHit(HitResult hit) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof Plane) {
            // Check if the hit was within the plane's polygon.
            return ((Plane) trackable).isPoseInPolygon(hit.getHitPose());
        } else if (trackable instanceof Point) {
            // Check if the hit was against an oriented point.
            return ((Point) trackable).getOrientationMode() == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL;
        }
        return false;
    }

    public static void setFullScreenOnWindowFocusChanged(Activity activity, boolean hasFocus) {
        if (hasFocus) {
            // https://developer.android.com/training/system-ui/immersive.html#sticky
            activity
                    .getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * Handles the button that creates an anchor.
     *
     * <p>Ensure Earth is in the proper state, then create the anchor. Persist the parameters used to
     * create the anchors so that the anchors will be loaded next time the app is launched.
     */
    private void handleSetAnchorButton() {}

    // Android focus change callback.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

}