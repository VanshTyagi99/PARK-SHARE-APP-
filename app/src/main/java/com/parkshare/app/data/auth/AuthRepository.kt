package com.parkshare.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signUp(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(
                        task.exception?.message
                            ?: "Account creation failed"
                    )
                }
            }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(
                        task.exception?.message
                            ?: "Login failed"
                    )
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUser() = auth.currentUser

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return try {
            val result = user.getIdToken(forceRefresh).await()
            result.token
        } catch (e: Exception) {
            null
        }
    }

    fun getIdToken(
        forceRefresh: Boolean = false,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("No authenticated user found")
            return
        }
        user.getIdToken(forceRefresh)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result?.token != null) {
                    onSuccess(task.result.token!!)
                } else {
                    onError(task.exception?.message ?: "Failed to retrieve ID token")
                }
            }
    }
}