package com.google.android.gms.drive.sample.quickstart;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.MetadataChangeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.google.android.gms.drive.DriveId.decodeFromString;

/**
 * Android Drive Quickstart activity. This activity takes a photo and saves it
 * in Google Drive. The user is prompted with a pre-made dialog which allows
 * them to choose the file location.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "drive-quickstart";
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private GoogleApiClient mGoogleApiClient;
    private boolean isFileEmpty = true;
    private DriveId mDriveId;
    private ArrayList<String> driveIDs;

    /**
     * Create a new file and save it to Drive.
     */
    private void saveFileToDrive(String path) {
        File file = new File(path);
        int length = (int) file.length();

        byte[] byteFile = new byte[length];

        try {
            FileInputStream in = new FileInputStream(file);
            in.read(byteFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String ext = FilenameUtils.getExtension(path);
        final String filename = FilenameUtils.getBaseName(path);

        // allocate additional 10 bytes for extension of the file and 4 bytes for extension size
        byte[] bytesExtLength = ByteBuffer.allocate(4).putInt(ext.length()).array();

        Toast.makeText(getApplicationContext(), String.valueOf(length), Toast.LENGTH_LONG).show();
        final byte[] byteEncodedFile = new byte[length + 14];
        for (int i = 0; i < length; i++) {
            byteEncodedFile[i] = byteFile[i];
        }
        for (int i = 0; i < 4; i++) {
            byteEncodedFile[length + i] = bytesExtLength[i];
        }
        for (int i = 0; i < ext.length(); i++) {
            byteEncodedFile[length + i + 4] = (byte) ext.charAt(i);
        }
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveContentsResult>() {

                    @Override
                    public void onResult(DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();

                        try {
                            outputStream.write(byteEncodedFile);
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }
                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("application/octet-stream").setTitle(filename + ".bin").build();

                        // Create a file in the root folder
                        Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                .createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                                .setResultCallback(fileCallback);
                    }
                });

    }

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {

                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        System.out.println("Error while trying to create the file");
                        return;
                    }
                    mDriveId = result.getDriveFile().getDriveId();
                    System.out.println("Created a file with content: " + result.getDriveFile().getDriveId());
                    driveIDs.add(mDriveId.encodeToString());
                    System.out.println("driveID size is " + driveIDs.size());
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("driveIDs", driveIDs);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        // connect the client
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it. Called typically when the app is not
        // yet authorized, and an authorization dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        System.out.println("upload intent received");
        Log.i(TAG, "API client connected.");
        Toast.makeText(getApplicationContext(), "Drive API launched", Toast.LENGTH_LONG).show();
        driveIDs = new ArrayList<>();

        int reqCode = getIntent().getIntExtra("requestcode", -1);

        if (reqCode == -1) {
            finish();
        }

        // 100: upload
        if (isFileEmpty) {
            if (reqCode == 100) {
                String[] path = getIntent().getStringArrayExtra("path");
                for (int i = 0; i < path.length; i++) {
                    saveFileToDrive(path[i]);
                }
                isFileEmpty = false;
            }

            // 200: download
            if (reqCode == 200) {
                String driveIdAsString = getIntent().getStringExtra("driveId");
                downloadFile(decodeFromString(driveIdAsString));
                isFileEmpty = false;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    public void downloadFile(DriveId driveID) {
        DriveFile file = driveID.asDriveFile();
        Toast.makeText(getApplicationContext(), String.valueOf(mGoogleApiClient.isConnected()), Toast.LENGTH_LONG).show();
        Toast.makeText(getApplicationContext(), driveID.toString(), Toast.LENGTH_LONG).show();
        file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(driveContentsCallback);
    }

    private ResultCallback<DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveContentsResult>() {
                @Override
                public void onResult(DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        System.out.println("Error while opening the file contents");
                        return;
                    }
                    DriveContents contents = result.getDriveContents();
                    try {
                        byte[] bytes = IOUtils.toByteArray(contents.getInputStream());
                        int length = bytes.length;
                        Toast.makeText(getApplicationContext(), String.valueOf(length), Toast.LENGTH_LONG).show();
                        byte[] fileBytes = new byte[length - 14];
                        byte[] extLengthBytes = new byte[4];
                        for (int i = 0; i < length - 14; i++) {
                            fileBytes[i] = bytes[i];
                        }
                        for (int i = 0; i < 4; i++) {
                            extLengthBytes[i] = bytes[length - 14 + i];
                        }
                        int extLength = ByteBuffer.wrap(extLengthBytes).getInt();
                        byte[] extBytes = new byte[extLength];
                        for (int i = 0; i < extLength; i++) {
                            extBytes[i] = bytes[length - 10 + i];
                        }
                        String ext = new String(extBytes);
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("extension", ext);
                        returnIntent.putExtra("filebytes", fileBytes);
                        setResult(RESULT_OK, returnIntent);
                        finish();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

    public static void saveFile(byte[] fileBytes, String extension, String filename) throws IOException {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + filename + "." + extension);
        file.createNewFile();
        //write the bytes in file
        if (file.exists()) {
            OutputStream fo = new FileOutputStream(file);
            fo.write(fileBytes);
            fo.close();
            System.out.println("file created: " + file);
        }
    }
}
