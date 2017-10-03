package com.example.jiahang.bluetoothproject2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothConnection mBluetoothConnection;
    BluetoothDevice mBTDevice;
    int REQUEST_ENABLE_BT = 15;

    UUID hc05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Set<BluetoothDevice> pairedDevices;

    Button sendButton;
    EditText sendText;
    TextView sentText;

    StringBuilder message;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize widgets
        sendButton = (Button)findViewById(R.id.button);
        sendText = (EditText)findViewById(R.id.editText);
        sentText = (TextView)findViewById(R.id.textView);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("sentArduinoData"));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            for(BluetoothDevice device : pairedDevices){
                mBTDevice = device;
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                ParcelUuid[] deviceUUID = device.getUuids();
                UUID uuid = deviceUUID[0].getUuid();
                Toast.makeText(getApplicationContext(), deviceName + " , " + deviceHardwareAddress
                        + uuid, Toast.LENGTH_LONG).show();
            }
        }

        startBtConnection(mBTDevice, hc05);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte [] bytes = sendText.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        });

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theData");
            sentText.setText(text);
        }
    };

    public void startBtConnection(BluetoothDevice device, UUID uuid){
        mBluetoothConnection = new BluetoothConnection(MainActivity.this, mBluetoothAdapter);
        mBluetoothConnection.startClient(device, uuid);
    }
}
