package com.example.myrtspplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myrtspplayer.databinding.ActivityMainBinding;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


public class MainActivity extends AppCompatActivity {
    private LibVLC libVLC;
    private MediaPlayer displayMediaPlayer;
    private MediaPlayer recordMediaPlayer;
    private boolean isRecording = false;
    private boolean isFullScreen = false;
    private ActivityMainBinding binding;
    private ConstraintLayout.LayoutParams defaultVideoViewParams;
    private File createdVideo;

    private float currentScale = PORTRAIT_DEFAULT_SCALE; // Define currentScale
    private float minimumScale = PORTRAIT_DEFAULT_SCALE; // Define currentScale

    private static final float SCALE_INCREMENT = 0.1f; // Define scaleIncrement
    private static final float PORTRAIT_DEFAULT_SCALE = .6f;
    private static final float FULL_SCREEN_DEFAULT_SCALE = .75f;


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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 101);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            }
        }
    }

    private void initializePlayer() {
        libVLC = new LibVLC(this);
        defaultVideoViewParams = (ConstraintLayout.LayoutParams) binding.contentMain.videoView.getLayoutParams();
        setupVideoView();
        setupUIInteractions();
    }

    private IMedia setupMedia() {
        String url = getString(R.string.rtspUrl);
        Media media = new Media(libVLC, Uri.parse(url));
        media.addOption("--aout=opensles");
        media.addOption("--audio-time-stretch");
        media.addOption("-vvv"); // verbosity

        return media;
    }

    private void addMediaRecordOptions(IMedia media) {
        if (createdVideo != null) {
            media.addOption(":sout=#duplicate{dst=display,dst=std{access=file,mux=ps,dst=" + createdVideo.getAbsolutePath() + "}");
            media.addOption(":sout-keep");
        }
    }

    private void setupUIInteractions() {
        binding.contentMain.zoomInButton.setOnClickListener(view -> zoomIn());
        binding.contentMain.zoomOutButton.setOnClickListener(view -> zoomOut());
        binding.contentMain.recordButton.setOnClickListener(view -> toggleRecording());
        binding.contentMain.fullScreenButton.setOnClickListener(view -> toggleFullScreen());
    }

    private void setupVideoView() {
        displayMediaPlayer = new MediaPlayer(libVLC);
        displayMediaPlayer.setMedia(setupMedia());
        updateVideoWindowSize();
        displayMediaPlayer.getVLCVout().setVideoSurface(binding.contentMain.videoView.getHolder().getSurface(), binding.contentMain.videoView.getHolder());
        displayMediaPlayer.getVLCVout().attachViews();
        displayMediaPlayer.play();
    }

    private void updateVideoWindowSize() {
        minimumScale = isFullScreen ? FULL_SCREEN_DEFAULT_SCALE : PORTRAIT_DEFAULT_SCALE;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            displayMediaPlayer.getVLCVout().setWindowSize(binding.contentMain.videoView.getWidth(), binding.contentMain.videoView.getHeight());
            displayMediaPlayer.setScale(minimumScale);
        }, 300);
    }

    private void toggleFullScreen() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            // Change to landscape mode
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            binding.contentMain.videoView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            updateVideoWindowSize();

        } else {
            // Change back to the original orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            binding.contentMain.videoView.setLayoutParams(defaultVideoViewParams);

            updateVideoWindowSize();
            displayMediaPlayer.setScale(.5f);
        }

        // Update layout constraints for buttons for landscape mode
        updateButtonLayout();
        binding.contentMain.fullScreenButton.setText(isFullScreen ? "Exit Full Screen" : "Full Screen");
    }

    private void updateButtonLayout() {
        // Set the constraints for the Zoom In Button
        LinearLayout container = binding.contentMain.buttonsContainer;
        ConstraintLayout.LayoutParams containerParams = (ConstraintLayout.LayoutParams) container.getLayoutParams();
        containerParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        containerParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        container.setLayoutParams(containerParams);
    }

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        try {
            // Start recording
            String fileName = System.currentTimeMillis() + "_recorded_video.mp4";
            File filesDir = getFilesDir();
            createdVideo = new File(filesDir, fileName);
            isRecording = true;

            recordMediaPlayer = new MediaPlayer(libVLC);
            IMedia media = setupMedia();
            addMediaRecordOptions(media);
            recordMediaPlayer.setMedia(media);

            // Restart the playback to record the stream
            recordMediaPlayer.play();
            binding.contentMain.recordButton.setText("Stop Recording");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (isRecording && recordMediaPlayer != null && recordMediaPlayer.getMedia() != null) {
            recordMediaPlayer.stop();
            recordMediaPlayer.release();

            saveRecordingToGallery();

            if (!createdVideo.delete()) {
                Log.e("Video recording", "Failed to delete temporary file");
            }

            isRecording = false;
            binding.contentMain.recordButton.setText("Record");
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveRecordingToGallery() {
        ContentResolver resolver = getContentResolver();
        ContentValues valuesVideo = new ContentValues();
        valuesVideo.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + getString(R.string.app_name));
        valuesVideo.put(MediaStore.Video.Media.TITLE, createdVideo.getName());
        valuesVideo.put(MediaStore.Video.Media.DISPLAY_NAME, createdVideo.getName());
        valuesVideo.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

        Uri uriSavedVideo;
        if (Build.VERSION.SDK_INT >= 29) {
            Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            uriSavedVideo = resolver.insert(collection, valuesVideo);
            valuesVideo.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            valuesVideo.put(MediaStore.Video.Media.IS_PENDING, 1);
        } else {
            valuesVideo.put(MediaStore.Video.Media.DATA, createdVideo.getAbsolutePath());
            uriSavedVideo = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesVideo);
        }
        if (uriSavedVideo != null) {
            ParcelFileDescriptor pfd;
            try {
                pfd = getContentResolver().openFileDescriptor(uriSavedVideo, "w");
                FileOutputStream out = null;
                if (pfd != null) {
                    out = new FileOutputStream(pfd.getFileDescriptor());
                }
                FileInputStream in = new FileInputStream(createdVideo);

                byte[] buf = new byte[8192];
                int len;
                if (out != null) {
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                    in.close();
                    pfd.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= 29) {
                valuesVideo.clear();
                valuesVideo.put(MediaStore.Video.Media.IS_PENDING, 0);
                getContentResolver().update(uriSavedVideo, valuesVideo, null, null);
            }
        }
    }

    private void zoomIn() {
        currentScale += SCALE_INCREMENT;
        displayMediaPlayer.setScale(currentScale);
    }

    private void zoomOut() {
        if (currentScale - SCALE_INCREMENT >= minimumScale) {
            currentScale -= SCALE_INCREMENT;
            displayMediaPlayer.setScale(currentScale);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (displayMediaPlayer != null) {
            displayMediaPlayer.stop();
            displayMediaPlayer.release();
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