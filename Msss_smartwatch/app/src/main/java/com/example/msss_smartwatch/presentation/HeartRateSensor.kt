package com.example.msss_smartwatch.presentation

import android.hardware.Sensor
import android.app.Activity
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import android.util.Log


class HeartRateSensor(context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private var running = false
    private val heartRates = mutableListOf<Pair<Long, Int>>()

    private val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)


    fun startSensor() {
        Log.d("TAG", deviceSensors.toString())
        running = true
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, 1000000)
        }
    }


    fun stopSensor() {
        running = false
        sensorManager.unregisterListener(this)
    }

    fun getData(): List<Pair<Long, Int>> {
        synchronized(heartRates) {
            return ArrayList(heartRates)
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_HEART_RATE) {
                val heartRate = it.values[0].toInt()
                synchronized(heartRates) {
                    //heartRates.add(heartRate)
                    val valueToPush = Pair(System.currentTimeMillis(), heartRate)
                    heartRates.add(valueToPush)
                }
            }
        }
    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

}

/*
class HeartRateSensorSimulate : Thread() {

    private var running = false
    private val heartRates = mutableListOf<Int>()

    override fun run() {
        running = true
        while (running) {
            val heartRate = simulateHeartRate()

            synchronized(heartRates) {
                heartRates.add(heartRate)
            }

            sleep(1000)
        }
    }

    fun stopSensor() {
        running = false
    }

    fun getData(): List<Int> {
        synchronized(heartRates) {
            return ArrayList(heartRates)
        }
    }

    private fun simulateHeartRate(): Int {
        return (60..100).random()
    }
}*/

