package com.example.commutingapp.data.firebase.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthenticatorWrapper(
    private var auth:FirebaseAuth = FirebaseAuth.getInstance()
):UserAuthenticator<Task<AuthResult>>  {


    override fun createUserWithEmailAndPassword(email: String, password: String):Task<AuthResult>{
       return auth.createUserWithEmailAndPassword(email,password)
    }

    override fun signOut() {
        auth.signOut()
    }

    override fun signInWithCredential(authCredential: AuthCredential):Task<AuthResult> {
       return auth.signInWithCredential(authCredential)
    }

    override fun signInWithEmailAndPassword(email: String, password: String):Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }
}