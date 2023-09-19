package com.raju.messengar.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.raju.messengar.Networks.ApiClient;
import com.raju.messengar.Networks.ApiService;
import com.raju.messengar.adapters.ChatAdapter;
import com.raju.messengar.connectivity.Connection;
import com.raju.messengar.connectivity.Constants;
import com.raju.messengar.connectivity.PreferenceManager;
import com.raju.messengar.databinding.ActivityChatBinding;
import com.raju.messengar.user_models.ChatMessage;
import com.raju.messengar.user_models.Users;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ChatActivity extends BaseActivity {
    private ActivityChatBinding chatBinding;
    private Users receiver;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private String conversationId=null;
    private boolean isUserAvailabilty=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatBinding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(chatBinding.getRoot());
        if(!checkConnection()){
            chatBinding.progressChat.setVisibility(View.VISIBLE);
        }
        else {
            chatBinding.progressChat.setVisibility(View.GONE);
            Listeners();
            loadDetails();
            init();
            listenerMessages();
            checkAvailabiltyOfReceiver();
        }
    }

    private void sendNotifications(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessages(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@Nonnull Call<String> call, @Nonnull Response<String> response) {
                if(response.isSuccessful()){
                    try{
                        if(response.body()!=null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure")==1){
                                JSONObject error = (JSONObject)results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("Notifications Sent");
                }
                else{
                    showToast("Error : "+response.code());
                }
            }

            //Notification sending was failed
            @Override
            public void onFailure(@Nonnull Call<String> call,@Nonnull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void checkAvailabiltyOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiver.id
        )
        .addSnapshotListener(ChatActivity.this,(value,error)->{
           if(error!=null){
               return;
           }
           if(value!=null){
               if(value.getLong(Constants.KEY_AVAILABILITY)!=null){
                   int availability= Objects.requireNonNull(
                           value.getLong(Constants.KEY_AVAILABILITY)
                   ).intValue();
                   isUserAvailabilty=1==availability;
               }
               receiver.token=value.getString(Constants.KEY_FCM_TOKEN);
               if(receiver.image==null){
                   receiver.image=value.getString(Constants.KEY_IMAGE);
                   chatAdapter.setReceiverProfile(getEncodedImage(receiver.image));
                   chatAdapter.notifyItemRangeChanged(0,chatMessages.size());
               }
               if(isUserAvailabilty){
                   chatBinding.userOnline.setVisibility(View.VISIBLE);
                   chatBinding.userLastSeen.setVisibility(View.GONE);
               }
               else{
                   if(value.getDate(Constants.KEY_LAST_SEEN)!=null){
                       chatBinding.userOnline.setVisibility(View.GONE);
                       SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm");
                       String date = formatter.format(Objects.requireNonNull(value.getDate(Constants.KEY_LAST_SEEN)));
                       chatBinding.userLastSeen.setText(String.format("Last seen at :%s", date));
                       chatBinding.userLastSeen.setVisibility(View.VISIBLE);
                   }
                   else{
                       chatBinding.userLastSeen.setVisibility(View.GONE);
                       chatBinding.userOnline.setVisibility(View.GONE);
                   }
               }
           }
        });
    }

    //Setting the actual listeners which will call eventlisteners and retrieve those chats
    private void listenerMessages(){
        //For Sending Messages
        //Here as a sender Sender id and user id need to be same
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiver.id)
                .addSnapshotListener(eventListener);

        //For Receiving Messages
        //Here as a reciever sender id and reciever id need to be same
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiver.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    //It is for after user clicked a account we are gonna retrieve those chats using this
    private final EventListener<QuerySnapshot> eventListener = (value,error) ->{
        //if error occurs we have to go back
        if(error!=null){
            return;
        }
        if(value!=null){
            int count=chatMessages.size();
            //Changed documents meaning : new chats also considered as change bcz we are inserting some new data
            for(DocumentChange documentChange : value.getDocumentChanges()){
                //Added means new chat
                if(documentChange.getType()==DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    //Getting that chats sender id
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    //Getting that chats receiver id
                    chatMessage.receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    //Getting that chats message
                    chatMessage.message=documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    //Getting the time when user actually receives message
                    chatMessage.dateTime=getReadableTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    //Getting time of message was send
                    chatMessage.dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);//
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if(count==0){
                //After you change the contents of the ArrayList , you need to tell the list that the source of the data had changed and it needs to redraw itself to show the new data.
                chatAdapter.notifyDataSetChanged();
            }
            else{
                //What it means is in chats new messages always in bottom and old will be on top (Here we mentioning after position start (Means : Size of chat meaning bottom) where the new chat need to be showed)
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                chatBinding.chatRecyler.smoothScrollToPosition(chatMessages.size()-1);
            }
            chatBinding.chatRecyler.setVisibility(View.VISIBLE);
            //If no recent messages are created it need to be created otherwise we can just update it
            if (conversationId==null){
                checkConversations();
            }
        }
    };

    private Bitmap getEncodedImage(String encoded){
        if(encoded!=null){
            byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        }
        else{
            return null;
        }
    }

    public void init(){
        preferenceManager=new PreferenceManager(getApplicationContext());
        chatMessages =new ArrayList<>();
        database=FirebaseFirestore.getInstance();
        chatAdapter= new ChatAdapter(
                getEncodedImage(receiver.image), chatMessages, preferenceManager.getString(Constants.KEY_USER_ID)
        );
        chatBinding.chatRecyler.setAdapter(chatAdapter);
    }

    private void sendMessages(){
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,receiver.id);
        message.put(Constants.KEY_MESSAGE,chatBinding.inputChat.getText().toString());
        message.put(Constants.KEY_TIMESTAMP,new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if (conversationId!=null){
            updateConversation(chatBinding.inputChat.getText().toString());
        }
        else{
            HashMap<String,Object> conversations=new HashMap<>();
            conversations.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversations.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversations.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
            conversations.put(Constants.KEY_RECEIVER_ID,receiver.id);
            conversations.put(Constants.KEY_RECEIVER_NAME,receiver.name);
            conversations.put(Constants.KEY_RECEIVER_IMAGE,receiver.image);
            conversations.put(Constants.KEY_LAST_MESSAGE,chatBinding.inputChat.getText().toString());
            addConversations(conversations);
        }
        if(!isUserAvailabilty){
            if(receiver.token!=null){
                try{
                    //Both are used for transmitting some data from server to user

                    //Its like a real Array
                    JSONArray tokens = new JSONArray();
                    tokens.put(receiver.token);

                    //Its like a hashmap
                    JSONObject data = new JSONObject();
                    data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                    data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                    data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                    data.put(Constants.KEY_MESSAGE,chatBinding.inputChat.getText().toString());

                    //Its used for combining both tokens and data
                    JSONObject body = new JSONObject();
                    body.put(Constants.REMOTE_MSG_DATA,data);
                    body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
                    sendNotifications(body.toString());
                }
                catch(Exception e){
                    showToast(e.getMessage());
                }
            }
        }
        chatBinding.inputChat.setText(null);
    }
    private void loadDetails(){
        receiver =(Users)getIntent().getSerializableExtra(Constants.KEY_USER);
        chatBinding.textName.setText(receiver.name);
    }
    private void Listeners(){
        chatBinding.backChatActivity.setOnClickListener(v -> onBackPressed());
        chatBinding.layoutSend.setOnClickListener(v -> sendMessages());
    }

    //Returning time of user receiving msg
    private String getReadableTime(Date date){
        return new SimpleDateFormat("MM dd,yyyy - hh:mm a", Locale.getDefault()).format(date);
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

    //Updating the last Conversations
    private void updateConversation(String message){
        //Point to the particular mentioned chat
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP,new Date()
        );
    }

    //Adding the last conversations
    private void addConversations(HashMap<String,Object> conversation){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId=documentReference.getId());
    }
    private void checkConversations(){
        if(chatMessages.size()!=0){
            //From sender to receiver
            checkForConversationRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiver.id
            );
            //From receiver to sender
            checkForConversationRemotely(
                    receiver.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    //Checking for the last conversations
    private void checkForConversationRemotely(String senderId,String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).
                whereEqualTo(Constants.KEY_SENDER_ID,senderId).
                whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId).
                get().
                addOnCompleteListener(conversationCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
            //We are trying to get the last message that's y we put get(0)
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            //Here we getting the id of it
            conversationId=documentSnapshot.getId();
        }
    };
}