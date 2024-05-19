package com.example.msss_smartphone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.ComponentActivity

class HistoryManager : ComponentActivity() {
    private val TAG = "HistoryManager"
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_page)

        val spinnerHistory: Spinner = findViewById(R.id.spinner_history)
        val buttonSelect: Button = findViewById(R.id.button_select)
        val fileManager = FileManager(this)
        var count = fileManager.getCounter()
        val dataArrayList = ArrayList<Data>()

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ArrayList<String>()
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerHistory.adapter = adapter

        if (count == 0) {
            // Se non ci sono partite, aggiungi un elemento "No match found"
            adapter.add("No match found")
            buttonSelect.isEnabled = false
        } else {
            // Altrimenti, aggiungi le partite esistenti
            while (count > 0) {
                val data = fileManager.readDataFromJson(count)
                data?.let {
                    dataArrayList.add(it)
                    val text = "Match $count"
                    adapter.add(text)
                }
                count--
            }
        }

        spinnerHistory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = adapter.getItem(position)
                selectedItem?.let {
                    Log.d(TAG, "Elemento selezionato: $it")
                    if (it != "No match found") {
                        buttonSelect.isEnabled = true
                    } else {
                        buttonSelect.isEnabled = false
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                buttonSelect.isEnabled = false
            }
        }

        buttonSelect.setOnClickListener {
            val selectedItemPosition = spinnerHistory.selectedItemPosition
            val selectedItem = dataArrayList[selectedItemPosition]

            val intent = Intent(this, GameEndedActivity::class.java)

            intent.putIntegerArrayListExtra("BpmArray", selectedItem.bpmArray)
            intent.putExtra("TimestampArray", selectedItem.timestampArray.toLongArray())
            intent.putExtra("InitialTimestamp", selectedItem.timestampStart)
            intent.putIntegerArrayListExtra("turnChangesKey", selectedItem.turnChanges)
            startActivity(intent)
            finish()
        }

        val buttonBack: Button = findViewById(R.id.button_back)
        buttonBack.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
