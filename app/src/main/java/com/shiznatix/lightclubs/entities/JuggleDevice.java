package com.shiznatix.lightclubs.entities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.view.View;

public class JuggleDevice {
    static public String STATE_CONNECTED_STRING = "Connected";
    static public String STATE_DISCONNECTED_STRING = "Disconnected";

    public String key = "";
    public BluetoothDevice device;
    public BluetoothGatt gatt;
    public BluetoothGattCharacteristic writeCharacteristic;
    public boolean connected = false;
    public View.OnClickListener stateChangeListener;

    public JuggleDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getStateString() {
        return (connected ? STATE_CONNECTED_STRING : STATE_DISCONNECTED_STRING);
    }
}
