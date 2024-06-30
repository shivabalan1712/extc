package com.example.expensetrackerr;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

     TextView profileNameTextView, profileEmailTextView;
     Button logoutButton;
     FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileNameTextView = view.findViewById(R.id.profileNameTextView);
        profileEmailTextView = view.findViewById(R.id.profileEmailTextView);
        logoutButton = view.findViewById(R.id.logoutButton);
        auth = FirebaseAuth.getInstance();

        loadProfileData();

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
    }

    private void loadProfileData() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        profileNameTextView.setText(name);
                        profileEmailTextView.setText(email);
                    });
        }
    }
}
