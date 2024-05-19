package com.example.msss_smartwatch.presentation

import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

class BluetoothServer(private val context: Context, private val bluetoothServerSocket: BluetoothServerSocket): Thread() {
    private val TAG = "BluetoothServerThread"

    override fun run(){
        while (true) {
            val socket = bluetoothServerSocket.accept()
            Log.d(TAG, "Connection accepted")

            val handleSocket = HandleSocket(context, socket)
            handleSocket.start()
        }
    }
}

class HandleSocket(private val context: Context, private val socket: BluetoothSocket?) : Thread() {

    private val TAG = "HandleSocketThread"
    private val heartRateSensor = HeartRateSensor(context)
    //private val heartRateSensorSimulate = HeartRateSensorSimulate()



    override fun run() {
        while(true){
            if (socket == null) {
                Log.e(TAG, "Socket is null")
                return
            }

            val inputStream = socket.inputStream
            val buffer = ByteArray(1024)

            try{
                val bytesRead: Int = inputStream.read(buffer)

                if (bytesRead != -1) {
                    when (val receivedMessage = String(buffer, 0, bytesRead)) {
                        "START_APP" -> {
                            Log.d("Bluetooth", "Application started by client")

                            //heartRateSensorSimulate.start()
                            heartRateSensor.startSensor()

                            val openMessage = Message.obtain()
                            openMessage.what = MainActivity.MSG_OPEN_APP
                            (context as MainActivity).handler.sendMessage(openMessage)

                        }
                        "END_APP" -> {
                            Log.d("Bluetooth", "Application closed by client")

                            heartRateSensor.stopSensor()
                            //heartRateSensorSimulate.stopSensor()

                            val data = heartRateSensor.getData()
                            //val data = heartRateSensorSimulate.getData()

                            /*val data: List<Pair<Long, Int>> = listOf(
                                Pair(1234567890L, 10),
                                Pair(9876543210L, 20),
                                Pair(1231231231L, 30),
                                Pair(4564564564L, 40),
                                Pair(7897897897L, 50)
                            )*/
                            sendDataToClient(data)

                            val closeMessage = Message.obtain()
                            closeMessage.what = MainActivity.MSG_CLOSE_APP
                            (context as MainActivity).handler.sendMessage(closeMessage)

                            return
                        }
                        else -> {
                            Log.d("Bluetooth", "Received message from client: $receivedMessage")
                            val msg = Message.obtain()
                            val bundle = Bundle()

                            msg.what = MainActivity.MSG_RECV_APP

                            bundle.putString("receivedMessage", receivedMessage)
                            msg.data = bundle

                            (context as MainActivity).handler.sendMessage(msg)
                        }
                    }
                }
            }
            catch(e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "IOException: ${e.message}")
                Log.d("Bluetooth", "Socket closed by client")
                return
            }
        }
    }

    private fun sendDataToClient(data: List<Pair<Long, Int>>) {
        if (socket == null) {
            println("Errore: socket non inizializzato o chiuso.")
            return
        }

        try {
            DataOutputStream(socket.outputStream).use { outputStream ->
                outputStream.writeInt(data.size)

                for (element in data) {
                    outputStream.writeLong(element.first)
                    outputStream.writeInt(element.second)
                }

                outputStream.flush()
                Log.d(TAG, "Dati inviati al client: ${data.joinToString(", ") { "(${it.first}, ${it.second})" }}")

            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante l'invio: ${e.message}")
        }
    }


}