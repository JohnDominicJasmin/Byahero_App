package com.example.commutingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;

import FirebaseUserManager.FirebaseUserManager;
import Logger.CustomToastMessage;
import MenuButtons.BackButtonDoubleClicked;
import MenuButtons.CustomBackButton;
import MenuButtons.backButton;
import Screen.ScreenDimension;

public class splashscreen extends AppCompatActivity implements BackButtonDoubleClicked {

    private final int delayInMillis = 1000;
    private CustomToastMessage toastMessageBackButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ScreenDimension(getWindow()).windowToFullScreen();
        setContentView(R.layout.activity_splashscreen);
        toastMessageBackButton = new CustomToastMessage(this, getString(R.string.doubleTappedMessage), 10);
        FirebaseUserManager.initializeFirebase();

    }
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUserManager.getCurrentUser();
        if (FirebaseUserManager.isUserAlreadySignedIn() && FirebaseUserManager.getFirebaseUserInstance().isEmailVerified()) {
            showMainScreen();
            return;
        }
        new Handler().postDelayed(this::showIntroSliders, delayInMillis);
    }
    @Override
    public void onBackPressed() {
        backButtonClicked();
    }
    private void showMainScreen() {
        startActivity(new Intent(this, MainScreen.class));
        finish();
    }
    private void showIntroSliders() {
        startActivity(new Intent(this, IntroSlider.class));
        finish();
    }
    @Override
    public void backButtonClicked() {

        new CustomBackButton(()->{
            if(backButton.isDoubleTapped()){
                toastMessageBackButton.hideToast();
                super.onBackPressed();
                return;
            }
            toastMessageBackButton.showToast();
            backButton.registerFirstClick();
        }).backButtonIsClicked();

    

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

         