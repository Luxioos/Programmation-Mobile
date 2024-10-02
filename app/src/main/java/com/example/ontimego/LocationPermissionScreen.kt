package com.example.ontimego

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationPermissionScreen(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Autoriser la localisation ?")
        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = { onFinish() }) {
                Text("Oui")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { onFinish() }) {
                Text("Non")
            }
        }
    }
}
