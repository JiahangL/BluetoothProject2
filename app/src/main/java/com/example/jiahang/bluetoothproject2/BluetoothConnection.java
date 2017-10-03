package com.example.jiahang.bluetoothproject2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Jiahang on 6/29/2017.
 */

public class BluetoothConnection extends Thread {
    private final String TAG = "ConnectTag";
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private BluetoothDevice mDevice;
    private Handler mHandler;
    private final UUID hc05 = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public BluetoothConnection(Context context, BluetoothAdapter bluetoothAdapter){
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        /*This is the BluetoothConnection's start(), which will invoke the start() of the
        * respective thread, depending on the state of each thread*/
        start();
    }

    public synchronized void start(){
        //Cancel any threads attempting a connection
        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        //Start an AcceptThread if one doesn't exists
        /*
        if(mAcceptThread == null){
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        */
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private void manageConnection(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
        //Start thread to manage the connection and perform transactions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out){
        //Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called");
        mConnectedThread.write(out);
    }

    /**Here are the individual threads*/
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(hc05);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            //Cancel discovery, since it slows down connection
            mBluetoothAdapter.cancelDiscovery();

            try{
                //Connect to remote device through socket. Blocking call
                mmSocket.connect();
            } catch (IOException e) {
                //Unable to connect, close socket and return
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "Could not close client socket", e1);
                }
            }

            // Connection was successful, pass the connected socket to a separate thread
            // to do work associated with connection
            manageConnection(mmSocket, mmDevice);
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Couldn't close client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        // Initialize class's local socket to the socket connected to device, and
        // initializes IO streams
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes=1024; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    Log.d(TAG, "before getting bytes from buffer");
                    //numBytes = mmInStream.read(mmBuffer);
                    Log.d(TAG, "return from reading inStream:"+mmInStream.read(mmBuffer));
                    String arduinoData = new String(mmBuffer, 0, numBytes);

                    Intent sentArduinoData = new Intent("sentArduinoData");
                    sentArduinoData.putExtra("theData", arduinoData);

                    Log.d(TAG, "THIS IS ARDUINO'S SENT DATA: " + arduinoData);

                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(sentArduinoData);
                    /*
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(69, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                    */
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                /*
                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        69, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
                */
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(68);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
