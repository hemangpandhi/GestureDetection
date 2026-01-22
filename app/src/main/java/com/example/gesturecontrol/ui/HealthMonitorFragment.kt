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

class HealthMonitorFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var bpmText: TextView
    private lateinit var stressText: TextView
    private lateinit var bpmStatus: TextView
    private lateinit var calmModeStatus: TextView
    private lateinit var signalGraphView: LineGraphView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_health_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bpmText = view.findViewById(R.id.bpmText)
        stressText = view.findViewById(R.id.stressText)
        bpmStatus = view.findViewById(R.id.bpmStatus)
        calmModeStatus = view.findViewById(R.id.calmModeStatus)
        signalGraphView = view.findViewById(R.id.signalGraphView)
        
        viewModel.healthState.observe(viewLifecycleOwner) { state ->
            bpmText.text = "${state.heartRate} BPM"
            stressText.text = state.stressLevel
            
            if (state.stressLevel == "High") {
                stressText.setTextColor(Color.RED)
                bpmStatus.text = "Status: ELEVATED STRESS"
                bpmStatus.setTextColor(Color.RED)
            } else {
                stressText.setTextColor(Color.WHITE)
                bpmStatus.text = "Status: Normal"
                bpmStatus.setTextColor(Color.parseColor("#00FF99"))
            }
            
            if (state.isCalmModeActive) {
                calmModeStatus.text = "Calm Mode: ACTIVE"
                calmModeStatus.setTextColor(Color.CYAN)
            } else {
                calmModeStatus.text = "Calm Mode: OFF"
                calmModeStatus.setTextColor(Color.parseColor("#88FFFFFF"))
            }
            
            // Update Graph
            signalGraphView.addPoint(state.rawSignal)
        }
    }
}
