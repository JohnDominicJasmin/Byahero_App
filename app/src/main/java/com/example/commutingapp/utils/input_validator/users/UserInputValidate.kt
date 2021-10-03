package com.example.commutingapp.utils.input_validator.users

class UserInputValidate constructor(var validator: ValidateInput) {


    /**kind of weird putting '== true' there, but it's part of kotlin null safety btw.*/
    fun signUpValidationFail(): Boolean {
        return validator.validationEmailFailed() == true ||
                validator.validationPasswordFailed() == true||
                validator.validationConfirmPasswordFailed() == true
    }

    fun signInValidationFail(): Boolean {
        return validator.validationEmailFailed() == true ||
                validator.validationPasswordFailed() == true
    }

}