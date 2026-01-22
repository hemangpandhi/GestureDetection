package com.example.gesturecontrol.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gesturecontrol.logic.GestureFeedback
import com.example.gesturecontrol.logic.HealthState

class MainViewModel : ViewModel() {
    val gestureFeedback = MutableLiveData<GestureFeedback>()
    val healthState = MutableLiveData<HealthState>()
}
