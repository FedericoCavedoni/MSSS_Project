package com.example.msss_smartphone

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.annotation.RequiresApi

class BluetoothActivity : ComponentActivity() {
    //private val tag = "BluetoothActivity"

    private lateinit var deviceSpinner: Spinner
    private lateinit var connectButton: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        deviceSpinner = findViewById(R.id.device_spinner)
        connectButton = findViewById(R.id.connect_button)

        bluetoothSemaphore.acquire()

        bluetoothManager.setContext(this)
        val deviceNames = bluetoothManager.getDeviceNames()

        bluetoothSemaphore.release()

        val adapter: ArrayAdapter<String>

        if (deviceNames.isEmpty()) {
            adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("No devices found"))
            connectButton.isEnabled = false
        } else {
            adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter


        connectButton.setOnClickListener {
            if (deviceNames.isNotEmpty()) {
                val selectedDevice = deviceNames[deviceSpinner.selectedItemPosition]
                val intent = Intent(this, MainActivityWaiting::class.java)
                intent.putExtra("SELECTED_DEVICE", selectedDevice)
                startActivity(intent)
                finish()
            }
        }
    }
}
