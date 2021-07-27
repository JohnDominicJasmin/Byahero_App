package com.example.commutingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import FirebaseUserManager.FirebaseUserManager;
import Logger.CustomToastMessage;
import MenuButtons.CustomBackButton;
import MenuButtons.backButton;
import MenuButtons.BackButtonDoubleClicked;

public class MainScreen extends AppCompatActivity implements BackButtonDoubleClicked {

    private CustomToastMessage toastMessageBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_screen);
        toastMessageBackButton = new CustomToastMessage(this,getString(R.string.doubleTappedMessage),10);
        FirebaseUserManager.initializeFirebase();

    }



    public void LogoutButtonClicked(View view) {
       signOutUser();
    }


    private void signOutUser(){
        FirebaseUserManager.getFirebaseAuthInstance().signOut();
        startActivity(new Intent(this, SignIn.class));
        finish();
    }
    @Override
    public void onBackPressed() {
      backButtonClicked();
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
}