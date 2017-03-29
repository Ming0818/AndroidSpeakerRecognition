package com.blueyond.speakerrecog;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.microsoft.cognitive.speakerrecognition.SpeakerIdentificationClient;
import com.microsoft.cognitive.speakerrecognition.SpeakerIdentificationRestClient;
import com.microsoft.cognitive.speakerrecognition.contract.identification.CreateProfileResponse;
import com.microsoft.cognitive.speakerrecognition.contract.identification.EnrollmentOperation;
import com.microsoft.cognitive.speakerrecognition.contract.identification.IdentificationOperation;
import com.microsoft.cognitive.speakerrecognition.contract.identification.OperationLocation;
import com.microsoft.cognitive.speakerrecognition.contract.identification.Status;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.UUID;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;

public class MainActivity extends AppCompatActivity {

    private String filePath;
    private UUID profileId = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            SpeakerIdentificationClient client = new SpeakerIdentificationRestClient("c9f3f8f4093a48499dd20a32e293c683");

            if (profileId == null) {
                // Enroll
                try {
                    CreateProfileResponse response = client.createProfile("en-US");
                    profileId = response.identificationProfileId;

                    OperationLocation loc = client.enroll(new FileInputStream(filePath), profileId, false);

                    // Wait for enrollment status
                    EnrollmentOperation op = client.checkEnrollmentStatus(loc);
                    while (op.status == Status.RUNNING || op.status == Status.NOTSTARTED) {
                        op = client.checkEnrollmentStatus(loc);
                    }

                    // Bail out if it failed...
                    if (op.status != Status.SUCCEEDED)
                        return;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                // Identify using recorded audio
                OperationLocation loc = client.identify(new FileInputStream(filePath), Arrays.asList(profileId));

                // Wait for result
                IdentificationOperation op = client.checkIdentificationStatus(loc);
                while (op.status == Status.RUNNING || op.status == Status.NOTSTARTED) {
                    op = client.checkIdentificationStatus(loc);
                }

                // Show result to user
                if (op.status == Status.SUCCEEDED) {
                    new AlertDialog.Builder(this)
                            .setTitle("Identified User")
                            .setMessage("User was identified as: " + op.processingResult.identifiedProfileId)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                }
                            })

                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Unable to identify user")
                            .setMessage("Status: " + op.message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                }
                            })

                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (resultCode == RESULT_CANCELED) {
            Log.d("", ":(");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        filePath = getExternalCacheDir().getAbsolutePath() + "/recorded_audio.wav";
        int color = getResources().getColor(R.color.colorPrimaryDark);
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(filePath)
                .setColor(color)
                .setRequestCode(requestCode)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.MONO)
                .setSampleRate(AudioSampleRate.HZ_16000)
                .setAutoStart(true)
                .setKeepDisplayOn(true)

                // Start recording
                .record();
    }
}