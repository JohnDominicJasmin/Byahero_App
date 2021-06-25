package ValidateUser;

import android.util.Patterns;
import android.widget.EditText;

import static android.content.res.Resources.getSystem;
import static com.example.commutingapp.R.string.*;


public class User implements CharactersValidation {
    private EditText name;
    private EditText email;
    private EditText phoneNumber;
    private EditText password;
    private EditText confirmPassword;


    public User(EditText name, EditText email, EditText phoneNumber, EditText password, EditText confirmPassword) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public User(EditText email, EditText password) {
        this.email = email;
        this.password = password;
    }

    public User() {

    }

    public String getNname() {
        return name.getText().toString();
    }

    public String getEmail() {
        return email.getText().toString();
    }

    public String getPhoneNumber() {
        return phoneNumber.getText().toString();
    }

    public String getPassword() {
        return password.getText().toString();
    }

    public String getConfirmPassword() {
        return confirmPassword.getText().toString();
    }


    public boolean validateNameFailed() {

        String nameInput = name.getText().toString().trim();

        if (isNull(nameInput)) {
            name.setError(getSystem().getString(getFieldLeftBlankMessage));
            name.requestFocus();
            return true;
        }

        if (nameInput.length() < 3) {
            name.setError(getSystem().getString(getNameTooShortMessage));
            name.requestFocus();
            return true;
        }

        if (hasSpecialCharacters(nameInput)) {
            name.setError(getSystem().getString(getNameHasSpecialCharactersMessage));
            name.requestFocus();
            return true;
        }
        name.setError(null);
        return false;
    }


    public boolean validateEmailFailed() {
        String emailInput = email.getText().toString().trim();

        if (isNull(emailInput)) {
            email.setError(getSystem().getString(getFieldLeftBlankMessage));
            email.requestFocus();
            return true;
        }

        if (!validEmail()) {
            email.setError(getSystem().getString(getEmailIsInvalidMessage));
            email.requestFocus();
            return true;
        }
        email.setError(null);
        return false;
    }

    public boolean validateConfirmPasswordFailed() {

        String confirmPasswordInput = confirmPassword.getText().toString().trim();

        if (isNull(confirmPasswordInput)) {
            confirmPassword.setError(getSystem().getString(getFieldLeftBlankMessage));
            confirmPassword.requestFocus();
            return true;
        }
        if (Password_Is_Unmatch()) {
            confirmPassword.setError(getSystem().getString(getPasswordIsNotMatchMessage));
            confirmPassword.requestFocus();
            return true;
        }

        if (!isPasswordStrong()) {
            confirmPassword.setError(getSystem().getString(getPasswordIsWeakMessage));
            confirmPassword.requestFocus();
            return true;

        }
        confirmPassword.setError(null);
        return false;
    }

    public boolean validatePasswordFailed() {
        String passwordInput = password.getText().toString().trim();

        if (isNull(passwordInput)) {
            password.setError(getSystem().getString(getFieldLeftBlankMessage));
            password.requestFocus();
            return true;
        }

        password.setError(null);
        return false;
    }

    public boolean validatePhoneNumberFailed() {

        String phoneNumberInput = phoneNumber.getText().toString().trim();
        if(isNull(phoneNumberInput)){
            phoneNumber.setError(getSystem().getString(getFieldLeftBlankMessage));
            phoneNumber.requestFocus();
            return true;
        }
        if (hasSpecialCharacters(phoneNumberInput) || isNumberSizeIncorrect() || isPhoneNumberDoesNotStartAtZero()) {
            phoneNumber.setError(getSystem().getString(getPhoneNumberIsInvalidMessage));
            phoneNumber.requestFocus();
            return true;
        }


        phoneNumber.setError(null);
        return false;
    }


    private boolean Password_Is_Unmatch() {
        return (!password.getText().toString().trim().equals(confirmPassword.getText().toString().trim()));
    }

    private boolean isPasswordStrong() {
        return confirmPassword.getText().toString().trim().toCharArray().length >= 8 && (hasNumber(confirmPassword.getText().toString().trim()) || hasSpecialCharacters(confirmPassword.getText().toString().trim()));
    }

    private boolean validEmail() {
        return Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches();
    }

    private boolean isNumberSizeIncorrect() {
        return phoneNumber.getText().toString().trim().toCharArray().length != 11;
    }

    private boolean isNull(String input) {
        return input.trim().isEmpty();
    }

    private boolean isPhoneNumberDoesNotStartAtZero() {
        return !phoneNumber.getText().toString().trim().startsWith("0");
    }
}
