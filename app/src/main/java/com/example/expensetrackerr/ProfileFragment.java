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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment implements OnMapReadyCallback {

    private TextView profileNameTextView, profileEmailTextView;
    private Button logoutButton, viewLocationButton;
    private FirebaseAuth auth;
    private GoogleMap googleMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileNameTextView = view.findViewById(R.id.profileNameTextView);
        profileEmailTextView = view.findViewById(R.id.profileEmailTextView);
        logoutButton = view.findViewById(R.id.logoutButton);
        viewLocationButton = view.findViewById(R.id.viewLocationButton); // Add view location button

        auth = FirebaseAuth.getInstance();

        loadProfileData();

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // Handle view location button click
        viewLocationButton.setOnClickListener(v -> {
            // Navigate to view location or perform other actions
            // For Google Maps, you would typically show the map here
            // You can update this according to your use case
            // Example: showMapAtLocation();
        });

        // Initialize Google Map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        return view;
    }

    private void loadProfileData() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        profileNameTextView.setText("Name: " + name);
                        profileEmailTextView.setText("Email: " + email);
                    });
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Example location to show on the map (Chennai, Tamil Nadu)
        LatLng chennai = new LatLng(13.0827, 80.2707);
        googleMap.addMarker(new MarkerOptions().position(chennai).title("Marker in Chennai"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(chennai));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }
}
