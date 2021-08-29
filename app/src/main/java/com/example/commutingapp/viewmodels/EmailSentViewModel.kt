package com.example.commutingapp.viewmodels

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commutingapp.R
import com.example.commutingapp.utils.FirebaseUserManager.FirebaseManager
import com.example.commutingapp.utils.ui_utilities.Event
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.*

private const val twoMinutes: Long = 120000
private const val oneSecond: Long = 1000
private const val two_Seconds: Long = 2000

class EmailSentViewModel:ViewModel() {

    private lateinit var verificationTimer: CountDownTimer
    private lateinit var job: Job
    var timerOnRunning = MutableLiveData<Int>()
    private set

    var timerOnFinished = MutableLiveData<Boolean>()
    private set

    var mainScreenActivityTransition = MutableLiveData<Event<Boolean>>()
    private set

    var noInternetActivityTransition = MutableLiveData<Boolean>()
    private set

    var sendEmailToUser = MutableLiveData<String?>()
    private set

    private var displayUserEmail = MutableLiveData<String>()



    fun displayUserEmailToTextView():LiveData<String>{

        viewModelScope.launch(Dispatchers.Main) {
            FirebaseManager.getFirebaseUserInstance().email?.let {
                displayUserEmail.value = it
            }
        }.also {
            return displayUserEmail
        }

    }




    fun refreshEmailSynchronously(){
         job = viewModelScope.launch(Dispatchers.IO) {
            while (this.isActive) {
                reloadUserEmail()
                Log.d("COROUTINE STATUS: ", "$isActive")
                delay(two_Seconds)
            }
        }


    }


   private fun reloadUserEmail() {
    FirebaseManager.getFirebaseUserInstance().reload().addOnCompleteListener { reload ->
        if (reload.isSuccessful && FirebaseManager.getFirebaseUserInstance().isEmailVerified) {
            mainScreenActivityTransition.postValue(Event(true))
            return@addOnCompleteListener
        }
        reload.exception?.let {
            handleEmailVerificationExceptions(reload)
        }
    }
    }


    fun sendEmailVerification(){
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseManager.getFirebaseUserInstance().sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sendEmailToUser.postValue(R.string.resendEmailSuccessMessage.toString())
                        return@addOnCompleteListener
                    }
                    sendEmailToUser.postValue(null)
                }
        }
    }


    private fun handleEmailVerificationExceptions(task:Task<*>){
        try{
            throw task.exception!!
        }catch (networkException: FirebaseNetworkException){
           noInternetActivityTransition.value = true
        }catch (ex:Exception){
            Log.e("Exception",ex.message.toString())
        }finally {
           job.cancel()
        }
    }



    fun startTimer(){

        viewModelScope.launch(Dispatchers.Main) {
            verificationTimer = object : CountDownTimer(twoMinutes, oneSecond) {
                override fun onTick(millisUntilFinished: Long) {
                    val timeLeft = millisUntilFinished / oneSecond
                    timerOnRunning.value = timeLeft.toInt()
                }

                override fun onFinish() {
                    timerOnFinished.value = true
                    stopTimer()
                }
            }.start()
        }
    }



    private fun stopTimer(){

        if(::verificationTimer.isInitialized){
            verificationTimer.cancel()
        }

    }
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
