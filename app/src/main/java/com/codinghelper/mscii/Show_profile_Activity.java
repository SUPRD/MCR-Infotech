package com.codinghelper.mscii;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;              //1....2...3...are the code sequence matlb code iss number ke hisab ke sath chlaga
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class Show_profile_Activity extends AppCompatActivity {
private String senderUserId, receiverUserID,currentState;
private CircularImageView userImage;
private TextView userProfilename,userProfileStatus;
private Button AddRequest,RemoveRequest;
private DatabaseReference reference,addRequestref,friendref,NotificationRef;
private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_profile_);
        auth=FirebaseAuth.getInstance();
        senderUserId=auth.getCurrentUser().getUid();
        receiverUserID=getIntent().getExtras().get("visit_user_id").toString();
        userImage=(CircularImageView)findViewById(R.id.visit_profile_image);
        userProfilename=(TextView)findViewById(R.id.visit_username);
        userProfileStatus=(TextView)findViewById(R.id.visit_userstatus);
        AddRequest=(Button)findViewById(R.id.add_friend);
        RemoveRequest=(Button)findViewById(R.id.remove_friend);
        reference= FirebaseDatabase.getInstance().getReference().child("studentDetail");
        addRequestref= FirebaseDatabase.getInstance().getReference().child("Add Friend Request");
        NotificationRef=FirebaseDatabase.getInstance().getReference().child("Notifications");
        friendref= FirebaseDatabase.getInstance().getReference().child("Friend list");
        //user position=current state
        currentState="new";
        //1.
        RetriveUserInfo();
    }
//1-->
    private void RetriveUserInfo() {
        reference.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
               if(dataSnapshot.exists()&&(dataSnapshot.hasChild("imageUrl"))){
                   String Simg =String.valueOf(dataSnapshot.child("imageUrl").getValue());
                   Picasso.get().load(Simg).fit().centerCrop().noFade().placeholder(R.drawable.main_stud).into(userImage);
                   String Sname =String.valueOf(dataSnapshot.child("username").getValue());
                   userProfilename.setText(Sname);
                   String Sstatus =String.valueOf(dataSnapshot.child("userstatus").getValue());
                   userProfileStatus.setText(" "+Sstatus);
                 //2.
                   ManageAddRequest();

               }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
//2-->
    private void ManageAddRequest() {
        addRequestref.child(senderUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(receiverUserID)){
                            String request_type=String.valueOf(dataSnapshot.child(receiverUserID).child("Request_type").getValue());
                            if(request_type.equals("sent")){
                                currentState="request_sent";
                                AddRequest.setText("Cancel Request");
                            }else if(request_type.equals("received")){
                                currentState="request_received";
                                AddRequest.setText("Confirm friend");
                                RemoveRequest.setVisibility(View.VISIBLE);
                                RemoveRequest.setEnabled(true);
                                RemoveRequest.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        CancelFreirdRequest();
                                    }
                                });
                            }
                        }else {
                            friendref.child(senderUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.hasChild(receiverUserID)){
                                                currentState="friends";
                                                AddRequest.setText("Unfriend");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
        //3.checks if current user not sending request to him/her self
        //*agr nhi equal hota hn tab request sent hoga
        if(!senderUserId.equals(receiverUserID)){
            AddRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //ek baar add request mai click krne ka baad dubara na work ho isliy esetenabled function ka use kr re hn
                    AddRequest.setEnabled(false);
                    //add request enabled false matlb abb user uss button ko press nhi kr skta jb tak enabled true na ho jay
                    if(currentState.equals("new")){
                        sendAddRequest();
                    }
                    if(currentState.equals("request_sent")){
                        CancelFreirdRequest();
                    }
                    if(currentState.equals("request_received")){
                        ConfirmFriendRequest();
                    }
                    if(currentState.equals("friends")){
                        RemoveFriend();
                    }
                }
            });
        }else {
            //*agr user ka current id reciver id ke sath match hota hn too usme add friend ka option he nhi ana chahya
            AddRequest.setVisibility(View.INVISIBLE);
        }
    }

    private void RemoveFriend() {
        friendref.child(senderUserId).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendref.child(receiverUserID).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                AddRequest.setEnabled(true);
                                                currentState="new";
                                                AddRequest.setText("Add friend");
                                                RemoveRequest.setVisibility(View.INVISIBLE);
                                                RemoveRequest.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void ConfirmFriendRequest() {
        friendref.child(senderUserId).child(receiverUserID)
                .child("Friends").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendref.child(receiverUserID).child(senderUserId)
                                    .child("Friends").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                addRequestref.child(senderUserId).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    addRequestref.child(receiverUserID).child(senderUserId)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                  AddRequest.setEnabled(true);
                                                                                  currentState="friends";
                                                                                  AddRequest.setText("Unfriend");
                                                                                  RemoveRequest.setVisibility(View.INVISIBLE);
                                                                                  RemoveRequest.setEnabled(false);

                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void CancelFreirdRequest() {
        addRequestref.child(senderUserId).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            addRequestref.child(receiverUserID).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                AddRequest.setEnabled(true);
                                                currentState="new";
                                                AddRequest.setText("Add friend");
                                                RemoveRequest.setVisibility(View.INVISIBLE);
                                                RemoveRequest.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void sendAddRequest() {
        //sender--->receiver
        addRequestref.child(senderUserId).child(receiverUserID)
                .child("Request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            //receiver--->sender
                            addRequestref.child(receiverUserID).child(senderUserId)
                                    .child("Request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            HashMap<String,String> friendNotificationMap =new HashMap<>();
                                            friendNotificationMap.put("from",senderUserId);
                                            friendNotificationMap.put("type","request");
                                            NotificationRef.child(receiverUserID).push()
                                                    .setValue(friendNotificationMap)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){
                                                                AddRequest.setEnabled(true);
                                                                currentState="request_sent";
                                                                AddRequest.setText("Cancel Request");
                                                            }
                                                        }
                                                    });

                                        }
                                    });

                        }
                    }
                });
    }
}