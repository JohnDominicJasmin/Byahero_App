package com.example.commutingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import FirebaseUserManager.FirebaseUserManager;
import InternetConnection.ConnectionManager;
import Logger.CustomToastMessage;
import Logger.LoggerErrorMessage;
import MenuButtons.ButtonClicksTimeDelay;
import MenuButtons.CustomBackButton;
import ValidateUser.UserManager;

public class SignIn extends AppCompatActivity implements CustomBackButton {

    private EditText email, password;
    private FirebaseUserManager firebaseUserManager;
    private CustomToastMessage toastMessageIncorrectUserNameAndPassword;
    private CustomToastMessage toastMessageNoInternetConnection;
    private CustomToastMessage toastMessageBackButton;


    private ConnectionManager connectionManager;
    private ProgressBar circularProgressBar;
    private UserManager userManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        email = findViewById(R.id.editlogin_TextEmail);
        password = findViewById(R.id.editLogin_TextPassword);

        circularProgressBar = findViewById(R.id.SignInProgressBar);


        userManager = new UserManager();
        firebaseUserManager = new FirebaseUserManager();
        firebaseUserManager.initializeFirebase();

        toastMessageIncorrectUserNameAndPassword = new CustomToastMessage(this, "Username or password is incorrect", 3);
        toastMessageNoInternetConnection = new CustomToastMessage(this, LoggerErrorMessage.getNoInternetConnectionErrorMessage(), 2);
        toastMessageBackButton = new CustomToastMessage(this,"Tap again to exit.",10);

    }


    public void SignUpTextClicked(View view) {
        this.startActivity(new Intent(this, Signup.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectionManager = new ConnectionManager(this);
    }

    @Override
    public void onBackPressed() {
        backButtonClicked();
    }

    public void SignInButtonIsClicked(View view) {

    userManager.verifyUserForSignIn(email, password);

    if (userManager.UserInputRequirementsFailedAtSignIn()) {
        return;
    }

    if (!connectionManager.PhoneHasInternetConnection()) {
        toastMessageNoInternetConnection.showMessage();
        return;
    }
        toastMessageNoInternetConnection.hideMessage();
        signInUser();
    }









    private void signInUser(){

        String userUsername = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        firebaseUserManager.getFirebaseInstance().signInWithEmailAndPassword(userUsername, userPassword).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                circularProgressBar.setVisibility(View.VISIBLE);
                firebaseUserManager.getCurrentUser();
                toastMessageIncorrectUserNameAndPassword.hideMessage();
                showMainScreen();
                return;
            }
            toastMessageIncorrectUserNameAndPassword.showMessage();
        });

    }
    private void showMainScreen() {
        startActivity(new Intent(this, MainScreen.class));
        finish();
    }

    @Override
    public void backButtonClicked() {
        ButtonClicksTimeDelay backButtonClick = new ButtonClicksTimeDelay(2000);
        CustomBackButton customBackButton = ()->{
            if(backButtonClick.isDoubleTapped()){
                toastMessageBackButton.hideMessage();
                super.onBackPressed();
                return;
            }
            toastMessageBackButton.showMessage();
            backButtonClick.registerFirstClick();
        };
        customBackButton.backButtonClicked();
    }
}