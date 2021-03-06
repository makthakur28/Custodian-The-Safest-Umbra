package com.example.custodian;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chaos.view.PinView;
import com.example.custodian.Modals.UserModal;
import com.example.custodian.NotificationHandler.FirebaseInstanceId;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceIdReceiver;

import java.util.concurrent.TimeUnit;

public class OtpVerification extends AppCompatActivity {

    FirebaseAuth mAuth;
    String VerificationCodeBySystem;
    PinView pinView;
    Button verify_btn;
    private String name,phone,password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);
        Intent intent = getIntent();

        pinView = findViewById(R.id.pin_view);
        mAuth = FirebaseAuth.getInstance();
        verify_btn = findViewById(R.id.verify_btn);

        name = intent.getStringExtra("name");
        password = intent.getStringExtra("password");
        phone = intent.getStringExtra("phone");

        pinView.requestFocus();


         sendOtpverification(phone);
         
         verify_btn.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 String code = pinView.getText().toString();
                 if (!code.isEmpty()){
                     verifyCode(code);
                 }
                 else{
                     Toast.makeText(OtpVerification.this, "Enter Code..", Toast.LENGTH_SHORT).show();
                 }
             }
         });

    }

    private void sendOtpverification(String phone) {

        Toast.makeText(this, "Verification proccessed"+ phone, Toast.LENGTH_SHORT).show();
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91"+phone)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);

    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            VerificationCodeBySystem=s;
        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            String code = phoneAuthCredential.getSmsCode();
            if (code!=null){
                pinView.setText(code);
                verifyCode(code);
            }
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            Toast.makeText(OtpVerification.this, "verification failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(VerificationCodeBySystem,code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            addDataToFirebase();

                            Intent intent = new Intent(OtpVerification.this, MainActivity.class);
                            intent.putExtra("name", name);
                            intent.putExtra("phone", "+91"+phone);
                            intent.putExtra("password", password);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(OtpVerification.this, "Wrong OTP..!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void addDataToFirebase() {

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");

            UserModal user = new UserModal(name,"+91"+phone, password);
            ref.child("+91"+phone).setValue(user);

    }
}