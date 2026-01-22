package com.example.gesturecontrol.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gesturecontrol.R

class DriverSafetyFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    
    private lateinit var statusText: TextView
    private lateinit var gazeText: TextView
    private lateinit var lastEventText: TextView
    private lateinit var alertOverlay: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_driver_safety, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.safetyStatusText)
        gazeText = view.findViewById(R.id.gazeText)
        lastEventText = view.findViewById(R.id.lastEventText)
        alertOverlay = view.findViewById(R.id.alertOverlay)
        
        viewModel.gestureFeedback.observe(viewLifecycleOwner) { feedback ->
            // Update Gaze (Mocked from Mood/State currently)
            // Ideally we'd have a specific field, but we are using Mood for new states
            
            when (feedback.mood) {
                "DROWSY WARNING" -> {
                    statusText.text = "DROWSY"
                    statusText.setTextColor(Color.RED)
                    lastEventText.text = "Eyes Closed > 1s"
                    alertOverlay.visibility = View.VISIBLE
                    alertOverlay.text = "WAKE UP!"
                    alertOverlay.setBackgroundColor(Color.parseColor("#CCFF0000"))
                }
                "DISTRACTED" -> {
                    statusText.text = "DISTRACTED"
                    statusText.setTextColor(Color.YELLOW)
                    gazeText.text = "Away From Road"
                    gazeText.setTextColor(Color.YELLOW)
                    lastEventText.text = "Gaze Deviated > 2s"
                    alertOverlay.visibility = View.GONE
                }
                "Confused ?" -> {
                    lastEventText.text = "Confusion (Brows Up)"
                    alertOverlay.visibility = View.GONE
                    resetNormalState()
                }
                "Discomfort/Wince" -> {
                    lastEventText.text = "Discomfort (Sneer)"
                    alertOverlay.visibility = View.GONE
                    resetNormalState()
                }
                else -> {
                    // Normal
                   resetNormalState()
                   alertOverlay.visibility = View.GONE
                }
            }
        }
    }
    
    private fun resetNormalState() {
        statusText.text = "ACTIVE"
        statusText.setTextColor(Color.GREEN)
        gazeText.text = "Road Center"
        gazeText.setTextColor(Color.WHITE)
    }
}
