package com.example.commutingapp.views.ui.activities;

import static com.example.commutingapp.R.string.disabledAccountMessage;
import static com.example.commutingapp.R.string.incorrectEmailOrPasswordMessage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.commutingapp.R;
import com.example.commutingapp.data.firebase.auth.FirebaseAuthenticatorWrapper;
import com.example.commutingapp.data.firebase.auth.UserAuthenticationProcessor;
import com.example.commutingapp.data.firebase.usr.FirebaseUserWrapper;
import com.example.commutingapp.data.firebase.usr.UserDataProcessor;
import com.example.commutingapp.data.firebase.usr.UserEmailProcessor;
import com.example.commutingapp.databinding.ActivitySignInBinding;
import com.example.commutingapp.databinding.CircularProgressbarBinding;
import com.example.commutingapp.utils.input_validator.users.UserValidatorManager;
import com.example.commutingapp.utils.input_validator.users.UserValidatorModel;
import com.example.commutingapp.utils.InternetConnection.ConnectionManager;
import com.example.commutingapp.utils.ui_utilities.ActivitySwitcher;
import com.example.commutingapp.utils.ui_utilities.ScreenDimension;
import com.example.commutingapp.views.dialogs.DialogDirector;

import com.example.commutingapp.views.MenuButtons.CustomBackButton;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;


