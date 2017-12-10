package com.shiznatix.lightclubs;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.shiznatix.lightclubs.entities.JuggleDevice;
import com.shiznatix.lightclubs.entities.ScriptFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

class JugglePlayer {
    private static final String LOG_TAG = "JL_" + JugglePlayer.class.getName();

    private static final String GATT_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final String GATT_CHARACTERISTIC = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";

    private Activity mActivity;
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private ArrayList<JuggleDevice> mJuggleDevices;
    private Map<String, ArrayList<ScriptFrame>> mPlaylists = new HashMap<>();
    private boolean mMediaPlayerReady = false;

    static BluetoothGattCharacteristic getGattWriteCharacteristic(BluetoothGatt gatt) {
        UUID serviceUuid = UUID.fromString(GATT_SERVICE);
        UUID characteristicUuid = UUID.fromString(GATT_CHARACTERISTIC);

        for (BluetoothGattService check : gatt.getServices()) {
            Log.i(LOG_TAG, "Services: " + check.getUuid().toString());
        }

        BluetoothGattService service = gatt.getService(serviceUuid);

        if (null == service) {
            Log.i(LOG_TAG, "GATT service does not exist");
            return null;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);

        if (null == characteristic) {
            Log.i(LOG_TAG, "GATT characteristic does not exist");
            return null;
        }

        return characteristic;
    }

    JugglePlayer(Activity activity, ArrayList<JuggleDevice> juggleDevices) {
        mActivity = activity;
        mJuggleDevices = juggleDevices;
    }

    void setAudioFile(Uri uri) {
        Log.i(LOG_TAG, "Set audio file: '" + uri.toString() + "'");

        mMediaPlayerReady = false;
        stop();

        mMediaPlayer = MediaPlayer.create(mActivity, uri);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.i(LOG_TAG, "MediaPlayer is ready");
                mMediaPlayerReady = true;
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(LOG_TAG, "MediaPlayer completed");
                stopTimer();
            }
        });
    }

    void setScriptFile(Uri uri) {
        Log.i(LOG_TAG, "Set script file: '" + uri.toString() + "'");

        ScriptFileParser scriptFileParser = new ScriptFileParser(mActivity.getContentResolver(), uri);
        mPlaylists = scriptFileParser.getPlaylists();
        scriptFileParser.closeStream();
        stop();
    }

    boolean readyToPlay() {
        return (mMediaPlayerReady && mPlaylists.size() > 0);
    }

    void playPause() {
        if (mMediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    void stop() {
        Log.i(LOG_TAG, "Stop");

        if (mMediaPlayerReady) {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(0);
        }

        stopTimer();
    }

    private void pause() {
        Log.i(LOG_TAG, "Pause");

        if (mMediaPlayerReady) {
            mMediaPlayer.pause();
        }

        stopTimer();
    }

    private void stopTimer() {
        Log.i(LOG_TAG, "Stop timer");

        for (Object o : mPlaylists.entrySet()) {
            Map.Entry playlist = (Map.Entry) o;
            ArrayList<ScriptFrame> scriptFrames = (ArrayList<ScriptFrame>) playlist.getValue();

            for (ScriptFrame scriptFrame : scriptFrames) {
                if (null != scriptFrame.timer) {
                    scriptFrame.timer.cancel();
                    scriptFrame.timer.purge();
                    scriptFrame.timer = null;
                }
            }
        }
    }

    private void play() {
        Log.i(LOG_TAG, "Play");

        if (!readyToPlay()) {
            return;
        }

        mMediaPlayer.start();
        int timersMade = 0;

        for (final JuggleDevice juggleDevice : mJuggleDevices) {
            if (mPlaylists.containsKey(juggleDevice.key)) {
                for (final ScriptFrame scriptFrame : mPlaylists.get(juggleDevice.key)) {
                    if (timersMade < 10) {
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Log.i(LOG_TAG, "Write to " + juggleDevice.key + " - " + scriptFrame.message + " - " + scriptFrame.timerStart);
//                            if (!write(juggleDevice, scriptFrame.message)) {
//                                Log.e(LOG_TAG, "Could not write to " + juggleDevice.key + " - " + scriptFrame.message);
//                            }
                            }
                        }, scriptFrame.timerStart * 1000);
                        scriptFrame.timer = timer;
                    }
                    timersMade++;
                }
            }
        }

        Log.i(LOG_TAG, "Made " + timersMade + " timers");
    }

    private boolean write(JuggleDevice juggleDevice, String value) {
        if (null == juggleDevice.writeCharacteristic) {
            Log.e(LOG_TAG, "Cannot write to JuggleDevice, write characteristic not found");
            return false;
        }

        juggleDevice.writeCharacteristic.setValue(value);
        return juggleDevice.gatt.writeCharacteristic(juggleDevice.writeCharacteristic);
    }
}
