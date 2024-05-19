package com.example.msss_smartphone

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.msss_smartphone.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {
    private val tag = "MainThread"

    private lateinit var countDownTimerTop: CountDownTimer
    private lateinit var countDownTimerBottom: CountDownTimer

    private var isTopTimerRunning = false
    private var isBottomTimerRunning = false

    private var timeTopInMillis: Long = 60000
    private var timeBottomInMillis: Long = 60000

    private var firstClick: Boolean = true

    private var timestampStart: Long = 0L

    private var finish = false
    private var increment : Long = 0

    private var turnChanges = listOf<Int>()
    private var initialSeconds: Int = 60
    
    private val timeValues = mapOf(
        "1 min" to 60_000L,
        "1 min | 1 sec" to 61_000L,
        "2 min | 1 sec" to 121_000L,
        "3 min" to 180_000L,
        "3 min | 2 sec" to 182_000L,
        "5 min" to 300_000L,
        "5 min | 5 sec" to 305_000L,
        "10 min" to 600_000L,
        "15 min | 10 sec" to 910_000L,
        "20 min" to 1_200_000L,
        "30 min" to 1_800_000L
    )

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val extras = intent.extras
        var selectedDevice: String? = null

        if (extras != null) {
            selectedDevice = extras.getString("SELECTED_DEVICE")
            Log.d(tag, "SelectedDevice: $selectedDevice")
        }

        if(selectedDevice == null){
            Log.d(tag, "Error with the device selection")
            return
        }

        bluetoothSemaphore.acquire()

        bluetoothManager.setContext(this)
        bluetoothManager.start()

        bluetoothSemaphore.release()

        val buttonTop: Button = binding.topTimer
        val buttonBottom: Button = binding.bottomTimer
        var timeStarted = false
        var last = ""

        binding.stopBtn.setOnClickListener {
            finish = true
            stopTopTimer()
            stopBottomTimer()
        }

        binding.playBtn.setOnClickListener {
            finish = false
            if(last == "bottom")
                startBottomTimer(buttonBottom)
            else
                startTopTimer(buttonTop)
        }

        binding.settingsBtn.setOnClickListener {
            showListDialog()
        }

        buttonTop.setOnClickListener {
            binding.settingsBtn.isEnabled = false

            if(firstClick){
                bluetoothSemaphore.acquire()
                bluetoothManager.sendMessage("START_APP")
                bluetoothSemaphore.release()
                timestampStart = System.currentTimeMillis()
                firstClick = false
            }

            if (isTopTimerRunning){
                buttonBottom.setBackgroundColor(ContextCompat.getColor(this, R.color.inactivebuttoncolor))
                buttonTop.setBackgroundColor(ContextCompat.getColor(this, R.color.buttoncolor))
                timeTopInMillis += increment

                val secondsLeft = timeTopInMillis / 1000
                val formattedTime = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)
                buttonTop.text = formattedTime

                turnChanges += (initialSeconds*2 - timeTopInMillis.toInt()/1000 - timeBottomInMillis.toInt()/1000)

                stopTopTimer()
                last = "bottom"
            }

            if(!timeStarted){
                buttonTop.setBackgroundColor(ContextCompat.getColor(this, R.color.inactivebuttoncolor))
                buttonBottom.setBackgroundColor(ContextCompat.getColor(this, R.color.buttoncolor))
                startTopTimer(buttonTop)
                timeStarted = true
            }

        }

        buttonBottom.setOnClickListener {
            binding.settingsBtn.isEnabled = false

            if(firstClick){
                bluetoothSemaphore.acquire()
                bluetoothManager.sendMessage("START_APP")
                bluetoothSemaphore.release()
                timestampStart = System.currentTimeMillis()
                firstClick = false
            }

            if (isBottomTimerRunning) {
                buttonTop.setBackgroundColor(ContextCompat.getColor(this, R.color.inactivebuttoncolor))
                buttonBottom.setBackgroundColor(ContextCompat.getColor(this, R.color.buttoncolor))
                timeBottomInMillis += increment

                val secondsLeft = timeBottomInMillis / 1000
                val formattedTime = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)
                buttonBottom.text = formattedTime

                turnChanges += (initialSeconds*2 - timeTopInMillis.toInt()/1000 - timeBottomInMillis.toInt()/1000)

                stopBottomTimer()
                last = "top"
            }

            if(!timeStarted){
                buttonBottom.setBackgroundColor(ContextCompat.getColor(this, R.color.inactivebuttoncolor))
                buttonTop.setBackgroundColor(ContextCompat.getColor(this, R.color.buttoncolor))
                startBottomTimer(buttonBottom)
                timeStarted = true
            }
        }

        binding.endBtn.setOnClickListener {
            bluetoothSemaphore.acquire()

            bluetoothManager.sendMessage("END_APP")
            val dataFromSmartwatch = bluetoothManager.getDataReceived()

            val timestampArrayList: ArrayList<Long> = ArrayList(dataFromSmartwatch!!.map { it.first })
            val bpmArrayList: ArrayList<Int> = ArrayList(dataFromSmartwatch.map { it.second })

            Log.d(tag, "Timestamp ArrayList: ${timestampArrayList.joinToString(", ")}")
            Log.d(tag, "BPM ArrayList: ${bpmArrayList.joinToString(", ")}")

            bluetoothSemaphore.release()

            turnChanges += (initialSeconds*2 - timeTopInMillis.toInt()/1000 - timeBottomInMillis.toInt()/1000)
            val fileManager = FileManager(this)
            fileManager.saveDataToJson(bpmArrayList, timestampArrayList, ArrayList(turnChanges), timestampStart)

            val intent = Intent(this, GameEndedActivity::class.java)
            intent.putExtra("TimestampArray", timestampArrayList.toLongArray())
            intent.putExtra("InitialTimestamp", timestampStart)
            intent.putIntegerArrayListExtra("BpmArray", bpmArrayList)
            intent.putIntegerArrayListExtra("turnChangesKey", ArrayList(turnChanges))
            startActivity(intent)
            finish()
        }

    }

    private fun showListDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_menu)

        val listView = dialog.findViewById<ListView>(R.id.listView)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listOf("1 min", "1 min | 1 sec", "2 min | 1 sec", "3 min", "3 min | 2 sec", "5 min", "5 min | 5 sec", "10 min", "15 min | 10 sec", "20 min", "30 min")
        )

        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = listView.adapter.getItem(position) as String
            var selectedTimeInMillis = timeValues[selectedItem] ?: 60_000L

            increment = selectedTimeInMillis % 60_000L
            selectedTimeInMillis -= increment

            timeTopInMillis = selectedTimeInMillis
            timeBottomInMillis = selectedTimeInMillis
            initialSeconds = (selectedTimeInMillis / 1000).toInt()      // da testare

            val buttonTop: Button = findViewById(R.id.topTimer)
            val buttonBottom: Button = findViewById(R.id.bottomTimer)

            val secondsLeft = timeTopInMillis / 1000
            val formattedTime = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)
            buttonBottom.text = formattedTime
            buttonTop.text = formattedTime

            Toast.makeText(this, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun startTopTimer(button: Button) {
        countDownTimerTop = object : CountDownTimer(timeTopInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                val formattedTime = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)
                button.text = formattedTime
                timeTopInMillis = millisUntilFinished
            }

            override fun onFinish() {
                isTopTimerRunning = false
                button.text = "00:00"
            }
        }
        countDownTimerTop.start()
        isTopTimerRunning = true
    }

    private fun startBottomTimer(button: Button) {
        countDownTimerBottom = object : CountDownTimer(timeBottomInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                val formattedTime = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60)
                button.text = formattedTime
                timeBottomInMillis = millisUntilFinished
            }

            override fun onFinish() {
                isBottomTimerRunning = false
                button.text = "00:00"
            }
        }
        countDownTimerBottom.start()
        isBottomTimerRunning = true
    }

    private fun stopTopTimer() {
        if (::countDownTimerTop.isInitialized)
            countDownTimerTop.cancel()

        isTopTimerRunning = false
        val buttonBottom: Button = findViewById(R.id.bottomTimer)
        if(!finish)
            startBottomTimer(buttonBottom)
    }

    private fun stopBottomTimer() {
        if (::countDownTimerBottom.isInitialized)
            countDownTimerBottom.cancel()

        isBottomTimerRunning = false
        val buttonTop: Button = findViewById(R.id.topTimer)
        if(!finish)
            startTopTimer(buttonTop)
    }
    
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(tag, "Permission bluetooth given")
                } else {
                    Log.d(tag, "Permission bluetooth denied")
                    return
                }
            }

            REQUEST_BLUETOOTH_CONNECT_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(tag, "Permission bluetooth connect given")
                } else {
                    Log.d(tag, "Permission bluetooth connect denied")
                    return
                }
            }
        }
    }
}