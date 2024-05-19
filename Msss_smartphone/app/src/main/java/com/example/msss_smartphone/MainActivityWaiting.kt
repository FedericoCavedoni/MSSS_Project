package com.example.msss_smartphone

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi

class MainActivityWaiting : ComponentActivity() {
    private val TAG = "MainWaitingThread"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_waiting)

        val extras = intent.extras
        val selectedDevice: String? = extras?.getString("SELECTED_DEVICE")

        if(selectedDevice == null){
            Log.d(TAG, "Error with the device selection")
            return
        }

        bluetoothManager.setContext(this)
        bluetoothManager.setSelectedDeviceName(selectedDevice)

        val delayMillis = 4000L

        bluetoothManager.setConnectionCallback(object : BluetoothManager.BluetoothConnectionCallback {
            override fun onConnectionSuccess() {
                val intent = Intent(this@MainActivityWaiting, MainActivity::class.java)
                intent.putExtra("SELECTED_DEVICE", selectedDevice)
                startActivity(intent)
                finish()

                Log.d(TAG, "onsuccess")
            }

            override fun onConnectionFailure(errorMessage: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivityWaiting, "The selected bluetooth device is not available", Toast.LENGTH_SHORT).show()
                }
                finish()

                Log.d(TAG, "onfail")
            }
        })

        Handler().postDelayed({
            bluetoothManager.connectToBluetoothServer()

            Log.d(TAG, "pippo")
        }, delayMillis)
    }
}
