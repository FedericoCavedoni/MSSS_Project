package com.example.msss_smartphone

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.io.IOException

//I File si trovano a /data/data/com.example.msss_smartphone/files

class FileManager(private val context: Context) {
    private val TAG = "FileManager"

    private val resultFolder = "results"
    private val jsonCounterFileName = "counterLog.json"

    private val gson = Gson()
    private var counter = readCounterFromJson()

    init {
        createResultFolderIfNeeded()
        createCounterFileIfNeeded()
    }

    private fun createResultFolderIfNeeded() {
        val folder = File(context.filesDir, resultFolder)
        if (!folder.exists()) {
            try {
                folder.mkdirs()
            } catch (e: IOException) {
                Log.e(TAG, "Errore durante la creazione della cartella di destinazione: ${e.message}")
            }
        }
    }

    private fun createCounterFileIfNeeded() {
        val counterFile = File(context.filesDir, jsonCounterFileName)
        if (!counterFile.exists()) {
            try {
                counterFile.createNewFile()
                counterFile.writeText("1")
            } catch (e: IOException) {
                Log.e(TAG, "Errore durante la creazione del file counterLog.json: ${e.message}")
            }
        }
    }

    private fun readCounterFromJson(): Int {
        val counterFile = File(context.filesDir, jsonCounterFileName)
        if (!counterFile.exists()) {
            Log.e(TAG, "Il file counterLog.json non esiste.")
            return 0
        }

        val jsonData = counterFile.readText()

        return gson.fromJson(jsonData, Int::class.java)
    }

    private fun updateCounterInJsonFile() {
        val counterFile = File(context.filesDir, jsonCounterFileName)
        if (!counterFile.exists()) {
            Log.e(TAG, "Il file counterLog.json non esiste.")
            return
        }

        val newCounterJson = gson.toJson(counter)
        try {
            counterFile.writeText(newCounterJson)
            Log.d(TAG, "Contatore aggiornato: $counter")
        } catch (e: IOException) {
            Log.e(TAG, "Errore durante l'aggiornamento del file counterLog.json: ${e.message}")
        }
    }

    fun getCounter(): Int{
        return counter-1
    }
    fun saveDataToJson(bpmArray: ArrayList<Int>, timestampArray: ArrayList<Long>, turnChanges: ArrayList<Int>, timeStampStart: Long) {
        val jsonFileName = "data$counter.json"
        counter++

        updateCounterInJsonFile()

        val folder = File(context.filesDir, resultFolder)
        if (!folder.exists()) {
            Log.e(TAG, "La cartella di destinazione non esiste.")
            return
        }

        val data = Data(timeStampStart, bpmArray, timestampArray, turnChanges)
        val jsonData = gson.toJson(data)

        val file = File(folder, jsonFileName)
        try {
            FileWriter(file).use { writer ->
                writer.write(jsonData)
            }
            Log.d(TAG, "File salvato: $jsonFileName")
            Log.d(TAG, "Percorso assoluto: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Errore durante la scrittura del file JSON: ${e.message}")
        }
    }

    fun readDataFromJson(counter: Int): Data? {
        val fileName = "data$counter.json"
        val file = File(context.filesDir, "$resultFolder/$fileName")
        if (!file.exists()) {
            return null
        }

        val jsonData = file.readText()

        Log.d(TAG, "File letto: $fileName")
        Log.d(TAG, "Percorso assoluto: ${file.absolutePath}")
        return gson.fromJson(jsonData, object : TypeToken<Data>() {}.type)
    }

}

data class Data(
    val timestampStart: Long,
    val bpmArray: ArrayList<Int>,
    val timestampArray: ArrayList<Long>,
    val turnChanges: ArrayList<Int>
)


