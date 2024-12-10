package com.example.ontimego

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.ontimego.ui.theme.OnTimeGoTheme
import androidx.compose.foundation.Image
import kotlinx.coroutines.*

/**
 * Page avec le logo affichée à tout lancement de l'application
 */
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SplashScreen()
        }

        val sharedPreferences = getSharedPreferences("OnTimeGoPrefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", null)
        val transportMode = sharedPreferences.getString("TRANSPORT_MODE", null)

        val nextActivity = if (userName.isNullOrEmpty() || transportMode.isNullOrEmpty()) {
            WelcomeActivity::class.java
        } else {
            MainActivity::class.java
        }
        val allPrefs = sharedPreferences.all
        println("SharedPreferences au démarrage : $allPrefs")

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            startActivity(Intent(this@SplashActivity, nextActivity))
            finish()
        }
    }

    @Composable
    fun SplashScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(R.drawable.logo), contentDescription = "Logo")
        }
    }
}