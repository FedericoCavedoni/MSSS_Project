package com.example.msss_smartwatch.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.example.msss_smartwatch.R
import java.io.IOException
import java.util.UUID


class MainActivity : FragmentActivity() {
    private val REQUEST_BLUETOOTH_PERMISSION = 1
    private val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val TAG = "MainThread"

    private var ambientController: AmbientModeSupport.AmbientController? = null
    companion object {
        const val MSG_OPEN_APP = 1
        const val MSG_CLOSE_APP = 2
        const val MSG_RECV_APP = 3
    }



    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_OPEN_APP -> openApplication()
                MSG_CLOSE_APP -> closeApplication()
                MSG_RECV_APP -> msg.data.getString("receivedMessage")?.let { onMessageReceived(it) }
                else -> super.handleMessage(msg)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!isBluetoothEnabled()){
            setErrorPage("Bluetooth not enabled")
            return
        }
        startBluetoothServer()

        setWelcomePage()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN), REQUEST_BLUETOOTH_PERMISSION)
        }
        Log.d(TAG, "Bluetooth permission OK")

        ambientController = AmbientModeSupport.attach(this)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startBluetoothServer() {
        Log.d(TAG, "Starting bluetooth server...")


        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT_PERMISSION)
            }
            Log.d(TAG, "Bluetooth connect permission OK")

            val serverName = "sdk_gwear_x86_64"
            val bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(serverName, uuid)
            Log.d(TAG, "Starting bluetooth server with name $serverName")

            val bluetoothServer = BluetoothServer(this@MainActivity, bluetoothServerSocket)
            bluetoothServer.start()

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "IOException: ${e.message}")
        }
    }

    private fun setWelcomePage(){
        setContentView(R.layout.activity_welcome)
    }

    private fun setErrorPage(msg: String){
        setContentView(R.layout.activity_error)

        val errorTextView: TextView = findViewById(R.id.errorTextView)
        errorTextView.text = msg

        val startButton: Button = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun openApplication() {
        setContentView(R.layout.activity_application)

        val heartImage = findViewById<ImageView>(R.id.heart_image)
        heartImage.setImageResource(R.drawable.heart)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            width = 200
            height = 200
            gravity = Gravity.CENTER
        }
        heartImage.layoutParams = layoutParams

        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.heart_pulse)
        heartImage.startAnimation(pulseAnimation)
    }



    fun closeApplication() {
        removeHeartImage()
        setContentView(R.layout.activity_end)

        Handler(Looper.getMainLooper()).postDelayed({
            setWelcomePage()
        }, 10000)
    }

    private fun removeHeartImage() {
        val containerLayout = findViewById<LinearLayout>(R.id.container_layout)
        containerLayout.removeAllViews()
    }

    private fun onMessageReceived(msg: String) {
        removeHeartImage()

        setContentView(R.layout.activity_application_msg)

        val messageTextView = findViewById<TextView>(R.id.message_textview)
        messageTextView.text = msg

        Handler(Looper.getMainLooper()).postDelayed({
            openApplication()
        }, 5000)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Bluetooth permission granted")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.d(TAG, "Bluetooth permission denied")
                    return
                }
            }

            REQUEST_BLUETOOTH_CONNECT_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Bluetooth connect permission granted")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.d(TAG, "Bluetooth connect permission denied")
                    return
                }
            }
        }
    }
}



