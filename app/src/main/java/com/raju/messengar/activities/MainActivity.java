package com.raju.messengar.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.raju.messengar.adapters.RecentConversationsAdapter;
import com.raju.messengar.connectivity.Connection;
import com.raju.messengar.connectivity.Constants;
import com.raju.messengar.connectivity.PreferenceManager;
import com.raju.messengar.databinding.ActivityMainBinding;
import com.raju.messengar.listeners.ConversationListener;
import com.raju.messengar.user_models.ChatMessage;
import com.raju.messengar.user_models.Users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversationListener {

    private ActivityMainBinding mainBinding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        preferenceManager=new PreferenceManager(getApplicationContext());
        if(!checkConnection()){
            mainBinding.loadingMain.setVisibility(View.VISIBLE);
            mainBinding.newChat.setVisibility(View.INVISIBLE);
            return;
        }
        mainBinding.loadingMain.setVisibility(View.INVISIBLE);
        mainBinding.newChat.setVisibility(View.VISIBLE);
        init();
        loadDetails();
        getToken();
        Listeners();
        listConversations();
    }

    private void listConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    private void init(){
       conversations =new ArrayList<>();
       conversationsAdapter = new RecentConversationsAdapter(conversations,this);
       mainBinding.recentChatRecycler.setAdapter(conversationsAdapter);
       database=FirebaseFirestore.getInstance();
    }
    private void loadDetails(){
        mainBinding.userNameMain.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes= Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        mainBinding.profileImageMain.setImageBitmap(bitmap);
    }

    private void Listeners(){
        mainBinding.signOutMain.setOnClickListener(view ->signOut());
        mainBinding.newChat.setOnClickListener(view ->{
            startActivity(new Intent(getApplicationContext(),UsersActivity.class));
        });
    }
    private void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private final EventListener<QuerySnapshot> eventListener = (value,error) ->{
        //Meaning if there is an error return
        if(error!=null){
            return;
        }
        //Meaning:There are more than 1 results were fetched
        if(value!=null){
            //Meaning All recent chats of Friends
            for (DocumentChange documentChange:value.getDocumentChanges()){
                //Meaning:New Account is created
                if (documentChange.getType()== DocumentChange.Type.ADDED){
                    //We are getting the document or chats sender and receiver ids
                    String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId=senderId;
                    chatMessage.receiverId=receiverId;
                    //If Sender sends something then Save Receiver Details
                    if(preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                    }
                    //If Receiver sends something then Save Sender Details
                    else{
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                    }
                    chatMessage.message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                }
                //Meaning : Already existing accounts chat is modified (recent chat)
                else if(documentChange.getType()== DocumentChange.Type.MODIFIED){
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        //Current Document is Changed so we have to update things for that we have to check all previous conversations one by one and select the correct one which sender as well as receiver id matches then apply updations
                        if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            //Sorting the list of messages based on time
            Collections.sort(conversations,(ob1,ob2)->ob2.dateObject.compareTo(ob1.dateObject));
            //Mentioning that data sets are changed
            conversationsAdapter.notifyDataSetChanged();
            mainBinding.recentChatRecycler.smoothScrollToPosition(0);
            mainBinding.recentChatRecycler.setVisibility(View.VISIBLE);
//            mainBinding.loadingMain.setVisibility(View.GONE);
        }
    };

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN,token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(result ->{
                    showToast("Unable to update token");
                });
    }

    private void signOut(){
        showToast("Signing out.....");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String,Object> value= new HashMap<>();
        value.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(value)
                .addOnSuccessListener(result ->{
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(result -> showToast(result.getMessage()));
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

    @Override
    public void onConversionClicked(Users user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}
