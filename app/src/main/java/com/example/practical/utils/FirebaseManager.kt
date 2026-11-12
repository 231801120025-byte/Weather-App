package com.example.practical.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseManager(context: Context) {

    init {
        ensureFirebaseApp(context)
    }

    private fun ensureFirebaseApp(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                FirebaseApp.initializeApp(context)
            } catch (e: Exception) {
                // Fallback initialization if google-services.json is missing
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:mockappid")
                    .setApiKey("AIzaSyMockApiKeyForAppInitOnly")
                    .setProjectId("practical-weather-app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        }
    }

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    fun getCurrentUser(): FirebaseUser? {
        return auth?.currentUser
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val a = auth
        if (a == null) {
            onResult(false, "Firebase Auth not available")
            return
        }
        a.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Sign in failed")
                }
            }
    }

    fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val a = auth
        if (a == null) {
            onResult(false, "Firebase Auth not available")
            return
        }
        a.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Registration failed")
                }
            }
    }

    fun signInAnonymously(onResult: (Boolean, String?) -> Unit) {
        val a = auth
        if (a == null) {
            onResult(false, "Firebase Auth not available")
            return
        }
        a.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Anonymous auth failed")
                }
            }
    }

    fun signOut() {
        auth?.signOut()
    }
}
