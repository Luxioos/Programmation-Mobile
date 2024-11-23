package com.example.ontimego

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.window.SplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import com.example.ontimego.ui.theme.OnTimeGoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Handler
import androidx.compose.foundation.Image
import androidx.lifecycle.lifecycleScope

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("OnTimeGoPrefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("USER_NAME", null)
        val transportMode = sharedPreferences.getString("TRANSPORT_MODE", null)

        /*CoroutineScope(Dispatchers.Main).launch {
            delay(2000) // 2 secondes
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }*/
        lifecycleScope.launch {
            delay(2000) // 2 secondes

            val nextActivity = if (userName != null && transportMode != null) {
                MainActivity::class.java
            } else {
                WelcomeActivity::class.java
            }
            startActivity(Intent(this@SplashActivity, nextActivity))
            finish()
        }

        setContent {
            OnTimeGoTheme {
                SplashScreen()
            }
        }
    }

    @Composable
    fun SplashScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(R.drawable.logo), contentDescription = "Logo")
        }
    }

    @Preview
    @Composable
    fun SplashScreenPreview() {
        OnTimeGoTheme {
            SplashScreen()
        }
    }
}