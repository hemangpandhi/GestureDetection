package com.example.gesturecontrol.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gesturecontrol.R
import com.example.gesturecontrol.logic.GestureMapper

class GestureControlFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var gestureText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gesture_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gestureText = view.findViewById(R.id.lastGestureText)
        
        viewModel.gestureFeedback.observe(viewLifecycleOwner) { feedback ->
            val nightModeText = if (feedback.isNightMode) " [NIGHT MODE]" else ""
            val sleepText = if (feedback.isSleeping) " [SLEEPING]" else " [ACTIVE]"
            val demoText = if (feedback.isDemoMode) " [DEMO MODE]" else ""
            val moodText = "Mood: ${feedback.mood}"
            
            val action = GestureMapper(requireContext()).getAction(feedback.gestureName)
            
            gestureText.text = "State: $sleepText$nightModeText$demoText\n" +
                    "Gesture: ${feedback.gestureName}\n" +
                    "Action: $action\n" +
                    "Score: ${String.format("%.2f", feedback.score)}\n" +
                    "Feedback: ${feedback.feedbackMessage}\n" +
                    "$moodText"
        }
    }
}
