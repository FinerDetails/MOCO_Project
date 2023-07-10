package com.example.moco_project;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.example.moco_project.helpers.DisplayRotationHelper;
import com.example.moco_project.helpers.TapHelper;
import com.example.moco_project.helpers.TrackingStateHelper;
import com.example.moco_project.rendering.ObjectRenderer;
import com.example.moco_project.rendering.PlaneRenderer;
import com.example.moco_project.rendering.PointCloudRenderer;
import com.google.android.gms.maps.model.LatLng;
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
import com.google.ar.core.ResolveAnchorOnTerrainFuture;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.VpsAvailabilityFuture;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ArActivity extends AppCompatActivity implements GLSurfaceView.Renderer,
 SurfaceTexture.OnFrameAvailableListener {

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
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
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

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();

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

    private final PlaneRenderer planeRenderer = new PlaneRenderer();

    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    private DisplayRotationHelper displayRotationHelper;

    private TapHelper tapHelper;

    private final AtomicBoolean automatorRun = new AtomicBoolean(false);

    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];

    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();

    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

    // Whether an error was thrown during session creation.
    private boolean errorCreatingSession = false;

    // Linear layout that contains preview image and status text.
    private LinearLayout imageTextLinearLayout;

    // Text view for displaying on screen status message.
    private TextView statusTextView;

    // Total number of CPU images processed.
    private int cpuImagesProcessed;

    private GeospatialPose ourCurrentLocation;

    private Earth earth;

    private VpsAvailabilityFuture future;

    private boolean geospatialModeSupported;

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
        /** The desired positioning confidence wasn't reached in time. */
        LOCALIZING_FAILED,
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

    /** ---------------------------------------------------------------------------------
     * METHODE AREA
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("ARCore:", "Creating ARActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_ar_availability);

        Bundle extraBundle = getIntent().getExtras();
        if (extraBundle != null && 1 == extraBundle.getShort("automator", (short) 0)) {
            automatorRun.set(true);
        }

        // GL surface view that renders camera preview image.
        surfaceView = findViewById(R.id.glsurfaceview);
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        displayRotationHelper = new DisplayRotationHelper(this);
        tapHelper = new TapHelper(this);
        surfaceView.setOnTouchListener(tapHelper);

        imageTextLinearLayout = findViewById(R.id.image_text_layout);
        statusTextView = findViewById(R.id.text_view);

        // Switch to allow pausing and resuming of ARCore.
        Switch arcoreSwitch = findViewById(R.id.arcore_switch);
        // Ensure initial switch position is set based on initial value of `arMode` variable.
        //arcoreSwitch.setChecked(arMode);
        Log.i("GameData:", String.valueOf(GameData.getIsArActivity()));
        arcoreSwitch.setChecked(GameData.getIsArActivity());

        arcoreSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {

                Log.i("ARCore:", "Switching to " + (checked ? "AR" : "non-AR") + " mode.");
                //Update the switch state
                if (compoundButton.isChecked()) {
                    statusTextView.setText("ARCore is activated");
                    Log.i("ARCore:", "ARCore is activated");
                    //arMode = true;
                    GameData.setIsArActivity(true);
                    resumeARCore();
                } else {
                    statusTextView.setText("ARCore was paused");
                    Log.i("ARCore:", "ARCore was paused");
                    //arMode = false;
                    pauseARCore();
                    GameData.setIsArActivity(false);
                    startActivity(new Intent(ArActivity.this, MapActivity.class));
                    //resumeCamera2();
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
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        waitUntilCameraCaptureSessionIsActive();
        startBackgroundThread();
        surfaceView.onResume();
        // When the activity starts and resumes for the first time, openCamera() will be called
        // from onSurfaceCreated(). In subsequent resumes we call openCamera() here.
        if(surfaceCreated) {
            openCamera();
        }
        displayRotationHelper.onResume();
        //How to get latitude of first marker that has been generated:
       /* Toast.makeText(this, "first mushroom latitude " +
                GameData.getMarkerData().get(0).getPosition().latitude, Toast.LENGTH_SHORT).show();*/
        statusTextView.setText("Mushroom:\nLatitude: " +
                        GameData.getMarkerData().get(0).getPosition().latitude +
                "\nLongitude: " + GameData.getMarkerData().get(0).getPosition().longitude);
        /*
        BTW the markers have IDs in their ".title" keys. IDs are given to markers when they are
        first generated. They are numbers starting from 0 and their type is String.
        When markers are clicked on the map, GameData.deleteMarkerByTitle(String title) is called.
        This function deletes the correct instance from the markerdata list.
        You can also get the last user location with GameData.getUserLocation()
         */
    }

    private void placeMushrooms() {
        if(earth != null) {
            updateGeospatialState(earth);
            Log.i("Shroomy:", String.valueOf(earth.getTrackingState()));
            /*if(earth.getTrackingState() != TrackingState.TRACKING) {
                Log.i("Shroomy:", String.valueOf(earth.getTrackingState()));
                Log.i("Shroomy:", String.valueOf(earth.getEarthState()));
                Log.i("Shroomy:", String.valueOf(session.getAllTrackables(Trackable.class)));
                return;
            }else if(earth.getTrackingState() == TrackingState.TRACKING) {

                // cameraGeospatialPose contains geodetic location, rotation, and confidences values.
                ourCurrentLocation = earth.getCameraGeospatialPose();

                future = session.checkVpsAvailabilityAsync(ourCurrentLocation.getLatitude(),
                        ourCurrentLocation.getLongitude(), null);
                if (future.getState() == FutureState.DONE) {
                    switch (future.getResult()) {
                        case AVAILABLE:
                            Log.i("Shroomy:", "VPS is available at this location");
                            LatLng mushroomPosition = GameData.getMarkerData().get(0).getPosition();

                            final ResolveAnchorOnTerrainFuture terrainFuture =
                                    earth.resolveAnchorOnTerrainAsync(mushroomPosition.latitude,
                                            mushroomPosition.longitude, 0.0f, 0, 0, 0, 0, (anchor, state) -> {
                                                if (state == Anchor.TerrainAnchorState.SUCCESS) {
                                                    Log.i("Shroomy:", "TerrainAnchor was created");
                                                } else {
                                                    Log.i("Shroomy:", "The anchor failed to resolve");
                                                }
                                            });
                            break;
                        case UNAVAILABLE:
                            Log.i("Shroomy", "VPS is unavailable at this location");
                            break;
                        case ERROR_NETWORK_CONNECTION:
                            Log.i("Shroomy:", "The external service could not be reached due to a network connection error.");
                            break;
                    }
                } else {
                    Log.i("Shroomy:", "FutureState not DONE");
                }
            }*/
        } else {
            Log.i("Shroomy:", "GeospatialMode not supported");
        }
    }

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

            return;
        }
    }

    /**
     * Handles the updating for @link State.LOCALIZING. In this state, wait for the horizontal and
     * orientation threshold to improve until it reaches your threshold.
     *
     * <p>If it takes too long for the threshold to be reached, this could mean that GPS data isn't
     * accurate enough, or that the user is in an area that can't be localized with StreetView.
     */
    private void updateLocalizingState(Earth earth) {
        GeospatialPose geospatialPose = earth.getCameraGeospatialPose();
        if (geospatialPose.getHorizontalAccuracy() <= LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
                && geospatialPose.getOrientationYawAccuracy()
                <= LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES) {
            state = State.LOCALIZED;
        }

       /* if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - localizingStartTimestamp)
                > LOCALIZING_TIMEOUT_SECONDS) {
            state = State.LOCALIZING_FAILED;
            return;
        }*/
    }

    @Override
    public void onPause() {
        shouldUpdateSurfaceTexture.set(false);
        surfaceView.onPause();
        waitUntilCameraCaptureSessionIsActive();
        displayRotationHelper.onPause();
        if (GameData.getIsArActivity()) {
            pauseARCore();
        }
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private synchronized void waitUntilCameraCaptureSessionIsActive() {
        while (!captureSessionChangesPossible) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                Log.e(TAG, "Unable to wait for a safe time to make changes to the capture session", e);
            }
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("sharedCameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // Stop background handler thread.
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while trying to join background handler thread", e);
            }
        }
    }

    /**
     * Verifies that ARCore is installed and using the current version.
     */
    public boolean checkARCoreStatus() {
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
     *
     * @return true --> was granted; false --> wasn't  granted
     */
    public boolean checkIfCameraPermissionHasBeenGranted() {
        Log.i(TAG, "PERMISSION has been granted before.");
        return (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Requests to use the camera. A pop up window will appear and asks for usage permission
     */
    public void requestCameraPermission() {
        Log.i(TAG, "REQUESTING PERMISSION.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
    }

    /**
     * Creates a new ARCore Session, in case ARCore is installed
     */
    public void createSession() {
        try {
            // Create an ARCore session that supports camera sharing.
            session = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));
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

        // Earth mode may not be supported on this device due to insufficient sensor quality.
        if (!session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
            state = State.UNSUPPORTED;
            return;
        }else {
            Log.i("Shroomy:", "Device supports GeospatialMode.");
        }

        // Enable auto focus mode while ARCore is running.
        Config sessionConfig = session.getConfig();
        sessionConfig = sessionConfig.setFocusMode(Config.FocusMode.AUTO).setGeospatialMode(Config.GeospatialMode.ENABLED);

        /*
        Enables Geospatial capabilities. Needed for placing mushrooms.
        ENABLED -> App can gain geo information from the Visual Positioning System (VPS)
         */

        Log.i("ShroomyInSession:", String.valueOf(geospatialModeSupported));
        session.configure(sessionConfig);
        state = State.PRETRACKING;
    }

        public void openCamera() {
        // Our camera is already opened and doesn't have to be opened again
        if (cameraDevice != null) {
            return;
        }

        // Checks our camera permissions
        if (!checkIfCameraPermissionHasBeenGranted()) {
            requestCameraPermission();
            return;
        }

        // Checks if ARCore ist installed
        if (!checkARCoreStatus()) {
            return;
        }

        if(session == null) {
            createSession();
            Log.i("ARSession:", "A new ARSession was created.");
           // placeMushrooms();
        }

        // Store the ARCore shared camera reference.
        sharedCamera = session.getSharedCamera();

        // Store the ID of the camera that ARCore uses.
        cameraId = session.getCameraConfig().getCameraId();

        try {

            // Wrap the callback in a shared camera callback.
            CameraDevice.StateCallback wrappedCallback = sharedCamera.createARDeviceStateCallback(
                    cameraDeviceCallback, backgroundHandler);

            // Store a reference to the camera system service.
            cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

            // Get the characteristics for the ARCore camera.
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.cameraId);

            // On Android P and later, get list of keys that are difficult to apply per-frame and can
            // result in unexpected delays when modified during the capture session lifetime.
            if (Build.VERSION.SDK_INT >= 28) {
                keysThatCanCauseCaptureDelaysWhenModified = characteristics.getAvailableSessionKeys();
                if (keysThatCanCauseCaptureDelaysWhenModified == null) {
                    // Initialize the list to an empty list if getAvailableSessionKeys() returns null.
                    keysThatCanCauseCaptureDelaysWhenModified = new ArrayList<>();
                }
            }

            // Prevent app crashes due to quick operations on camera open / close by waiting for the
            // capture session's onActive() callback to be triggered.
            captureSessionChangesPossible = false;

            // Open the camera device using the ARCore wrapped callback.
            cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    // Close the camera device.
    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            waitUntilCameraCaptureSessionIsActive();
            safeToExitApp.close();
            cameraDevice.close();
            safeToExitApp.block();
        }
    }

    /**
     * Creates a new capture session. TEMPLATE_RECORD makes shure that our capture request is
     * compatible with ARCore.
     * Through this we can switch between non-AR and AR mode at runtime
     */
    public void createCameraPreviewSession() {
        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());
            sharedCamera.getSurfaceTexture().setOnFrameAvailableListener(this);

            // Create an ARCore-compatible capture request using `TEMPLATE_RECORD`.
            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            // Build a list of surfaces, starting with ARCore provided surfaces.
            List<Surface> surfaceList = sharedCamera.getArCoreSurfaces();

            // Add ARCore surfaces and CPU image surface targets.

            // Surface list should now contain two surfaces:
            // 0. sharedCamera.getSurfaceTexture()
            // 1. …

            for (Surface surface : surfaceList) {
                Log.i("My Surface: ",surface.toString());
                previewCaptureRequestBuilder.addTarget(surface);
            }

            // Wrap our callback in a shared camera callback.
            CameraCaptureSession.StateCallback wrappedCallback =
                    sharedCamera.createARSessionStateCallback(cameraSessionStateCallback, backgroundHandler);

            // Create a camera capture session for camera preview using an ARCore wrapped callback.
            cameraDevice.createCaptureSession(surfaceList, wrappedCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }


    /**
     * Called when starting non-AR mode or switching to non-AR mode.
     * Also called when app starts in AR mode, or resumes in AR mode.
     */
    private void setRepeatingCaptureRequest() {
        try {
            setCameraEffects(previewCaptureRequestBuilder);

            captureSession.setRepeatingRequest(
                    previewCaptureRequestBuilder.build(), cameraCaptureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set repeating request", e);
        }
    }

    private <T> boolean checkIfKeyCanCauseDelay(CaptureRequest.Key<T> key) {
        if (Build.VERSION.SDK_INT >= 28) {
            // On Android P and later, return true if key is difficult to apply per-frame.
            return keysThatCanCauseCaptureDelaysWhenModified.contains(key);
        } else {
            // On earlier Android versions, log a warning since there is no API to determine whether
            // the key is difficult to apply per-frame. Certain keys such as CONTROL_AE_TARGET_FPS_RANGE
            // are known to cause a noticeable delay on certain devices.
            // If avoiding unexpected capture delays when switching between non-AR and AR modes is
            // important, verify the runtime behavior on each pre-Android P device on which the app will
            // be distributed. Note that this device-specific runtime behavior may change when the
            // device's operating system is updated.
            Log.w(
                    TAG,
                    "Changing "
                            + key
                            + " may cause a noticeable capture delay. Please verify actual runtime behavior on"
                            + " specific pre-Android P devices that this app will be distributed on.");
            // Allow the change since we're unable to determine whether it can cause unexpected delays.
            return false;
        }
    }

    // If possible, apply effect in non-AR mode, to help visually distinguish between from AR mode.
    private void setCameraEffects(CaptureRequest.Builder captureBuilder) {
        if (checkIfKeyCanCauseDelay(CaptureRequest.CONTROL_EFFECT_MODE)) {
            Log.w(TAG, "Not setting CONTROL_EFFECT_MODE since it can cause delays between transitions.");
        } else {
            Log.d(TAG, "Setting CONTROL_EFFECT_MODE to SEPIA in non-AR mode.");
            captureBuilder.set(
                    CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);
        }
    }

    /**
     * Uses the camera device state callback.
     * There is a reference of the camera device stored. We can start here a new capture session
     * for AR switching
     *
     * @param cameraDevice
     */
    private final CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " opened.");
            ArActivity.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " closed.");
            ArActivity.this.cameraDevice = null;
            safeToExitApp.open();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.w(TAG, "Camera device ID " + cameraDevice.getId() + " disconnected.");
            cameraDevice.close();
            ArActivity.this.cameraDevice = cameraDevice;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Camera device ID " + cameraDevice.getId() + " error " + error);
            cameraDevice.close();
            ArActivity.this.cameraDevice = null;
            // Fatal error. Quit application.
            finish();
        }
    };

    private void resumeCamera2() {
        setRepeatingCaptureRequest();
        sharedCamera.getSurfaceTexture().setOnFrameAvailableListener(this);
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
                backgroundRenderer.suppressTimestampZeroRendering(false);
                // Resume ARCore.
                session.resume();
                arcoreActive = true;

                // Set capture session callback while in AR mode.
                sharedCamera.setCaptureCallback(cameraCaptureCallback, backgroundHandler);
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

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        surfaceCreated = true;

        // Set GL clear color to black.
        GLES20.glClearColor(0f, 0f, 0f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the camera preview image texture. Used in non-AR and AR mode.
            backgroundRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(this);

            virtualObject.createOnGlThread(this, "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObjectShadow.createOnGlThread(
                    this, "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

            openCamera();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    // GL surface changed callback. Will be called on the GL thread.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        displayRotationHelper.onSurfaceChanged(width, height);

        runOnUiThread(
            () -> {
                // Adjust layout based on display orientation.
                imageTextLinearLayout.setOrientation(
                        width > height ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            });
    }

    // GL draw callback. Will be called each frame on the GL thread.
    @Override
    public void onDrawFrame(GL10 gl) {
        // Use the cGL clear color specified in onSurfaceCreated() to erase the GL surface.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (!shouldUpdateSurfaceTexture.get()) {
            // Not ready to draw.
            return;
        }

        // Handle display rotations.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            if (GameData.getIsArActivity()) {
                onDrawFrameARCore();
            } else {
                onDrawFrameCamera2();
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    // Draw frame when in non-AR mode. Called on the GL thread.
    public void onDrawFrameCamera2() {
        SurfaceTexture texture = sharedCamera.getSurfaceTexture();

        // ARCore may attach the SurfaceTexture to a different texture from the camera texture, so we
        // need to manually reattach it to our desired texture.
        if (isFirstFrameWithoutArcore.getAndSet(false)) {
            try {
                texture.detachFromGLContext();
            } catch (RuntimeException e) {
                // Ignore if fails, it may not be attached yet.
            }
            texture.attachToGLContext(backgroundRenderer.getTextureId());
        }

        // Update the surface.
        texture.updateTexImage();

        // Account for any difference between camera sensor orientation and display orientation.
        int rotationDegrees = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId);

        // Determine size of the camera preview image.
        Size size = session.getCameraConfig().getTextureSize();

        // Determine aspect ratio of the output GL surface, accounting for the current display rotation
        // relative to the camera sensor orientation of the device.
        float displayAspectRatio =
                displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId);

        // Render camera preview image to the GL surface.
        backgroundRenderer.draw(size.getWidth(), size.getHeight(), displayAspectRatio, rotationDegrees);
    }

    // Surface texture on frame available callback, used only in non-AR mode.
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // Log.d(TAG, "onFrameAvailable()");
    }

    /**
     * An Anchor describes a fixed location and orientation in the real world.
     * With getPose() you can get the current position of an anchor. In case
     * Session.update() is called somewhere the location of the anchor might change
     */
    private static class ColoredAnchor {
        public final Anchor anchor;
        public final float[] color;

        public ColoredAnchor(Anchor a, float[] color4f) {
            this.anchor = a;
            this.color = color4f;
        }
    }



    // Draw frame when in AR mode. Called on the GL thread.
    public void onDrawFrameARCore() throws CameraNotAvailableException {
        if (!arcoreActive) {
            // ARCore not yet active, so nothing to draw yet.
            return;
        }

        if (errorCreatingSession) {
            // Session not created, so nothing to draw.
            return;
        }

        // Perform ARCore per-frame update.
        Frame frame = session.update();
        Camera camera = frame.getCamera();

        // Handle screen tap.
        handleTap(frame, camera);

        // If frame is ready, render camera preview image to the GL surface.
        backgroundRenderer.draw(frame);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        earth = session.getEarth();
        Log.i("Shroomy:", String.valueOf(earth));
        if(earth != null) {
            updateGeospatialState(earth);
            Log.i("Shroomy:", String.valueOf(earth.getTrackingState()));
        }

        // Get projection matrix.
        float[] projmtx = new float[16];
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

        // Get camera matrix and draw.
        float[] viewmtx = new float[16];
        camera.getViewMatrix(viewmtx, 0);

        // Compute lighting from average intensity of the image.
        // The first three components are color scaling factors.
        // The last one is the average pixel intensity in gamma space.
        final float[] colorCorrectionRgba = new float[4];
        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewmtx, projmtx);
        }

        // Visualize planes.
        planeRenderer.drawPlanes(
                session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

        // Visualize anchors created by touch.
        float scaleFactor = 1.0f;
        for (ColoredAnchor coloredAnchor : anchors) {
            if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to sharedSession.update() as ARCore refines its estimate of the world.
            coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

            // Update and draw the model and its shadow.
            virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
            virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
            virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
            virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon
                Trackable trackable = hit.getTrackable();
                // Creates an anchor if a plane or an oriented point was hit.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                    // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size() >= 20) {
                        anchors.get(0).anchor.detach();
                        anchors.remove(0);
                    }

                    // Assign a color to the object for rendering based on the trackable type
                    // this anchor attached to. For AR_TRACKABLE_POINT, it's blue color, and
                    // for AR_TRACKABLE_PLANE, it's green color.
                    float[] objColor;
                    if (trackable instanceof Point) {
                        objColor = new float[]{66.0f, 133.0f, 244.0f, 255.0f};
                    } else if (trackable instanceof Plane) {
                        objColor = new float[]{139.0f, 195.0f, 74.0f, 255.0f};
                    } else {
                        objColor = new float[]{0f, 0f, 0f, 0f};
                    }

                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(new ColoredAnchor(hit.createAnchor(), objColor));
                    break;
                }
            }
        }
    }


    /**
     * Repeating camera capture session state callback
     *
     * @param session
     */
    CameraCaptureSession.StateCallback cameraSessionStateCallback = new CameraCaptureSession.StateCallback() {
        // Called when the camera capture session is first configured after the app
        // is initialized, and again each time the activity is resumed.
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session configured.");
            captureSession = session;
            if (GameData.getIsArActivity()) {
                setRepeatingCaptureRequest();
            // Note, resumeARCore() must be called in onActive(), not here.
            } else {
                // Calls `setRepeatingCaptureRequest()`.
                resumeCamera2();
            }
        }

        @Override
        public void onSurfacePrepared(
                @NonNull CameraCaptureSession session, @NonNull Surface surface) {
            Log.d(TAG, "Camera capture surface prepared.");
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session ready.");
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session active.");
            if (GameData.getIsArActivity() && !arcoreActive) {
                resumeARCore();
            }
            synchronized (ArActivity.this) {
                captureSessionChangesPossible = true;
                ArActivity.this.notify();
            }
        }

        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
            Log.w(TAG, "Camera capture queue empty.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "Camera capture session closed.");
        }
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure camera capture session.");
        }
    };


    /**
     * Repeating camera capture session capture callback.
     */
    private final CameraCaptureSession.CaptureCallback cameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull TotalCaptureResult result) {
            shouldUpdateSurfaceTexture.set(true);
        }

        @Override
        public void onCaptureBufferLost(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull Surface target,
                long frameNumber) {
            Log.e(TAG, "onCaptureBufferLost: " + frameNumber);
        }

        @Override
        public void onCaptureFailed(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull CaptureFailure failure) {
            Log.e(TAG, "onCaptureFailed: " + failure.getFrameNumber() + " " + failure.getReason());
        }

        @Override
        public void onCaptureSequenceAborted(
                @NonNull CameraCaptureSession session, int sequenceId) {
            Log.e(TAG, "onCaptureSequenceAborted: " + sequenceId + " " + session);
        }
    };


    /** Check to see if we need to show the rationale for this permission. */
    public static boolean shouldShowRequestPermissionRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
    }

    /** Launch Application Setting to grant permission. */
    public static void launchPermissionSettings(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }

    /** Callback for the result from requesting permissions. This method is invoked for every call on requestPermission */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if(!checkIfCameraPermissionHasBeenGranted()) {
            Log.i(TAG, "Camera permission is needed to run this application.");
            if(!shouldShowRequestPermissionRationale(this)) {
                launchPermissionSettings(this);
            }
            finish();
        }
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

    // Android focus change callback.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

}