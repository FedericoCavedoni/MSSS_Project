package com.example.msss_smartphone

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore

const val REQUEST_BLUETOOTH_PERMISSION = 1
const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2

@SuppressLint("StaticFieldLeak")
@RequiresApi(Build.VERSION_CODES.S)
var bluetoothManager = BluetoothManager()
val bluetoothSemaphore = Semaphore(1)

@RequiresApi(Build.VERSION_CODES.S)
class BluetoothManager() : Thread() {
    private val TAG = "BluetoothManagerThread"
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var selectedDeviceName: String? = null
    private var deviceSelected: Boolean = false

    private var context: Context? = null

    private var deviceList: MutableList<BluetoothDevice>? = null
    private val deviceListLatch = CountDownLatch(1)

    private val messageQueue = ArrayBlockingQueue<String>(1)

    private var dataReceived: List<Pair<Long, Int>>? = null
    private val dataReceivedLatch = CountDownLatch(1)

    private var fileManager: FileManager? = null
    private var socket: BluetoothSocket? = null

    private var status: Boolean? = null
    private val statusLatch = CountDownLatch(1)

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    interface BluetoothConnectionCallback {
        fun onConnectionSuccess()
        fun onConnectionFailure(errorMessage: String)
    }

    private var connectionCallback: BluetoothConnectionCallback? = null

    fun setConnectionCallback(callback: BluetoothConnectionCallback) {
        connectionCallback = callback
    }

    private fun setStatus(st: Boolean){
        status = st
        statusLatch.countDown()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun run() {
        loop()

        if (socket == null) {
            Log.e(TAG, "Socket is null")
            return
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initializeDeviceList() {
        val devices = mutableListOf<BluetoothDevice>()

        if (context == null) {
            Log.e(TAG, "Context is null")
            deviceList = devices
            deviceListLatch.countDown()
            return
        }

        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Bluetooth permission denied")
        }

        if(bluetoothAdapter == null){
            deviceList = devices
            deviceListLatch.countDown()
            return
        }

        val bondedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        devices.addAll(bondedDevices)

        deviceList = devices
        deviceListLatch.countDown()
        deviceSelected = true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun connectToBluetoothServer() {
        if (context == null) {
            Log.e(TAG, "Context is null")
            return
        }

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Bluetooth permission denied")
        }

        Log.d(TAG, "Connecting to Bluetooth server...")

        val device = getDeviceByName()

        if (device == null) {
            Log.e(TAG, "Device not found")
            return
        }
        Log.d(TAG, "Device found")

        try {
            if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bluetooth permission denied")
            }

            socket = device.createRfcommSocketToServiceRecord(uuid)
            Log.d(TAG, "Socket created")


            socket?.connect()
            Log.d(TAG, "Socket connected")
            setStatus(true)


            connectionCallback?.onConnectionSuccess()

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Error connecting or sending message: ${e.message}")
            setStatus(false)
            connectionCallback?.onConnectionFailure("Error connecting to Bluetooth server: ${e.message}")
            return
        }

        context?.let { fileManager = FileManager(it) }
    }

    private fun loop() {
        while (true) {
            try {
                Log.d(TAG, "Waiting for the message")
                val message = messageQueue.take()

                val outputStream = socket?.outputStream
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
                Log.d(TAG, "Message sent successfully: $message")

                if (message == "END_APP") {
                    Log.d(TAG, "Connection closed, waiting for the data")

                    val receivedData = getDataFromServer()
                    dataReceived = receivedData

                    dataReceivedLatch.countDown()

                    if (receivedData != null) {
                        Log.d(TAG, "Dati ricevuti dal server: ${receivedData.joinToString(", ") { "(${it.first}, ${it.second})" }}")

                    } else {
                        Log.d(TAG, "Failed to receive data from server.")
                    }
                    break
                }

            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "IOException: ${e.message}")
                Log.d("Bluetooth", "Socket closed by Server")
                return
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getDeviceByName(): BluetoothDevice? {
        if(!deviceSelected){
            initializeDeviceList()
        }

        if (context == null) {
            Log.e(TAG, "Context is null")
            return null
        }

        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Bluetooth permission denied")
        }


        try {
            deviceListLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Log.e(TAG, "Interrupted while waiting for deviceList initialization: ${e.message}")
            return null
        }

        return deviceList?.find { it.name == selectedDeviceName }
    }

    private fun getDataFromServer(): List<Pair<Long, Int>>? {
        if (socket == null) {
            Log.e(TAG, "Errore: socket non inizializzato o chiuso.")
            return null
        }

        return try {
            DataInputStream(socket!!.inputStream).use { dataInput ->
                val length = dataInput.readInt()
                val data = mutableListOf<Pair<Long, Int>>()

                for (i in 0 until length) {
                    val first = dataInput.readLong()
                    val second = dataInput.readInt()
                    data.add(Pair(first, second))
                }

                Log.d(TAG, "Dati ricevuti dal server")
                data
            }
        } catch (e: IOException) {
            Log.e(TAG, "Errore: ${e.message}")
            null
        }
    }


    fun sendMessage(message: String) {
        messageQueue.put(message)
    }

    fun setContext(newContext: Context) {
        context = newContext
    }

    fun setSelectedDeviceName(deviceName: String) {
        selectedDeviceName = deviceName
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun getDeviceNames(): List<String> {
        if(!deviceSelected){
            initializeDeviceList()
        }
        try {
            deviceListLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Log.e(TAG, "Interrupted while waiting for deviceList initialization: ${e.message}")
        }

        if (context == null) {
            Log.e(TAG, "Context is null")
            return emptyList()
        }

        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Bluetooth permission denied")
        }

        return deviceList?.map { it.name } ?: emptyList()
    }

    fun getDataReceived(): List<Pair<Long, Int>>? {
        dataReceivedLatch.await()
        //return dataReceived?.toList()?.let { ArrayList(it) }
        return dataReceived?.toList()?.let { it }
    }
}
