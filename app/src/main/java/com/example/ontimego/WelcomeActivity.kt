package com.example.ontimego

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import com.example.ontimego.ui.theme.OnTimeGoTheme

/**
 * Utilisée pour gérer la transition entre les pages de lancement welcome et modetransport
 * Si les infos ont déjà été entrées alors l'utilisateur ne passe plus par là
 */
class WelcomeActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OnTimeGoTheme {
                var currentScreen by remember { mutableStateOf("welcome") }
                var userName by remember { mutableStateOf("") }

                if (currentScreen == "welcome") {
                    WelcomeScreen { name ->
                        userName = name
                        currentScreen = "transport"
                    }
                } else if (currentScreen == "transport") {
                    TransportModeScreen(userName) { transportMode ->
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
