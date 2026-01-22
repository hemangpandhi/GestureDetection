package com.example.gesturecontrol.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.gesturecontrol.R
import com.example.gesturecontrol.service.GestureService

class CabinSenseFragment : Fragment(R.layout.fragment_cabin_sense) {

    private lateinit var viewModel: MainViewModel
    private var gestureService: GestureService? = null
    
    // UI
    private lateinit var driverNameText: TextView
    private lateinit var passengerStatusText: TextView
    private lateinit var passengerActionText: TextView
    private lateinit var profileDetails: TextView // Removed in XML, but we can remove it here too
    private lateinit var driverNameInput: android.widget.EditText
    private lateinit var registerButton: Button
    
    // New Preference Controls
    private lateinit var seatSpinner: android.widget.Spinner
    private lateinit var climateSeekBar: android.widget.SeekBar
    private lateinit var climateValue: TextView
    private lateinit var mirrorSpinner: android.widget.Spinner
    
    private val seatOptions = listOf("Default", "Reclined", "Sport", "Relax")
    private val mirrorOptions = listOf("Standard", "Angled Down", "Wide View")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        driverNameText = view.findViewById(R.id.driverNameText)
        passengerStatusText = view.findViewById(R.id.passengerStatusText)
        passengerActionText = view.findViewById(R.id.passengerActionText)
        // profileDetails = view.findViewById(R.id.profileDetails) // Removed
        driverNameInput = view.findViewById(R.id.driverNameInput)
        registerButton = view.findViewById(R.id.registerButton)
        
        seatSpinner = view.findViewById(R.id.seatSpinner)
        climateSeekBar = view.findViewById(R.id.climateSeekBar)
        climateValue = view.findViewById(R.id.climateValue)
        mirrorSpinner = view.findViewById(R.id.mirrorSpinner)
        
        // Setup Spinners
        val seatAdapter = android.widget.ArrayAdapter(requireContext(), R.layout.cabin_spinner_item, seatOptions)
        seatAdapter.setDropDownViewResource(R.layout.cabin_spinner_dropdown_item)
        seatSpinner.adapter = seatAdapter
        
        val mirrorAdapter = android.widget.ArrayAdapter(requireContext(), R.layout.cabin_spinner_item, mirrorOptions)
        mirrorAdapter.setDropDownViewResource(R.layout.cabin_spinner_dropdown_item)
        mirrorSpinner.adapter = mirrorAdapter
        
        // Setup SeekBar (16-30C mapped to 0-14)
        climateSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                climateValue.text = "${progress + 16}°C"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // Register Button Logic
        registerButton.setOnClickListener {
            val name = driverNameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                val seat = seatSpinner.selectedItem.toString()
                val climate = climateSeekBar.progress + 16
                val mirror = mirrorSpinner.selectedItem.toString()
                
                (requireActivity() as? com.example.gesturecontrol.MainActivity)?.registerCurrentDriver(name, seat, climate, mirror)
                
                driverNameInput.setText("")
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            } else {
                 android.widget.Toast.makeText(requireContext(), "Please enter a name", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.gestureFeedback.observe(viewLifecycleOwner) { feedback ->
             // Update Driver Name
            driverNameText.text = feedback.driverName
            
            if (feedback.driverName != "Guest") {
                driverNameText.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_success))
                registerButton.isEnabled = false
                registerButton.text = "REGISTERED"
                
                // Fetch and Load Profile
                val mainActivity = requireActivity() as? com.example.gesturecontrol.MainActivity
                val profile = mainActivity?.getDriverProfile(feedback.driverName)
                
                if (profile != null) {
                    val seatIdx = seatOptions.indexOf(profile.seatPosition)
                    if (seatIdx >= 0) seatSpinner.setSelection(seatIdx)
                    
                    val temp = profile.climateTemp
                    climateSeekBar.progress = temp - 16
                    climateValue.text = "${temp}°C"
                    
                    val mirrorIdx = mirrorOptions.indexOf(profile.mirrorSetting)
                    if (mirrorIdx >= 0) mirrorSpinner.setSelection(mirrorIdx)
                }
            } else {
                driverNameText.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_accent))
                registerButton.isEnabled = true
                registerButton.text = "REGISTER FACE"
            }

            // Update Passenger
            if (feedback.passengerDetected) {
                passengerStatusText.text = "OCCUPIED"
                passengerStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_success))
                passengerActionText.text = "Entertainment: ENABLED\nNavi Typing: ALLOWED"
            } else {
                passengerStatusText.text = "VACANT"
                passengerStatusText.setTextColor(android.graphics.Color.parseColor("#555555"))
                passengerActionText.text = "Entertainment: OFF\nNavi Typing: BLOCKED"
            }
        }
    }
}
