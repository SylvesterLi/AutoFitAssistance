package com.wru.autofitassistance;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.bigkoo.svprogresshud.SVProgressHUD;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements DeviceListAdapter.DataSource, DeviceListAdapter.Delegate {
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String TAG  = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private RecyclerView deviceList;
    private DeviceListAdapter deviceListAdapter;
    private LinearLayoutManager layoutManager;
    private ArrayList<BluetoothDevice> devices;
    private BluetoothSocket mBluetoothSocket;
    private SVProgressHUD hud;
    private EditText messageInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devices = new ArrayList<>();
        deviceList = findViewById(R.id.deviceList);
        deviceListAdapter = new DeviceListAdapter(this);
        deviceListAdapter.dataSource = this;
        deviceListAdapter.delegate = this;
        deviceList.setAdapter(deviceListAdapter);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        deviceList.setLayoutManager(layoutManager);

        messageInput = findViewById(R.id.message_input);
    }

    public void initBluetooth(View v) {
        if(!mBluetoothAdapter.isEnabled()){
            Log.i(TAG, "blue tooth is disabled");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        } else {
            Log.i(TAG, "blue tooth is already enabled");
        }
    }

    public void showBondedDevices(View v) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.i(TAG, "showBondedDevices: ");
        devices.clear();
        Iterator it = pairedDevices.iterator();
        while (it.hasNext()) {
            BluetoothDevice device = (BluetoothDevice) it.next();
            devices.add(device);
            Log.i(TAG, "device: " + device + ", name: " + device.getName());
        }
        deviceListAdapter.notifyDataSetChanged();
    }

    public void beginSearch(View v) {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        Log.i(TAG, "beginSearch: ");

        hud = new SVProgressHUD(this);
        hud.showWithStatus("搜索...");

        devices.clear();
        deviceListAdapter.notifyDataSetChanged();
        mBluetoothAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
                deviceListAdapter.notifyDataSetChanged();
                Log.i(TAG, "find device: " + device + ", name: " + device.getName());
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "searching finished");
                hud.dismiss();
                hud = null;
            }
        }
    };

    @Override
    public int numberOfDevice() {
        return devices.size();
    }

    @Override
    public BluetoothDevice deviceAtIndex(int index) {
        return devices.get(index);
    }

    @Override
    public void didSelectDevice(BluetoothDevice device) {
        debugDevice(device);
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            device.createBond();
        }

        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            connectDevice(device);
        }
    }

    private void debugDevice(BluetoothDevice device) {
        String bondState;
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED: {
                bondState = "bonded";
                break;
            }
            case BluetoothDevice.BOND_BONDING: {
                bondState = "bonding";
                break;
            }
            case BluetoothDevice.BOND_NONE:
            default: {
                bondState = "none";
                break;
            }
        }
        Log.i(TAG, "device: " + device.getName() + ", bond state: " + bondState);
    }

    private void connectDevice(final BluetoothDevice device) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //通过和服务器协商的uuid来进行连接
                    mBluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                    mBluetoothSocket.connect();
                    Log.i(TAG, "connect to: " + device.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (mBluetoothSocket.isConnected()) {
                        Log.i(TAG, "connected");
                    } else {
                        Log.i(TAG, "connecting failed");
                    }
                }
            }
        }).run();
    }

    public void sendMessage(View v) {
        if (messageInput.getText().length() == 0) {
            return;
        }

        if (mBluetoothSocket == null || !mBluetoothSocket.isConnected()) {
            return;
        }

        try {
            OutputStream outputStream = mBluetoothSocket.getOutputStream();
            byte[] data = messageInput.getText().toString().getBytes();
            outputStream.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        messageInput.setText(null);
    }
}

