package com.example.savingbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.savingbuddy.ui.navigation.MainNavigation
import com.example.savingbuddy.ui.theme.SavingBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SavingBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MainNavigation()
                }
            }
        }
    }
}