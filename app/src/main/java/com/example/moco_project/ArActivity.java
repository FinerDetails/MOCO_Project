package com.example.moco_project;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.Manifest;
import android.view.Surface;
import android.widget.Toast;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.util.EnumSet;
import java.util.List;

public class ArActivity extends AppCompatActivity {

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

    private CameraDevice.StateCallback cameraDeviceCallback; //TODO
    private CameraCaptureSession.CaptureCallback cameraCaptureCallback; //TODO
    private CameraCaptureSession.StateCallback cameraSessionStateCallback; //TODO
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewCaptureRequestBuilder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_ar_availability);
        // Enable AR-related functionality on ARCore supported devices only.
        checkCameraPermission();
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

    /**
     * Checks if Google Play Services for AR are installed
     */
    @Override
    protected void onResume() {
        super.onResume();

       if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }
                // Create an ARCore session that supports camera sharing if there is no existing session.
                session = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));
            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
             //   messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
            }
            useCamera();
        }
    }
    public void checkCameraPermission() {
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.i(TAG,"PERMISSION is granted.");

            } else {
                Log.i(TAG,"PERMISSION is not granted.");
            }
        });

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            Log.i(TAG,"PERMISSION has been granted before.");

        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            Log.i(TAG,"REQUESTING PERMISSION.");
            requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA);
        }
    }
    @SuppressLint("MissingPermission")
    public void useCamera(){
        // Store the ARCore shared camera reference.
        sharedCamera = session.getSharedCamera();

        // Store the ID of the camera that ARCore uses.
        cameraId = session.getCameraConfig().getCameraId();
        startBackgroundThread();
        // Wrap the callback in a shared camera callback.
        CameraDevice.StateCallback wrappedCallback =
                sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler);

        // Store a reference to the camera system service.
        cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        // Open the camera device using the ARCore wrapped callback.
        try {
            cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }
    //Run when the camera opens.
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        Log.d(TAG, "Camera device ID " + cameraDevice.getId() + " opened.");
        ArActivity.this.cameraDevice = cameraDevice;
        createCameraPreviewSession();
    }

    void createCameraPreviewSession() {
        try {
            // Create an ARCore-compatible capture request using `TEMPLATE_RECORD`.
            previewCaptureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            // Build a list of surfaces, starting with ARCore provided surfaces.
            List<Surface> surfaceList = sharedCamera.getArCoreSurfaces();

            // (Optional) Add a CPU image reader surface.
            //surfaceList.add(cpuImageReader.getSurface());

            // The list should now contain three surfaces:
            // 0. sharedCamera.getSurfaceTexture()
            // 1. …
            // 2. cpuImageReader.getSurface()

            // Add ARCore surfaces and CPU image surface targets.
            for (Surface surface : surfaceList) {
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
}