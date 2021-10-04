package com.example.commutingapp.utils.InternetConnection

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager


object Connection {


    @SuppressLint("MissingPermission")
    @Suppress("Deprecation")
    fun hasInternetConnection(context: Context):Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.apply {
            return isConnected && isAvailable
        }
        return false
    }






}