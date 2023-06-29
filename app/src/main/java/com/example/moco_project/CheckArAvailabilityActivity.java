package com.example.moco_project;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
//import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;

public class CheckArAvailabilityActivity extends AppCompatActivity {

    /*
     requestInstall(Activity, true) will trigger installation of Google Play Services for AR if
     necessary.
     */
    private boolean mUserRequestedInstall = true;
    private Session session;
    private Button mArButton;
    private boolean installRequested;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_ar_availability);
        mArButton = findViewById(R.id.arButton);
        mArButton.setVisibility(View.INVISIBLE);
        // Enable AR-related functionality on ARCore supported devices only.
        checkCameraPermission();
    }

    /**
     * Checks if AR is supported on the mobile device.
     * If yes, the AR button will be shown. If not, the device is unsupported or unknown
     */

    void maybeEnableArButton() {

        ArCoreApk.getInstance().checkAvailabilityAsync(this, availability -> {
            if (availability.isSupported()) {
                mArButton.setVisibility(View.VISIBLE);
                mArButton.setEnabled(true);
            } else { // The device is unsupported or unknown.
                mArButton.setVisibility(View.INVISIBLE);
                mArButton.setEnabled(false);
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

                // Create the session.
                session = new Session(/* context= */ this);
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
                Snackbar.make(this.getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
             //   messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
            }
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
                maybeEnableArButton();

            } else {
                //
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Log.i(TAG,"PERMISSION is not granted.");
            }
        });

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            Log.i(TAG,"PERMISSION has been granted before.");
            maybeEnableArButton();

        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            Log.i(TAG,"REQUESTING PERMISSION.");
            requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA);
        }



    }



}