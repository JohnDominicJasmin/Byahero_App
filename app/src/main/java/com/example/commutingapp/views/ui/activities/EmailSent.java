package com.example.commutingapp.views.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.commutingapp.R;
import com.example.commutingapp.databinding.ActivityEmailSentBinding;
import com.example.commutingapp.databinding.CircularProgressbarBinding;
import com.example.commutingapp.utils.ui_utilities.ActivitySwitch;
import com.example.commutingapp.viewmodels.EmailSentViewModel;
import com.example.commutingapp.views.dialogs.DialogDirector;
import com.example.commutingapp.views.menubuttons.BackButton;
import static com.example.commutingapp.utils.others.Constants.DELAY_INTERVAL_FOR_MAIN_SCREEN_ACTIVITY;
import static com.example.commutingapp.utils.others.Constants.DELAY_INTERVAL_FOR_NO_INTERNET_DIALOG;


public class EmailSent extends AppCompatActivity {


    private DialogDirector dialogDirector;
    private ActivityEmailSentBinding emailBinding;
    private CircularProgressbarBinding progressbarBinding;
    private EmailSentViewModel viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        initializeAttributes();
        displayUserEmailToTextView();
        subscribeToObservers();
        viewModel.refreshEmailSynchronously();



    }

    private void initializeAttributes() {
        emailBinding = ActivityEmailSentBinding.inflate(getLayoutInflater());
        progressbarBinding = CircularProgressbarBinding.bind(emailBinding.getRoot());
        setContentView(emailBinding.getRoot());
        dialogDirector = new DialogDirector(this);
        viewModel = new ViewModelProvider(this).get(EmailSentViewModel.class);

    }
    private void subscribeToObservers() {
        observeNoInternetActivityTransition();
        observeMainActivityTransition();
        observeEmailVerification();
    }

    private void observeMainActivityTransition() {
        viewModel.getMainScreenTransitionStatus().observe(this, transition -> {
            if (transition.getContentIfNotHandled() != null) {
                showMainScreenActivity();
            }
        });
    }

    private void observeNoInternetActivityTransition() {
        viewModel.getInternetConnectionStatus().observe(this, transition -> showNoInternetActivity());
    }

    private void observeEmailVerification() {
        viewModel.sendEmailOnSuccess().observe(this, task -> {
            dialogDirector.showSuccessDialog(getString(R.string.newEmailSent), getString(R.string.resendEmailSuccessMessage));
        });
        viewModel.sendEmailOnFail().observe(this, task -> {
            dialogDirector.showWarningDialog(getString(R.string.pleaseCheckYourInbox), getString(R.string.resendEmailFailedMessage));
        });
    }




    private void destroyBinding() {
        emailBinding = null;
        progressbarBinding = null;
    }






    @Override
    protected void onStart() {
        super.onStart();
    }





    private void showNoInternetActivity() {
        progressbarBinding.circularProgressBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {

            dialogDirector.buildNoInternetDialog().setOnDismissListener(T->viewModel.refreshEmailSynchronously());
            progressbarBinding.circularProgressBar.setVisibility(View.INVISIBLE);
        }, DELAY_INTERVAL_FOR_NO_INTERNET_DIALOG);
    }

    private void showMainScreenActivity() {
        progressbarBinding.circularProgressBar.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> {
            ActivitySwitch.INSTANCE.startActivityOf(this, MainScreen.class);
        }, DELAY_INTERVAL_FOR_MAIN_SCREEN_ACTIVITY);

    }

    @Override
    public void onBackPressed() {
        new BackButton().applyDoubleClickToExit(this);
    }

    private void displayUserEmailToTextView() {
        viewModel.displayUserEmailToTextView().observe(this, userEmail -> emailBinding.textViewEmail.setText(userEmail));
    }

    public void resendEmailIsClicked(View view) {

        startVerificationTimer();
        viewModel.sendEmailVerification();
    }

    private void displayWhenVerificationTimerStarted(long secondsLeft) {
        emailBinding.ResendVerificationButton.setTextColor(ContextCompat.getColor(this, R.color.gray));
        emailBinding.ResendVerificationButton.setText(new StringBuilder().append(getString(R.string.resendVerification)).append(" in").append(secondsLeft).append("s").toString());
        emailBinding.ResendVerificationButton.setEnabled(false);
    }

    private void displayWhenVerificationTimerIsFinished() {
        emailBinding.ResendVerificationButton.setTextColor(ContextCompat.getColor(this, R.color.blue2));
        emailBinding.ResendVerificationButton.setText(getString(R.string.resendVerification));
        emailBinding.ResendVerificationButton.setEnabled(true);
    }

    private void startVerificationTimer() {
        viewModel.startTimer();
        viewModel.getTimerOnRunningStatus().observe(this, this::displayWhenVerificationTimerStarted);
        viewModel.getTimerOnFinishedStatus().observe(this, timer -> displayWhenVerificationTimerIsFinished());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBinding();
    }


}
