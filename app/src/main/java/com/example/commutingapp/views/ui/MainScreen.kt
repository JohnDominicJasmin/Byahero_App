package com.example.commutingapp.views.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.commutingapp.R
import com.example.commutingapp.databinding.ActivityMainScreenBinding
import com.example.commutingapp.utils.FirebaseUserManager.FirebaseManager
import com.example.commutingapp.utils.ui_utilities.ActivitySwitcher
import com.example.commutingapp.utils.ui_utilities.AttributesInitializer
import com.example.commutingapp.utils.ui_utilities.BindingDestroyer
import com.example.commutingapp.utils.ui_utilities.ScreenDimension
import com.example.commutingapp.views.Logger.CustomToastMessage
import com.example.commutingapp.views.MenuButtons.CustomBackButton
import com.facebook.login.LoginManager
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserInfo
import java.util.*

class MainScreen : AppCompatActivity(),AttributesInitializer,BindingDestroyer {
    private var toastMessageBackButton: CustomToastMessage? = null
    private var activityMainScreenBinding: ActivityMainScreenBinding? = null
    private lateinit var userInfo: MutableList<out UserInfo>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeAttributes()
        FirebaseManager.initializeFirebaseApp()

    }

    override fun onDestroy() {
        super.onDestroy()
        destroyBinding()
    }
    override fun onStart() {
        super.onStart()
        displayUserProfileName()
    }
    override fun initializeAttributes() {
        ScreenDimension(window).setWindowToFullScreen()
        activityMainScreenBinding = ActivityMainScreenBinding.inflate(layoutInflater)
        setContentView(activityMainScreenBinding?.root)
        toastMessageBackButton = CustomToastMessage(this, getString(R.string.doubleTappedMessage), 10)
        userInfo = FirebaseManager.getFirebaseUserInstance().providerData
    }
    override fun destroyBinding() {
        activityMainScreenBinding = null
    }



    private val userProfileName: String?
        get() {
            for (user in userInfo) {
                if (userSignInViaFacebook(user.providerId) || userSignInViaGoogle(user.providerId)) {
                    Log.e("User id from main screen",user.providerId)
                    return FirebaseManager.getFirebaseUserInstance().displayName
                }
            }
            return filterEmailAddress(FirebaseManager.getFirebaseUserInstance().email)
        }

    private fun userSignInViaFacebook(userProviderId:String) = userProviderId == FacebookAuthProvider.PROVIDER_ID
    private fun userSignInViaGoogle(userProviderId:String) = userProviderId == GoogleAuthProvider.PROVIDER_ID



    private fun displayUserProfileName() {
        activityMainScreenBinding?.nameTextView?.text = userProfileName
    }

    private fun filterEmailAddress(userEmail: String?): String? {
       val emailLists = emailExtensions
       userEmail?.let{
         for (counter in emailLists.indices) {
           val emailExtension = emailLists[counter]
              if (userEmail.contains(emailExtension)) {
                 return userEmail.replace(emailExtension.toRegex(), "")
              }
           }
        }
        return userEmail
    }

    private val emailExtensions: List<String>
        get() {
            val list: MutableList<String> = ArrayList()
            list.add("@gmail.com")
            list.add("@protonmail.ch")
            list.add("@yahoo.com")
            list.add("@hotmail.com")
            list.add("@outlook.com")
            return list
        }

    fun logoutButtonIsClicked(view: View?) {
        Log.e(javaClass.name, "Logging out!")
        putUserToLoginFlow()
    }

    private fun signOutAccount() {
        LoginManager.getInstance().logOut()
        FirebaseManager.getFirebaseAuthInstance().signOut()
    }

    private fun putUserToLoginFlow() {
        signOutAccount()
        showSignInActivity()
    }

    private fun showSignInActivity() {

        ActivitySwitcher.startActivityOf(this,this, SignIn::class.java)
    }

    override fun onBackPressed() {
       CustomBackButton(this,this).applyDoubleClickToExit()
    }



}