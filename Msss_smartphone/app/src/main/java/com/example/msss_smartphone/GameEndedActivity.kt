package com.example.msss_smartphone

// MainActivity.kt
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.floor


class GameEndedActivity : ComponentActivity() {
    private val TAG = "GameEndedActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_ended)

        val lineChart: LineChart = findViewById(R.id.lineChart)
        //imageView.setImageResource(R.drawable.plot)

        
        val bpmValues = intent.getIntegerArrayListExtra("BpmArray")
        val timestampStart = intent.getLongExtra("InitialTimestamp", 0)
        val timestamps = intent.getLongArrayExtra("TimestampArray")
        val thresholdValues = intent.getIntegerArrayListExtra("turnChangesKey")

        Log.d(TAG, "Timestamp ArrayList: ${timestamps?.joinToString(", ")}")
        Log.d(TAG, "BPM ArrayList: ${bpmValues?.joinToString(", ")}")

        // creazione  x, y
        val xValues = (1..thresholdValues!!.last()).toList()

        val yValues: ArrayList<Int> = ArrayList()
        var k = 1
        var i = 1

        Log.d(TAG, "Timestamp SIZE: ${timestamps?.size}")

        yValues.add(bpmValues!!.get(0))

        while (i < xValues.size && k < timestamps!!.size) {
            val diff = floor(((timestamps.get(k) - timestamps.get(k-1)) / 1000).toDouble()).toInt()
            Log.d(TAG, "diff: ${diff}")
            if (diff > 1){
                // mettere valori mancanti con lastValue
                val lastValue: Int = bpmValues!!.get(k-1)
                for (j in 1 ..diff){
                    yValues.add(lastValue)
                }
                i += diff
                Log.d(TAG, "lastvlue: ${lastValue}")
            }
            else{
                yValues.add(bpmValues!!.get(k))
                Log.d(TAG, "bpm value: ${bpmValues!!.get(k)}")
                i += 1
            }
            k += 1
            Log.d(TAG, "k: ${k}")
            Log.d(TAG, "i: ${i}")
        }

        for(i in yValues.size..thresholdValues.last()){
            yValues.add(bpmValues!!.last())
        }

        val hrvCalculator = HRVCalculator()
        val rrIntervals = hrvCalculator.calculateRRIntervals(yValues!!.toList())
        val hrvSdnn = hrvCalculator.calculateSDNN(rrIntervals)
        val hrvRmssd = hrvCalculator.calculateRMSSD(rrIntervals)
        val initialAvg = hrvCalculator.calculatePercentageAverage(yValues, 1)
        val midAvg = hrvCalculator.calculatePercentageAverage(yValues, 2)
        val finalAvg = hrvCalculator.calculatePercentageAverage(yValues, 3)

        val myturnAvg = hrvCalculator.calculateTurnAverages(yValues, thresholdValues)
        Log.d(TAG,myturnAvg.toString())

        val mybpm = findViewById<TextView>(R.id.myturnAverageTextView)
        mybpm.text = "Player turn avg BPM: ${myturnAvg!!.first}"

        val advbpm = findViewById<TextView>(R.id.advAverageTextView)
        advbpm.text = "Opponent turn avg BPM: ${myturnAvg!!.second}"

        val hrvSdnnTextView = findViewById<TextView>(R.id.hrvSdnnTextView)
        hrvSdnnTextView.text = "HRV (SDNN): $hrvSdnn"

        val hrvRmssdTextView = findViewById<TextView>(R.id.hrvRmssdTextView)
        hrvRmssdTextView.text = "HRV (RMSSD): $hrvRmssd"

        val initialAverageTextView = findViewById<TextView>(R.id.initialAverageTextView)
        initialAverageTextView.text = "Opening avg BPM: $initialAvg"

        val midAverageTextView = findViewById<TextView>(R.id.midAverageTextView)
        midAverageTextView.text = "MidGame avg BPM: $midAvg"

        val finalAverageTextView = findViewById<TextView>(R.id.finalAverageTextView)
        finalAverageTextView.text = "EndGame avg BPM: $finalAvg"

        val tipsTextView = findViewById<TextView>(R.id.tips)
        if(finalAvg < midAvg || finalAvg < initialAvg)
            tipsTextView.text = getString(R.string.first_tip)
        else if(midAvg < initialAvg)
            tipsTextView.text = getString(R.string.second_tip)
        else
            tipsTextView.text = getString(R.string.third_tip)

        val avgBpmConclusion = findViewById<TextView>(R.id.averageTextViewConclusion)
        if(myturnAvg.first > myturnAvg.second + 10 || myturnAvg.first < myturnAvg.second - 10)
            avgBpmConclusion.text = getString(R.string.turnbad_tip)
        else
            avgBpmConclusion.text = getString(R.string.turnok_tip)

        val indexConclusion = findViewById<TextView>(R.id.indexTextViewConclusion)
        if(hrvSdnn < 25)
            indexConclusion.text = getString(R.string.stress)
        else if(hrvSdnn < 50)
            indexConclusion.text = getString(R.string.moderate_stress)
        else
            indexConclusion.text = getString(R.string.no_stress)

        val buttonHome: Button = findViewById(R.id.button_home)
        buttonHome.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }

        Log.d(TAG,"thresholdValues:"+ thresholdValues.toString())
        Log.d(TAG,"yvalues:"+ yValues.toString())
        Log.d(TAG,"yvaluessize:"+ yValues.size)
        Log.d(TAG,"xvalues:"+ xValues.toString())
        Log.d(TAG,"xvaluessize:"+ xValues.size)

        // Calcola i colori per la linea del grafico in base ai valori di threshold
        val lineColors = mutableListOf<Int>()
        var currentColor = Color.GREEN // Inizia con il colore verde
        var currentIndex = 0

        for (j in xValues.indices) {
            if (currentIndex < thresholdValues!!.size && xValues[j] >= thresholdValues!![currentIndex]) {
                currentColor = if (currentColor == Color.GREEN) Color.RED else Color.GREEN
                currentIndex++
            }
            lineColors.add(currentColor)
        }

        // Creazione delle viste del grafico
        val entries = mutableListOf<Entry>()
        for (j in xValues.indices) {
            entries.add(Entry(xValues[j].toFloat(), yValues[j]?.toFloat()!!))
        }
        val dataSet = LineDataSet(entries, "Bpm time series")

        dataSet.colors = lineColors //lineColors.toIntArray()
        val lineData = LineData(dataSet)

        // Configurazione del grafico
        lineChart.data = lineData

        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }
}

