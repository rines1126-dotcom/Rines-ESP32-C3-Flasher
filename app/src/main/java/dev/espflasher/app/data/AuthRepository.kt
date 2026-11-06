
package dev.espflasher.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInAnonymously(): FirebaseUser {
        val user = auth.currentUser

        if (user != null) {
            return user
        }

        return auth.signInAnonymously().await().user
            ?: throw IllegalStateException("Anonymous sign-in failed.")
    }

    suspend fun register(
        email: String,
        password: String
    ): FirebaseUser {
        return auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).await().user
            ?: throw IllegalStateException("Registration failed.")
    }

    suspend fun signIn(
        email: String,
        password: String
    ): FirebaseUser {
        return auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).await().user
            ?: throw IllegalStateException("Sign-in failed.")
    }

    suspend fun signInWithGoogleIdToken(
        idToken: String
    ): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        return auth.signInWithCredential(credential).await().user
            ?: throw IllegalStateException("Google sign-in failed.")
    }

    suspend fun signOut() {
        auth.signOut()
        signInAnonymously()
    }
}
