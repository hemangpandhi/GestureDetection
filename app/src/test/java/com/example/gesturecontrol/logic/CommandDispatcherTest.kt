package com.example.gesturecontrol.logic

import android.content.Context
import android.media.AudioManager
import org.junit.Test
import org.mockito.Mockito.*

class CommandDispatcherTest {

    @Test
    fun testOnGestureRecognized_OpenPalm_PausesMedia() {
        // Mock Context and AudioManager
        val mockContext = mock(Context::class.java)
        val mockAudioManager = mock(AudioManager::class.java)
        
        // When getSystemService is called, return mockAudioManager
        `when`(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        
        // Create Dispatcher
        val dispatcher = CommandDispatcher(mockContext)
        
        // Trigger Gesture
        dispatcher.onGestureRecognized("Open_Palm")
        
        // Verify Media Key Event dispatched (roughly - KeyEvent is hard to mock strictly without Robolectric, 
        // but we can check if AudioManager was accessed).
        verify(mockAudioManager, atLeastOnce()).dispatchMediaKeyEvent(any())
    }
}
