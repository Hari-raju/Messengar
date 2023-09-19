package com.raju.messengar.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raju.messengar.databinding.RecentConversationContainerBinding;
import com.raju.messengar.listeners.ConversationListener;
import com.raju.messengar.user_models.ChatMessage;
import com.raju.messengar.user_models.Users;

import java.util.List;

//Adapter will be used as an intermediate which fetch data from one place and display it for user
public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversationHolder> {
    private final ConversationListener conversationListener;
    private final List<ChatMessage> chats;
    public RecentConversationsAdapter(List<ChatMessage> list,ConversationListener conversationListener) {
        this.chats = list;
        this.conversationListener=conversationListener;
    }

    @NonNull
    @Override
    public ConversationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationHolder(
                RecentConversationContainerBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationHolder holder, int position) {
        holder.setData(chats.get(position));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    //View holder is responsible for holding the data
     class ConversationHolder extends RecyclerView.ViewHolder{
         RecentConversationContainerBinding recentChatBinding;
         ConversationHolder(RecentConversationContainerBinding recentConversationContainerBinding){
             super(recentConversationContainerBinding.getRoot());
             recentChatBinding =recentConversationContainerBinding;
         }

         void setData(ChatMessage chatMessage){
             recentChatBinding.userProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
             recentChatBinding.textName.setText(chatMessage.conversionName);
             recentChatBinding.textRecentMessage.setText(chatMessage.message);
             recentChatBinding.getRoot().setOnClickListener(v->{
                 Users user= new Users();
                 user.id= chatMessage.conversionId;
                 user.image= chatMessage.conversionImage;
                 user.name= chatMessage.conversionName;
                 conversationListener.onConversionClicked(user);
             });
         }
    }

    private Bitmap getConversionImage(String encodedImage){
        byte[] bytes= Base64.decode(encodedImage,Base64.DEFAULT);
        //Bitmap factory converts any files into an bitmap object
        //source,offset -> from where to start ,End
        return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
    }
}
