package com.example.msss_smartphone

import android.util.Log
import kotlin.math.sqrt
class HRVCalculator {

    fun calculateSDNN(rrIntervals: List<Int>): Double {
        val mean = rrIntervals.average()
        val squaredDiffs = rrIntervals.map { (it - mean) * (it - mean) }
        return sqrt(squaredDiffs.average())
    }

    fun calculateRMSSD(rrIntervals: List<Int>): Double {
        val squaredDiffs = rrIntervals.drop(1).zip(rrIntervals.dropLast(1)) { a, b -> (a - b) * (a - b) }
        return sqrt(squaredDiffs.average())
    }

    fun calculateRRIntervals(bpmList: List<Int>): List<Int> {
        // Converte la lista di bpm in una lista di intervalli RR in millisecondi
        return bpmList.map { 60_000 / it }
    }

    fun calculatePercentageAverage(values: List<Int>, position: Int): Double {
        if (values.isEmpty()) return 0.0

        val percentage = when (position) {
            1 -> 0.2
            2 -> 0.4
            3 -> 0.4
            else -> 0.0
        }

        val count = (values.size * percentage).toInt()

        val sublist = when (position) {
            1 -> values.subList(0, count.coerceAtMost(values.size))
            2 -> values.subList((values.size * 0.2).toInt(), (values.size - count).coerceAtLeast(0))
            3 -> values.subList(values.size - count.coerceAtMost(values.size), values.size)
            else -> emptyList()
        }

        return sublist.average()
    }

    fun calculateTurnAverages(bpmValues: List<Int>, turnChanges: List<Int>): Pair<Double, Double>? {
        if (bpmValues.isEmpty() || turnChanges.isEmpty()) return null

        var yourTurnSum = 0
        var opponentTurnSum = 0

        var yourTurnCount = 0
        var opponentTurnCount = 0

        var currentTurnIndex = 0

        for (i in bpmValues.indices) {
            if (i >= turnChanges[currentTurnIndex])
                currentTurnIndex++

            if (currentTurnIndex % 2 == 0) {
                yourTurnSum += bpmValues[i]
                yourTurnCount++
            } else {
                // Il turno è dell'avversario
                opponentTurnSum += bpmValues[i]
                opponentTurnCount++
            }
        }

        val yourTurnAverage = yourTurnSum.toDouble() / yourTurnCount
        val opponentTurnAverage = opponentTurnSum.toDouble() / opponentTurnCount

        return Pair(yourTurnAverage, opponentTurnAverage)
    }

}
