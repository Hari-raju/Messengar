package com.raju.messengar.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.raju.messengar.connectivity.Connection;
import com.raju.messengar.connectivity.Constants;
import com.raju.messengar.connectivity.PreferenceManager;
import com.raju.messengar.databinding.ActivitySignUpBinding;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpBinding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signUpBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(signUpBinding.getRoot());
        if (!checkConnection()) {
            signUpBinding.buttonSignUp.setVisibility(View.INVISIBLE);
            return;
        }
        signUpBinding.progressSignup.setVisibility(View.GONE);
        preferenceManager = new PreferenceManager(getApplicationContext());
        signUpBinding.buttonSignUp.setVisibility(View.VISIBLE);
        Listeners();
    }

    private void Listeners() {
        //Setting on click listeners to navigate back sign in activity
        signUpBinding.textLogBackSignUp.setOnClickListener(view -> {
            onBackPressed();
        });

        //Setting Action listener for signup
        signUpBinding.buttonSignUp.setOnClickListener(view -> {
            signUpBinding.buttonSignUp.setVisibility(View.INVISIBLE);
            signUpBinding.progressSignup.setVisibility(View.VISIBLE);
            if (!isDetailsInvalid()) {
                signUpBinding.buttonSignUp.setVisibility(View.VISIBLE);
                signUpBinding.progressSignup.setVisibility(View.GONE);
            } else {
              signUP();
            }
        });

        signUpBinding.userProfileSignUP.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void signUP() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, signUpBinding.usernameSignUp.getEditText().getText().toString());
        user.put(Constants.KEY_EMAIL, signUpBinding.usermailSignUp.getEditText().getText().toString());
        user.put(Constants.KEY_PASSWORD, signUpBinding.userpasswordSignUp.getEditText().getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS).add(user).
                addOnSuccessListener(docReference -> {
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, docReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, signUpBinding.usernameSignUp.getEditText().getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).addOnFailureListener(exception -> {
                    signUpBinding.buttonSignUp.setVisibility(View.VISIBLE);
                    signUpBinding.progressSignup.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isDetailsInvalid() {
        if (!validateUserPassword() || !validateUserConfirmPassword() || !validateUserMail() || !validateUserName()) {
            return false;
        }
        if (encodedImage == null) {
            showToast("Insert your profile picture");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean validateUserName() {
        if (signUpBinding.usernameSignUp.getEditText().getText().toString().trim().isEmpty()) {
            signUpBinding.usernameSignUp.setError("Enter your name!");
            return false;
        }
        signUpBinding.usernameSignUp.setError(null);
        return true;
    }

    private boolean validateUserPassword() {
        String pattern = "^" + "(?=.*[a-zA-Z])" + "(?=\\S+$)" + ".{6,}" + "$";
        if (signUpBinding.userpasswordSignUp.getEditText().getText().toString().trim().isEmpty()) {
            signUpBinding.userpasswordSignUp.setError("Enter  Password!");
            return false;
        }
        if (!signUpBinding.userpasswordSignUp.getEditText().getText().toString().matches(pattern)) {
            signUpBinding.userpasswordSignUp.setError("Password is weak");
            return false;
        }
        signUpBinding.userpasswordSignUp.setError(null);
        return true;
    }

    private boolean validateUserConfirmPassword() {
        String pattern = "^" + "(?=.*[a-zA-Z])" + "(?=\\S+$)" + ".{6,}" + "$";
        if (signUpBinding.userConfirmpasswordSignUp.getEditText().getText().toString().trim().isEmpty()) {
            signUpBinding.userConfirmpasswordSignUp.setError("Enter  Password!");
            return false;
        }
        if (!signUpBinding.userConfirmpasswordSignUp.getEditText().getText().toString().matches(pattern)) {
            signUpBinding.userConfirmpasswordSignUp.setError("Password is weak!");
            return false;
        }
        if (!signUpBinding.userpasswordSignUp.getEditText().getText().toString().equals(signUpBinding.userConfirmpasswordSignUp.getEditText().getText().toString())) {
            signUpBinding.userConfirmpasswordSignUp.setError("Password isn't matching!");
            return false;
        }
        signUpBinding.userConfirmpasswordSignUp.setError(null);
        return true;
    }

    private boolean validateUserMail() {
        String pattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (signUpBinding.usermailSignUp.getEditText().getText().toString().trim().isEmpty()) {
            signUpBinding.usermailSignUp.setError("Enter your mail !");
            return false;
        }
        if (!signUpBinding.usermailSignUp.getEditText().getText().toString().matches(pattern)) {
            signUpBinding.usermailSignUp.setError("Enter valid Mail!");
            return false;
        }
        signUpBinding.usermailSignUp.setError(null);
        return true;
    }

    private String encodedImage(Bitmap bitmap) {
        //Setting width
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        //Creating a image with customized height and width
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //Compressing our image into jpeg format then putting it inside ByteArrayOutputStream
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        //then we converting it into byteArrray
        byte[] bytes = byteArrayOutputStream.toByteArray();
        //Encrypting it
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private ActivityResultLauncher<Intent> pickImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            signUpBinding.userProfileSignUP.setImageBitmap(bitmap);
                            signUpBinding.camIconSignUP.setVisibility(View.GONE);
                            encodedImage = encodedImage(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private boolean checkConnection() {
        Connection connection = new Connection();
        try {
            if (!connection.isConnected(getApplicationContext())) {
                Toast.makeText(this, "Turn on Internet", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}