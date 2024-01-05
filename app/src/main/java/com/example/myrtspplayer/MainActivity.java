package com.example.myrtspplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.myrtspplayer.databinding.ActivityMainBinding;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.IOException;
import android.content.pm.ActivityInfo;


public class MainActivity extends AppCompatActivity {
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isFullScreen = false;
    private ActivityMainBinding binding;
    private ConstraintLayout.LayoutParams defaultVideoViewParams;
    private float currentScale = 1.0f; // Define currentScale
    private final float scaleIncrement = 0.1f; // Define scaleIncrement

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        checkAndRequestPermissions();
        initializePlayer();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    private void initializePlayer() {
        libVLC = new LibVLC(this);
        String url = getString(R.string.rtspUrl);
        Media media = new Media(libVLC, Uri.parse(url));
        media.addOption("--aout=opensles");
        media.addOption("--audio-time-stretch");
        media.addOption("-vvv"); // verbosity

        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.setMedia(media);

        defaultVideoViewParams = (ConstraintLayout.LayoutParams) binding.contentMain.videoView.getLayoutParams();
        setupVideoView();
        setupUIInteractions();
    }

    private void setupUIInteractions() {
        binding.contentMain.zoomInButton.setOnClickListener(view -> zoomIn());
        binding.contentMain.zoomOutButton.setOnClickListener(view -> zoomOut());
        binding.contentMain.recordButton.setOnClickListener(view -> toggleRecording());
        binding.contentMain.fullScreenButton.setOnClickListener(view -> toggleFullScreen());
    }

    private void setupVideoView() {
        mediaPlayer.getVLCVout().setWindowSize(binding.contentMain.videoView.getWidth(), binding.contentMain.videoView.getHeight());
        mediaPlayer.getVLCVout().setVideoSurface(binding.contentMain.videoView.getHolder().getSurface(), binding.contentMain.videoView.getHolder());
        mediaPlayer.getVLCVout().attachViews();
        mediaPlayer.play();
    }

    private void toggleFullScreen() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            // Change to landscape mode
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            binding.contentMain.videoView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            // Update layout constraints for buttons for landscape mode
            updateButtonLayoutForLandscape(
                    binding.contentMain.zoomInButton,
                    binding.contentMain.zoomOutButton,
                    binding.contentMain.recordButton
            );

            // Make buttons visible
            binding.contentMain.zoomInButton.setVisibility(View.VISIBLE);
            binding.contentMain.zoomOutButton.setVisibility(View.VISIBLE);
            binding.contentMain.recordButton.setVisibility(View.VISIBLE);
        } else {
            // Change back to the original orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            binding.contentMain.videoView.setLayoutParams(defaultVideoViewParams);

            // Reset layout constraints for zoom buttons for portrait mode
            resetZoomButtonLayout(binding.contentMain.zoomInButton, binding.contentMain.videoView.getId());
            resetZoomButtonLayout(binding.contentMain.zoomOutButton, binding.contentMain.zoomInButton.getId());

            // Hide zoom buttons or reset their layout
            binding.contentMain.zoomInButton.setVisibility(View.GONE);
            binding.contentMain.zoomOutButton.setVisibility(View.GONE);
        }
        binding.contentMain.fullScreenButton.setText(isFullScreen ? "Exit Full Screen" : "Full Screen");
    }

    private void updateButtonLayoutForLandscape(Button zoomInButton, Button zoomOutButton, Button recordButton) {
        // Set the constraints for the Zoom In Button
        ConstraintLayout.LayoutParams zoomInParams = (ConstraintLayout.LayoutParams) zoomInButton.getLayoutParams();
        zoomInParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        zoomInParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        zoomInParams.bottomToTop = zoomOutButton.getId();
        zoomInParams.verticalBias = 0.5f; // Adjust this value as needed
        zoomInParams.setMargins(0, 0, 16, 0); // Right margin to push it from the right edge
        zoomInButton.setLayoutParams(zoomInParams);

        // Set the constraints for the Zoom Out Button
        ConstraintLayout.LayoutParams zoomOutParams = (ConstraintLayout.LayoutParams) zoomOutButton.getLayoutParams();
        zoomOutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        zoomOutParams.topToBottom = zoomInButton.getId();
        zoomOutParams.bottomToTop = recordButton.getId();
        zoomOutParams.verticalBias = 0.5f; // Adjust this value as needed
        zoomOutParams.setMargins(0, 0, 16, 0); // Right margin to push it from the right edge
        zoomOutButton.setLayoutParams(zoomOutParams);

        // Set the constraints for the Record Button
        ConstraintLayout.LayoutParams recordParams = (ConstraintLayout.LayoutParams) recordButton.getLayoutParams();
        recordParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        recordParams.topToBottom = zoomOutButton.getId();
        recordParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        recordParams.verticalBias = 0.5f; // Adjust this value as needed
        recordParams.setMargins(0, 0, 16, 0); // Right margin to push it from the right edge
        recordButton.setLayoutParams(recordParams);
    }



    private void resetZoomButtonLayout(Button button, int startToStartOf) {
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) button.getLayoutParams();
        layoutParams.startToStart = startToStartOf;
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        layoutParams.topToTop = ConstraintLayout.LayoutParams.UNSET;
        layoutParams.bottomToBottom = binding.contentMain.videoView.getId();
        layoutParams.setMargins(16, 16, 0, 0);
        button.setLayoutParams(layoutParams);
    }



    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        if (mediaPlayer != null && mediaPlayer.getMedia() != null) {
            try {
                String filename = "recorded_video.mp4";
                File storageDir = getExternalFilesDir(null);
                File videoFile = new File(storageDir, filename);

                // Start recording
                mediaPlayer.getMedia().addOption(":sout=#transcode{vcodec=h264,acodec=mpga,vb=800,ab=128,deinterlace}:file{dst=" + videoFile.getAbsolutePath() + "}");
                mediaPlayer.getMedia().addOption(":sout-keep");

                mediaPlayer.play(); // Restart the playback to record the stream
                isRecording = true;
                binding.contentMain.recordButton.setText("Stop Recording");
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        if (isRecording) {
            mediaPlayer.getMedia().addOption(":sout");
            mediaPlayer.stop(); // Stop playback to stop recording the stream
            isRecording = false;
            binding.contentMain.recordButton.setText("Record");
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void zoomIn() {
        currentScale += scaleIncrement;
        mediaPlayer.setScale(currentScale);
    }

    private void zoomOut() {
        if (currentScale - scaleIncrement >= 1.0f) {
            currentScale -= scaleIncrement;
            mediaPlayer.setScale(currentScale);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}