package com.shiznatix.lightclubs;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.shiznatix.lightclubs.adapters.DevicesListViewAdapter;
import com.shiznatix.lightclubs.entities.JuggleDevice;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "JL_" + MainActivity.class.getName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BROWSE_MUSIC = 2;
    private static final int REQUEST_BROWSE_SCRIPT = 3;
    private static final int REQUEST_PERMISSION_LOCATION = 4;

    private static final int BT_SCAN_PERIOD = 10000;

    private BluetoothAdapter mBtAdapter;
    private ArrayList<JuggleDevice> mJuggleDevices = new ArrayList<>();
    private boolean mScanning;
    private Handler mHandler = new Handler();
    private JugglePlayer mJugglePlayer;
    Preferences mPreferences;

    private Button mButtonScan;
    private TextView mTextAudioFile;
    private TextView mTextScriptFile;
    private Button mButtonPlayPause;
    private Button mButtonStop;
    private DevicesListViewAdapter mDevicesListViewAdapter;

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            deviceFound(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                deviceFound(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            Log.e(LOG_TAG, "Scan error: " + errorCode);
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(LOG_TAG, "Conection change to state: " + newState);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(LOG_TAG, "JuggleDevice: " + gatt.getDevice().getAddress() + " STATE_CONNECTED");

                    connectedToGatt(gatt);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(LOG_TAG, "JuggleDevice: " + gatt.getDevice().getAddress() + " STATE_DISCONNECTED");

                    disconnectedFromGatt(gatt);
                    break;
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattCharacteristic writeCharacteristic = JugglePlayer.getGattWriteCharacteristic(gatt);

            if (null == writeCharacteristic) {
                Log.e(LOG_TAG, "GATT write characteristic not found in discovered services, disconnecting");
                gatt.disconnect();
            } else {
                JuggleDevice juggleDevice = getJuggleDevice(gatt.getDevice().getAddress());

                if (null != juggleDevice) {
                    Log.i(LOG_TAG, "Writing juggle device write characteristic: " + juggleDevice.device.getAddress());
                    juggleDevice.writeCharacteristic = writeCharacteristic;
                } else {
                    Log.e(LOG_TAG, "Juggle device not found for write characteristic set!");
                }
            }
        }
    };

    private void scanDevices(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBtAdapter.getBluetoothLeScanner();

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setScanning(false);
                    bluetoothLeScanner.stopScan(mScanCallback);

                }
            }, BT_SCAN_PERIOD);

            setScanning(true);
            bluetoothLeScanner.startScan(mScanCallback);
        } else {
            setScanning(false);
            bluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    private void setScanning(boolean scanning) {
        mScanning = scanning;

        if (mScanning) {
            // first, remove all non-connected devices from our listview
            boolean devicesListModified = false;
            ArrayList<JuggleDevice> disconnectedDevices = new ArrayList<>();

            for (JuggleDevice juggleDevice : mJuggleDevices) {
                if (!juggleDevice.connected) {
                    disconnectedDevices.add(juggleDevice);
                    devicesListModified = true;
                }
            }

            if (devicesListModified) {
                mJuggleDevices.removeAll(disconnectedDevices);
                reloadListView();
            }

            mButtonScan.setText("Scanning...");
        } else {
            mButtonScan.setText("Scan");
        }
    }

    private void deviceFound(BluetoothDevice newDevice) {
        for (JuggleDevice juggleDevice : mJuggleDevices) {
            // if we already have this address in our list, don't duplicate the entry
            if (juggleDevice.device.getAddress().equals(newDevice.getAddress())) {
                return;
            }
        }

        Log.i(LOG_TAG, "New device: " + newDevice.getAddress());

        final JuggleDevice juggleDevice = new JuggleDevice(newDevice);
        juggleDevice.stateChangeListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "JuggleDevice clicked: " + juggleDevice.device.getAddress());

                if (null == juggleDevice.gatt) {
                    juggleDevice.device.connectGatt(MainActivity.this, false, mGattCallback);
                } else {
                    juggleDevice.gatt.disconnect();
                }
            }
        };
        mJuggleDevices.add(juggleDevice);
        reloadListView();
    }

    private JuggleDevice getJuggleDevice(String address) {
        for (JuggleDevice juggleDevice : mJuggleDevices) {
            if (juggleDevice.device.getAddress().equals(address)) {
                return juggleDevice;
            }
        }

        return null;
    }

    private void reloadListView() {
        // this might have been called from the bluetooth thread, just to be careful...
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDevicesListViewAdapter.notifyDataSetChanged();
            }
        });
    }

    private void connectedToGatt(BluetoothGatt gatt) {
        String address = gatt.getDevice().getAddress();
        JuggleDevice juggleDevice = getJuggleDevice(address);

        Log.i(LOG_TAG, "Connected to device: " + address);

        if (null != juggleDevice) {
            juggleDevice.gatt = gatt;
            juggleDevice.connected = true;

            reloadListView();
        }
    }

    private void disconnectedFromGatt(BluetoothGatt gatt) {
        String address = gatt.getDevice().getAddress();
        JuggleDevice juggleDevice = getJuggleDevice(address);

        Log.i(LOG_TAG, "Disconnected from device: " + address);

        if (null != juggleDevice) {
            juggleDevice.gatt = null;
            juggleDevice.writeCharacteristic = null;
            juggleDevice.connected = false;

            reloadListView();
        }
    }

    private String getFileName(String file) {
        if (null == file) {
            return null;
        }

        String fileName = null;

        String[] parts = file.split("/");

        try {
            String filePath = URLDecoder.decode(parts[parts.length - 1], "UTF-8");
            String[] filePathParts = filePath.split("/");
            fileName = filePathParts[filePathParts.length - 1];

        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return fileName;
    }

    private void getFileForResult(int resultCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, resultCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        boolean locationPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!locationPermitted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        }

        mPreferences = new Preferences(this);
        mJugglePlayer = new JugglePlayer(this, mJuggleDevices);

        mButtonScan = findViewById(R.id.button_scan);
        mButtonScan.setEnabled(locationPermitted);
        mButtonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "Scan button clicked");

                scanDevices(!mScanning);
            }
        });

        Button buttonBrowseAudio = findViewById(R.id.button_browse_audio);
        buttonBrowseAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "Browse audio button clicked");

                getFileForResult(REQUEST_BROWSE_MUSIC);
            }
        });

        String lastAudioFile = mPreferences.getAudioFile();
        String lastAudioFileName = getFileName(lastAudioFile);
        boolean lastAudioFileExists = (null != lastAudioFileName);
        mTextAudioFile = findViewById(R.id.text_audio_file);
        mTextAudioFile.setText((!lastAudioFileExists ? "Not set..." : lastAudioFileName));

        Button buttonBrowseScript = findViewById(R.id.button_browse_script);
        buttonBrowseScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "Browse script button clicked");

                getFileForResult(REQUEST_BROWSE_SCRIPT);
            }
        });

        String lastScriptFile = mPreferences.getScriptFile();
        String lastScriptFileName = getFileName(lastScriptFile);
        boolean lastScriptFileExists = (null != lastScriptFileName);
        mTextScriptFile = findViewById(R.id.text_script_file);
        mTextScriptFile.setText((!lastScriptFileExists ? "Not set..." : lastScriptFileName));

        boolean filesReady = (lastScriptFileExists && lastAudioFileExists);

        mButtonPlayPause = findViewById(R.id.button_play_pause);
        mButtonPlayPause.setEnabled(filesReady);
        mButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "Play/pause button clicked");

                mJugglePlayer.playPause();
            }
        });

        mButtonStop = findViewById(R.id.button_stop);
        mButtonStop.setEnabled(filesReady);
        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "Stop button clicked");

                mJugglePlayer.stop();
            }
        });

        mDevicesListViewAdapter = new DevicesListViewAdapter(getBaseContext(), mJuggleDevices);
        ListView listViewDevices = findViewById(R.id.list_view_devices);
        listViewDevices.setAdapter(mDevicesListViewAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();

        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (lastAudioFileExists) {
            Uri lastAudioUri = Uri.parse(lastAudioFile);
            getContentResolver().takePersistableUriPermission(lastAudioUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mJugglePlayer.setAudioFile(lastAudioUri);
        }
        if (lastScriptFileExists) {
            mJugglePlayer.setScriptFile(Uri.parse(lastScriptFile));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_BROWSE_MUSIC && requestCode != REQUEST_BROWSE_SCRIPT) {
            return;
        }

        if (null == data) {
            return;
        }

        Uri result = data.getData();
        getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String fileName = getFileName(result.toString());

        if (null == fileName) {
            return;
        }

        if (requestCode == REQUEST_BROWSE_MUSIC) {
            mJugglePlayer.setAudioFile(result);
            mTextAudioFile.setText(fileName);
            mPreferences.setAudioFile(result.toString());
        } else {
            mJugglePlayer.setScriptFile(result);
            mTextScriptFile.setText(fileName);
            mPreferences.setScriptFile(result.toString());
        }

        if (mJugglePlayer.readyToPlay()) {
            mButtonPlayPause.setEnabled(true);
            mButtonStop.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mButtonScan.setEnabled(true);
                }
            }
        }
    }
}