public class SignIn extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private final String TAG = "FacebookAuthentication";
    private final String FACEBOOK_CONNECTION_FAILURE = "CONNECTION_FAILURE: CONNECTION_FAILURE";
    private DialogDirector director;
    private CallbackManager facebookCallBackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivitySignInBinding activitySignInBinding;
    private CircularProgressbarBinding circularProgressbarBinding;
    private DialogDirector dialogDirector;

    private FirebaseUserWrapper firebaseUser;

    private UserAuthenticationProcessor<Task<AuthResult>> userAuth;
    private UserDataProcessor<List<UserInfo>> userData;
    private UserEmailProcessor<Task<Void>> userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        initializeAttributes();
        createRequestSignOptionsGoogle();
        FacebookSdk.sdkInitialize(getApplicationContext());
        facebookCallBackManager = CallbackManager.Factory.create();
        removeFacebookUserAccountPreviousToken();

    }


    private void initializeAttributes() {


        activitySignInBinding = ActivitySignInBinding.inflate(getLayoutInflater());
        circularProgressbarBinding = CircularProgressbarBinding.bind(activitySignInBinding.getRoot());
        new ScreenDimension(getWindow()).setWindowToFullScreen();
        setContentView(activitySignInBinding.getRoot());
        dialogDirector = new DialogDirector(this);
        firebaseUser = new FirebaseUserWrapper();
        userAuth = new UserAuthenticationProcessor(new FirebaseAuthenticatorWrapper());
        userData = new UserDataProcessor(firebaseUser);
        userEmail =  new UserEmailProcessor(firebaseUser);
        director = new DialogDirector(this);
    }


    public void FacebookButtonIsClicked(View view) {
        loginViaFacebook();
    }

    public void loginViaFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(facebookCallBackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                showLoading();
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.e(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                handleFacebookSignInException(error);
            }
        });
    }

    private void handleFacebookSignInException(Exception error) {

        if (Objects.equals(error.getMessage(), FACEBOOK_CONNECTION_FAILURE)) {
            showNoInternetActivity();
            return;
        }
        if (error.getMessage() != null) {
            dialogDirector.constructErrorDialog("Error", error.getMessage());
            removeFacebookUserAccountPreviousToken();
        }
    }


    private void handleFacebookAccessToken(AccessToken token) {

        AuthCredential authCredential = FacebookAuthProvider.getCredential(token.getToken());
        userAuth.signInWithCredential(authCredential).
                addOnCompleteListener(this,
                        task -> {
                            if (task.isSuccessful()) {
                                userData.saveCreatedAccount();
                                showMainScreenActivity();
                                return;
                            }
                            if (task.getException() != null) {
                                finishLoading();
                                handleFacebookSignInException(task.getException());
                            }
                        });
    }

    private void removeFacebookUserAccountPreviousToken() {
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        handleFacebookSignInCallback(requestCode, resultCode, data);
        handleGoogleSignInCallback(requestCode, data);

    }

    private void handleFacebookSignInCallback(int requestCode, int resultCode, Intent data) {
        facebookCallBackManager.onActivityResult(requestCode, resultCode, data);
    }


    private void handleGoogleSignInCallback(int requestCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                showLoading();
                authenticateFirebaseWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                handleGoogleSignInException(e);
            }
        }

    }

    private void handleGoogleSignInException(ApiException error) {

        if (error.getStatus().isCanceled()) {
            return;
        }
        if (!error.getStatus().isSuccess() && noInternetConnection()) {
            showNoInternetActivity();
            return;
        }
        Timber.e("when onCancel() event occur");

    }

    private boolean noInternetConnection() {
        return !new ConnectionManager(this).internetConnectionAvailable();
    }

    private void createRequestSignOptionsGoogle() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    private void authenticateFirebaseWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        userAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        userData.saveCreatedAccount();
                        showMainScreenActivity();
                        return;
                    }
                    finishLoading();
                    dialogDirector.constructErrorDialog("Error", "Authentication Failed. Please try again later.");
                });
    }

    @Override
    protected void onDestroy() {
        LoginManager.getInstance().unregisterCallback(facebookCallBackManager);
        destroyBinding();
        super.onDestroy();
    }

    private void destroyBinding() {
        activitySignInBinding = null;
        circularProgressbarBinding = null;
    }

    public void SignUpTextClicked(View view) {
        showSignUpActivity();
    }

    @Override
    public void onBackPressed() {
        new CustomBackButton(this, this).applyDoubleClickToExit();
    }

    public void SignInButtonIsClicked(View view) {
        loginViaDefaultSignIn();
    }

    private void loginViaDefaultSignIn() {
        UserValidatorManager user = new UserValidatorManager(new UserValidatorModel(this,
                activitySignInBinding.editloginTextEmail,
                activitySignInBinding.editLoginTextPassword,
                null));

        if (user.signInValidationFail()) {
            return;
        }

        if (noInternetConnection()) {
            showNoInternetActivity();
            return;
        }

        proceedToSignIn();
    }

    private void proceedToSignIn() {

        String email = Objects.requireNonNull(activitySignInBinding.editloginTextEmail.getText()).toString().trim();
        String userPassword = Objects.requireNonNull(activitySignInBinding.editLoginTextPassword.getText()).toString().trim();

        showLoading();
        userAuth.signInWithEmailAndPassword(email, userPassword).addOnCompleteListener(this, signInTask -> {
            if (signInTask.isSuccessful()) {
                userData.saveCreatedAccount();
                verifyUserEmail();
                return;
            }
            if (signInTask.getException() != null) {
                handleFirebaseSignInException(signInTask);
                finishLoading();
            }
        });

    }

    private void verifyUserEmail() {
        userEmail.reloadEmail().addOnCompleteListener(emailReload -> {
            if (emailReload.isSuccessful() && Objects.requireNonNull(userEmail.isEmailVerified())) {
                showMainScreenActivity();
                return;
            }
            showEmailSentActivity();

        });
    }

    private void handleFirebaseSignInException(Task<?> task) {
        try {
            throw Objects.requireNonNull(task.getException());
        } catch (FirebaseNetworkException firebaseNetworkException) {
            showNoInternetActivity();
        } catch (FirebaseAuthInvalidUserException firebaseAuthInvalidUserException) {
            handleUserAccountExceptions(firebaseAuthInvalidUserException);
        } catch (Exception e) {
            dialogDirector.constructErrorDialog("Oops..", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void handleUserAccountExceptions(FirebaseAuthInvalidUserException exception) {
        if (exception.getErrorCode().equals("ERROR_USER_DISABLED")) {
            dialogDirector.constructErrorDialog("Oops..", getString(disabledAccountMessage));
            return;
        }
        dialogDirector.constructErrorDialog("Oops..", getString(incorrectEmailOrPasswordMessage));

    }

    private void showLoading() {
        processLoading(false, View.VISIBLE);
    }

    private void finishLoading() {
        processLoading(true, View.INVISIBLE);
    }

    private void processLoading(boolean visible, int progressBarVisibility) {
        circularProgressbarBinding.circularProgressBar.setVisibility(progressBarVisibility);
        activitySignInBinding.editloginTextEmail.setEnabled(visible);
        activitySignInBinding.editLoginTextPassword.setEnabled(visible);
        activitySignInBinding.LogInButton.setEnabled(visible);
        activitySignInBinding.FacebookButton.setEnabled(visible);
        activitySignInBinding.GoogleButton.setEnabled(visible);
        activitySignInBinding.TextViewDontHaveAnAccount.setEnabled(visible);
        activitySignInBinding.TextViewSignUp.setEnabled(visible);

    }

    private void showMainScreenActivity() {

        ActivitySwitcher.INSTANCE.startActivityOf(this, this, MainScreen.class);
    }

    private void showNoInternetActivity() {

        director.constructNoInternetDialog();
    }

    private void showEmailSentActivity() {

        ActivitySwitcher.INSTANCE.startActivityOf(this, this, EmailSent.class);
    }

    private void showSignUpActivity() {
        ActivitySwitcher.INSTANCE.startActivityOf(this, this, Signup.class);
    }

    public void googleButtonIsClicked(View view) {
        loginViaGoogle();
    }

    private void loginViaGoogle() {

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


}