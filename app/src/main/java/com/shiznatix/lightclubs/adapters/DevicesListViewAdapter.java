package com.shiznatix.lightclubs.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.shiznatix.lightclubs.R;
import com.shiznatix.lightclubs.entities.JuggleDevice;

import java.util.ArrayList;

public class DevicesListViewAdapter extends BaseAdapter {
    static final protected String LOG_TAG = "JL_" + DevicesListViewAdapter.class.getName();

    private Context mContext;
    private ArrayList<JuggleDevice> mBluetoothDevices;
    private LayoutInflater mLayoutInflater;

    public DevicesListViewAdapter(Context context, ArrayList<JuggleDevice> bluetoothDevices) {
        mContext = context;
        mBluetoothDevices = bluetoothDevices;
        mLayoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return mBluetoothDevices.size();
    }

    @Override
    public JuggleDevice getItem(int position) {
        return mBluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = mLayoutInflater.inflate(R.layout.list_item_devices, parent, false);
        }

        final JuggleDevice juggleDevice = getItem(position);

        Spinner deviceKeySpinner = convertView.findViewById(R.id.device_key_spinner);
        ArrayAdapter<CharSequence> deviceKeySpinnerAdapter = ArrayAdapter.createFromResource(
            mContext,
            R.array.juggle_device_keys,
            android.R.layout.simple_spinner_item
        );
        deviceKeySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceKeySpinner.setAdapter(deviceKeySpinnerAdapter);
        deviceKeySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                juggleDevice.key = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        deviceKeySpinner.setSelection(deviceKeySpinnerAdapter.getPosition(juggleDevice.key));

        ((TextView)convertView.findViewById(R.id.device_name))
                .setText(juggleDevice.device.getName());
        ((TextView)convertView.findViewById(R.id.device_address))
                .setText(juggleDevice.device.getAddress());

        Button deviceStateButton = convertView.findViewById(R.id.device_state);
        deviceStateButton.setOnClickListener(juggleDevice.stateChangeListener);
        deviceStateButton.setText(juggleDevice.getStateString());

        return convertView;
    }
}
