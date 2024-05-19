package com.example.msss_smartphone
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.msss_smartphone.databinding.HomePageBinding


class HomePageActivity : ComponentActivity() {
    private val tag = "HomePageActivity"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADMIN), REQUEST_BLUETOOTH_PERMISSION)
        }
        val binding:HomePageBinding = DataBindingUtil.setContentView(this, R.layout.home_page)

        binding.playButton.setOnClickListener {
            if(!bluetoothManager.isBluetoothEnabled()){
                runOnUiThread {
                    Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                val intent = Intent(this, BluetoothActivity::class.java)
                startActivity(intent)
            }
        }

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryManager::class.java)
            startActivity(intent)
        }

    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(tag, "Permission bluetooth given")
                    val intent = Intent(this, HomePageActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.d(tag, "Permission bluetooth denied")
                    return
                }
            }

            REQUEST_BLUETOOTH_CONNECT_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(tag, "Permission bluetooth connect given")
                    val intent = Intent(this, HomePageActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.d(tag, "Permission bluetooth connect denied")
                    return
                }
            }
        }
    }
}

