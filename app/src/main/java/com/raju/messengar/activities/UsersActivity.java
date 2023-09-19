package com.raju.messengar.activities;



import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.raju.messengar.adapters.UsersAdapter;
import com.raju.messengar.connectivity.Connection;
import com.raju.messengar.connectivity.Constants;
import com.raju.messengar.connectivity.PreferenceManager;
import com.raju.messengar.databinding.ActivityUsersBinding;
import com.raju.messengar.listeners.UserListeners;
import com.raju.messengar.user_models.Users;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListeners {
    ActivityUsersBinding usersBinding;
    PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usersBinding=ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(usersBinding.getRoot());
        if(!checkConnection()){
            return;
        }
        preferenceManager=new PreferenceManager(getApplicationContext());
        Listeners();
        getUsers();
    }


    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                //Entering Db
                .addOnCompleteListener(task ->{
                    loading(false);
                    //getting current id
                    String currentUserId=preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.getResult()!=null &&task.isSuccessful()){
                        //Creating a list of users which we created
                        List<Users> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot:task.getResult()) {
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                //ignoring current users id
                                continue;
                            }
                            //other than current users id we are adding all users details into the list
                            Users user = new Users();
                            user.name=queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email=queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image=queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token=queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id=queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        //After loop ends we are gonna display those details if its > 0
                        if(users.size()>0){
                            //Creating instance of our customized adapter
                            UsersAdapter usersAdapter = new UsersAdapter(users,this);
                            //setting up or connecting our layout adapter to customized adapter
                            usersBinding.usersRecycleView.setAdapter(usersAdapter);
                            usersBinding.usersRecycleView.setVisibility(View.VISIBLE);
                        }
                        else{
                            showErrorMsg();
                        }
                    }
                    else{
                        showErrorMsg();
                    }
                });
    }

    private void Listeners(){
        usersBinding.backUsers.setOnClickListener(view ->{
            onBackPressed();
        });
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

    private void loading(boolean value){
        if(value){
            usersBinding.progressUsers.setVisibility(View.VISIBLE);
        }
        else{
            usersBinding.progressUsers.setVisibility(View.INVISIBLE);
        }
    }

    private void showErrorMsg(){
        usersBinding.textErrorMsg.setText(String.format("%s","No users found"));
        usersBinding.textErrorMsg.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserClicked(Users user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }

}