package com.example.ontimego

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(onNext: (String) -> Unit) {
    var userName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Bienvenue sur OnTimeGO ! Votre nom ?")
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Nom") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onNext(userName) },
            enabled = userName.isNotBlank()
        ) {
            Text("Suivant")
        }
    }
}
