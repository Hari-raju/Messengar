package com.raju.messengar.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.raju.messengar.connectivity.Connection;
import com.raju.messengar.connectivity.Constants;
import com.raju.messengar.connectivity.PreferenceManager;
import com.raju.messengar.databinding.ActivitySignInBinding;
public class SignInActivity extends AppCompatActivity {

    //Binding is used for binding the layout file with our java file and allows us the direct access of elements and its id's

    private ActivitySignInBinding signInBinding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signInBinding =ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(signInBinding.getRoot());
        if (!checkConnection()){
            signInBinding.buttonSignIn.setVisibility(View.INVISIBLE);
            return;
        }
        preferenceManager=new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        signInBinding.buttonSignIn.setVisibility(View.VISIBLE);
        Listeners();
    }
    private void Listeners(){
        signInBinding.textCreateNewAccountSignIn.setOnClickListener(view ->{
            checkConnection();
            //It navigates to Sign up activity getApplication context refers to our current activity
            startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
        });

        signInBinding.buttonSignIn.setOnClickListener(view ->{
            signInBinding.buttonSignIn.setVisibility(View.INVISIBLE);
            signInBinding.progressSignin.setVisibility(View.VISIBLE);
            if(!isDetailsValid()){
                signInBinding.buttonSignIn.setVisibility(View.VISIBLE);
                signInBinding.progressSignin.setVisibility(View.INVISIBLE);
                return;
            }
            else{
                FirebaseFirestore database =FirebaseFirestore.getInstance();
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .whereEqualTo(Constants.KEY_EMAIL,signInBinding.usermailSignIn.getEditText().getText().toString())
                        .whereEqualTo(Constants.KEY_PASSWORD,signInBinding.userpasswordSignIn.getEditText().getText().toString()).get()
                        .addOnCompleteListener(task -> {
                            if(task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0){
                                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                                preferenceManager.putString(Constants.KEY_USER_ID,documentSnapshot.getId());
                                preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));
                                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                            else{
                                Toast.makeText(this, "Unable to sign in", Toast.LENGTH_SHORT).show();
                                signInBinding.buttonSignIn.setVisibility(View.VISIBLE);
                                signInBinding.progressSignin.setVisibility(View.INVISIBLE);
                            }
                        });
            }
        });
    }

    private boolean isDetailsValid() {
        if(!validateUserMail()||!validateUserPassword()){
            return false;
        }
        else{
            return true;
        }
    }

    private boolean checkConnection(){
        Connection connection =new Connection();
        try {
            if (!connection.isConnected(getApplicationContext())) {
                Toast.makeText(this, "Turn on Internet", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    private boolean validateUserMail(){
        String pattern="[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if(signInBinding.usermailSignIn.getEditText().getText().toString().trim().isEmpty()){
            signInBinding.usermailSignIn.setError("Enter your mail !");
            return false;
        }
        if(!signInBinding.usermailSignIn.getEditText().getText().toString().matches(pattern)){
            signInBinding.usermailSignIn.setError("Enter valid Mail!");
            return false;
        }
        signInBinding.usermailSignIn.setError(null);
        return true;
    }

    private boolean validateUserPassword(){
        String pattern="^"+"(?=.*[a-zA-Z])"+"(?=\\S+$)"+".{6,}"+"$";
        if(signInBinding.userpasswordSignIn.getEditText().getText().toString().trim().isEmpty()){
            signInBinding.userpasswordSignIn.setError("Enter  Password!");
            return false;
        }
        if(!signInBinding.userpasswordSignIn.getEditText().getText().toString().matches(pattern)){
            signInBinding.userpasswordSignIn.setError("Password is weak");
            return false;
        }
        signInBinding.userpasswordSignIn.setError(null);
        return true;
    }
}