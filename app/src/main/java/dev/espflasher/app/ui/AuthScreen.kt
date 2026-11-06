
package dev.espflasher.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    vm: AuthViewModel,
    onContinueAsGuest: () -> Unit
) {
    val state by vm.state.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    val credentialManager = remember {
        CredentialManager.create(context)
    }

    fun signInWithGoogle() {
        scope.launch {
            googleLoading = true
            googleError = null

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(
                        context.getString(R.string.default_web_client_id)
                    )
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type ==
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(
                            credential.data
                        )

                    vm.signInWithGoogleIdToken(
                        googleCredential.idToken
                    )
                } else {
                    googleError = "Invalid Google account credential."
                }
            } catch (e: Exception) {
                googleError = e.message
                    ?: "Google Sign-In was cancelled or failed."
            } finally {
                googleLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Rines ESP32-C3 Flasher",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRegister) {
                "Create your account"
            } else {
                "Sign in to your account"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isRegister) {
                    vm.register(email, password)
                } else {
                    vm.signIn(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading &&
                !googleLoading &&
                email.isNotBlank() &&
                password.length >= 6
        ) {
            Text(if (isRegister) "Register" else "Sign In")
        }

        TextButton(
            onClick = {
                isRegister = !isRegister
                vm.clearMessages()
                googleError = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !googleLoading
        ) {
            Text(
                if (isRegister) {
                    "Already have an account? Sign In"
                } else {
                    "Create a new account"
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { signInWithGoogle() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !googleLoading
        ) {
            Text("Continue with Google")
        }

        TextButton(
            onClick = onContinueAsGuest,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !googleLoading
        ) {
            Text("Continue as Guest")
        }

        if (state.isLoading || googleLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error
            )
        }

        googleError?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.message?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
