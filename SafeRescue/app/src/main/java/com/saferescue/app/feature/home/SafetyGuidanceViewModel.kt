package com.saferescue.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferescue.app.core.location.LocationPoint
import com.saferescue.app.core.safety.RemoteSafetyDataRepository
import com.saferescue.app.core.safety.SafetyDataRepository
import com.saferescue.app.core.safety.SafetyGuidanceSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SafetyGuidanceViewModel(
    private val repository: SafetyDataRepository = RemoteSafetyDataRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(SafetyGuidanceSnapshot())
    val state: StateFlow<SafetyGuidanceSnapshot> = _state.asStateFlow()

    fun refresh(location: LocationPoint?) {
        if (location == null) {
            _state.value = SafetyGuidanceSnapshot(error = "A live location fix is required. Start an SOS and grant location permission first.")
            return
        }
        _state.value = SafetyGuidanceSnapshot(loading = true)
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val places = async { repository.nearbySafePlaces(location) }
                    val weather = async { repository.currentWeather(location) }
                    SafetyGuidanceSnapshot(places = places.await(), weather = weather.await(), loadedAtMillis = System.currentTimeMillis())
                }
            }.onSuccess { _state.value = it }.onFailure {
                _state.value = SafetyGuidanceSnapshot(error = it.message ?: "Safety data could not be loaded")
            }
        }
    }
}
