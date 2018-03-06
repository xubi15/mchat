package com.marceme.marcefirebasechat.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.marceme.marcefirebasechat.R;
import com.marceme.marcefirebasechat.adapter.ChatUsersChatAdapter;
import com.marceme.marcefirebasechat.login.ChatLogInActivity;
import com.marceme.marcefirebasechat.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;


/*
* CAUTION: This app is still far away from a production app
* Note: (1) Still fixing some code, and functionality and
*       I don't use FirebaseUI, but recommend you to use it.
* */

public class ChatMainActivity extends Activity {


    private static String TAG = ChatMainActivity.class.getSimpleName();

    @BindView(R.id.progress_bar_users)
    ProgressBar mProgressBarForUsers;
    @BindView(R.id.recycler_view_users)
    RecyclerView mUsersRecyclerView;

    private String mCurrentUserUid;
    private List<String> mUsersKeyList;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mUserRefDatabase;
    private ChildEventListener mChildEventListener;
    private ChatUsersChatAdapter mChatUsersChatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_main);

        bindButterKnife();
        setAuthInstance();
        setUsersDatabase();
        setUserRecyclerView();
        setUsersKeyList();
        setAuthListener();
    }

    private void bindButterKnife() {
        ButterKnife.bind(this);
    }

    private void setAuthInstance() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setUsersDatabase() {
        mUserRefDatabase = FirebaseDatabase.getInstance().getReference().child("users");
    }

    private void setUserRecyclerView() {

        mChatUsersChatAdapter = new ChatUsersChatAdapter(this, new ArrayList<User>());
        mUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUsersRecyclerView.setHasFixedSize(true);
        mUsersRecyclerView.setAdapter(mChatUsersChatAdapter);
    }

    private void setUsersKeyList() {
        mUsersKeyList = new ArrayList<String>();
    }

    private void setAuthListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                hideProgressBarForUsers();
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {

                    setUserData(user);
                    queryAllUsers();
                } else {
                    // User is signed out
                    goToLogin();
                }
            }
        };
    }

    private void setUserData(FirebaseUser user) {

        mCurrentUserUid = user.getUid();
    }

    private void queryAllUsers() {
        mChildEventListener = getChildEventListener();
        mUserRefDatabase.limitToFirst(50).addChildEventListener(mChildEventListener);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, ChatLogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // LoginActivity is a New Task
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // The old task when coming back to this activity should be cleared so we cannot come back to it.
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        showProgressBarForUsers();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        clearCurrentUsers();

        if (mChildEventListener != null) {
            mUserRefDatabase.removeEventListener(mChildEventListener);
        }

        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }

    }

    private void clearCurrentUsers() {
        mChatUsersChatAdapter.clear();
        mUsersKeyList.clear();
    }

    private void logout() {
        showProgressBarForUsers();
        setUserOffline();
        mAuth.signOut();
    }

    private void setUserOffline() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            mUserRefDatabase.child(userId).child("connection").setValue(ChatUsersChatAdapter.OFFLINE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProgressBarForUsers() {
        mProgressBarForUsers.setVisibility(View.VISIBLE);
    }

    private void hideProgressBarForUsers() {
        if (mProgressBarForUsers.getVisibility() == View.VISIBLE) {
            mProgressBarForUsers.setVisibility(View.GONE);
        }
    }

    private ChildEventListener getChildEventListener() {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.exists()) {

                    String userUid = dataSnapshot.getKey();

                    /*User user = dataSnapshot.getValue(User.class);
                    System.out.println("UserType-->: " + user.getUserType());*/

                    if (dataSnapshot.getKey().equals(mCurrentUserUid)) {
                        User currentUser = dataSnapshot.getValue(User.class);
                        mChatUsersChatAdapter.setCurrentUserInfo(userUid, currentUser.getEmail(), currentUser.getCreatedAt(), currentUser.getUserType());
                    } else {
                        User user = dataSnapshot.getValue(User.class);
                        System.out.println("UserType-->: " + user.getUserType());
                        //System.out.println("email-->: " + user.getEmail());

                        if (Objects.equals(user.getUserType(), "user")) {

                            User recipient = dataSnapshot.getValue(User.class);
                            recipient.setRecipientId(userUid);
                            mUsersKeyList.add(userUid);
                            mChatUsersChatAdapter.refill(recipient);
                            hideProgressBarForUsers();
                        }
                    }
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists()) {
                    String userUid = dataSnapshot.getKey();
                    if (!userUid.equals(mCurrentUserUid)) {

                        User user = dataSnapshot.getValue(User.class);

                        int index = mUsersKeyList.indexOf(userUid);
                        if (index > -1) {
                            mChatUsersChatAdapter.changeUser(index, user);
                        }
                    }

                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

}