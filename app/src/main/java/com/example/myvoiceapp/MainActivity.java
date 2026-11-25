package com.example.myvoiceapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class MainActivity extends AppCompatActivity {

    // BURAYA KENDİ AGORA APP ID'Nİ YAZ (console.agora.io'dan ücretsiz al)
    private static final String APP_ID = "0c5832db611a4bb8b7630a6b15993adc";
    // Basit test için token boş: Agora Console'da "App Certificate"i boş bırakırsan token gerekmez
    private static final String TOKEN = "";

    private RtcEngine rtcEngine;
    private EditText roomInput;
    private TextView statusText;
    private boolean muted = false;
    private boolean joined = false;

    private final IRtcEngineEventHandler handler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> statusText.setText("Odadasın: " + channel));
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Bir kullanıcı katıldı: " + uid, Toast.LENGTH_SHORT).show());
        }

       @Override
public void onUserOffline(int uid, int reason) {
    runOnUiThread(() -> Toast.makeText(MainActivity.this,
            "Kullanıcı ayrıldı: " + uid, Toast.LENGTH_SHORT).show());
} 
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            runOnUiThread(() -> statusText.setText("Bağlı değilsin"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomInput = findViewById(R.id.roomInput);
        statusText = findViewById(R.id.statusText);
        Button joinBtn = findViewById(R.id.joinBtn);
        Button leaveBtn = findViewById(R.id.leaveBtn);
        Button muteBtn = findViewById(R.id.muteBtn);

        joinBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
            joinRoom();
        });

        leaveBtn.setOnClickListener(v -> leaveRoom());

        muteBtn.setOnClickListener(v -> {
            if (rtcEngine != null) {
                muted = !muted;
                rtcEngine.muteLocalAudioStream(muted);
                muteBtn.setText(muted ? "Mikrofonu Aç" : "Mikrofonu Kapat");
            }
        });

        initEngine();
    }

    private void initEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = APP_ID;
            config.mEventHandler = handler;
            rtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            Toast.makeText(this, "Engine hatası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void joinRoom() {
        String channel = roomInput.getText().toString().trim();
        if (channel.isEmpty() || rtcEngine == null) {
            Toast.makeText(this, "Oda adı gir", Toast.LENGTH_SHORT).show();
            return;
        }
        rtcEngine.joinChannel(TOKEN, channel, "", 0);
        joined = true;
    }

    private void leaveRoom() {
        if (rtcEngine != null && joined) {
            rtcEngine.leaveChannel();
            joined = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            new Thread(() -> {
                RtcEngine.destroy();
                rtcEngine = null;
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            joinRoom();
        } else {
            Toast.makeText(this, "Mikrofon izni gerekli", Toast.LENGTH_SHORT).show();
        }
    }
}
