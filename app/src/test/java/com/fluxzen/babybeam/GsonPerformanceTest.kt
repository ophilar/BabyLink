package com.fluxzen.babybeam

import com.google.gson.Gson
import kotlin.system.measureTimeMillis

fun main() {
    data class DummyMessage(val type: String, val payload: String)
    val iterations = 10000
    val dummyData = DummyMessage("test", "data")

    val creationTime = measureTimeMillis {
        for (i in 0 until iterations) {
            val gson = Gson()
            val json = gson.toJson(dummyData)
            gson.fromJson(json, DummyMessage::class.java)
        }
    }

    val reuseTime = measureTimeMillis {
        val gson = Gson()
        for (i in 0 until iterations) {
            val json = gson.toJson(dummyData)
            gson.fromJson(json, DummyMessage::class.java)
        }
    }

    println("=========================================")
    println("Performance test results ($iterations iterations):")
    println("Baseline (New Gson instance every time): $creationTime ms")
    println("Optimization (Reusing Gson instance): $reuseTime ms")
    println("Improvement: ${creationTime - reuseTime} ms (${"%.2f".format((creationTime - reuseTime).toDouble() / creationTime * 100)}%)")
    println("=========================================")
}
