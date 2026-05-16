package com.fluxzen.babybeam

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BabyMonitorStateRepository @Inject constructor() {
    private val _temperature = MutableStateFlow<Float?>(null)
    val temperature = _temperature.asStateFlow()

    private val _humidity = MutableStateFlow<Float?>(null)
    val humidity = _humidity.asStateFlow()

    private val _noiseLevel = MutableStateFlow(0f)
    val noiseLevel = _noiseLevel.asStateFlow()

    private val _isNightLightOn = MutableStateFlow(false)
    val isNightLightOn = _isNightLightOn.asStateFlow()

    fun updateTemperature(temp: Float?) { _temperature.value = temp }
    fun updateHumidity(hum: Float?) { _humidity.value = hum }
    fun updateNoiseLevel(level: Float) { _noiseLevel.value = level }
    fun toggleNightLight() { _isNightLightOn.value = !_isNightLightOn.value }
}
