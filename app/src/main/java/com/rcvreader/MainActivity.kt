package com.rcvreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rcvreader.ui.theme.RCVReaderTheme
import com.rcvreader.ui.reading.ReadingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RCVReaderTheme {
                ReadingScreen()
            }
        }
    }
}
