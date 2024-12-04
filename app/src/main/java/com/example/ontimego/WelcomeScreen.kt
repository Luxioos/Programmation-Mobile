package com.example.ontimego

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Page au lancement quand l'utilisateur doit entrer son nom
 */
@Composable
fun WelcomeScreen(onNext: (String) -> Unit) {
    var userName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

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
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (userName.isNotEmpty()) {
                    val sharedPreferences = context.getSharedPreferences("OnTimeGoPrefs", MODE_PRIVATE)
                    sharedPreferences.edit()
                        .putString("USER_NAME", userName)
                        .apply()
                    onNext(userName)
                }
            },
            enabled = userName.isNotBlank()
        ) {
            Text("Suivant")
        }
    }
}
