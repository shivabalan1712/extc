package com.example.expensetrackerr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Register extends AppCompatActivity {

    EditText SignUpName, SignUpEmail, SignUpPassword;
    String uid;
    Button signUp;
    ProgressBar progressBar;
    FirebaseAuth fAuth2;
    FirebaseFirestore fStore2;

    @Override
    public void onStart() {
        super.onStart();
        fAuth2 = FirebaseAuth.getInstance();
        FirebaseUser currentUser = fAuth2.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext() , MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        SignUpName = findViewById(R.id.name);
        SignUpEmail = findViewById(R.id.email);
        SignUpPassword = findViewById(R.id.password);
        TextView toLogin = findViewById(R.id.click_here_to_login);
        progressBar = findViewById(R.id.progressBar);
        fAuth2 = FirebaseAuth.getInstance();
        fStore2 = FirebaseFirestore.getInstance();
        toLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
            }
        });
        signUp = findViewById(R.id.btn_register);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email = SignUpEmail.getText().toString().trim();
                String password = SignUpPassword.getText().toString().trim();
                String name = SignUpName.getText().toString().trim();
                fAuth2.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isComplete()) {
                            uid = Objects.requireNonNull(fAuth2.getCurrentUser()).getUid();
                            DocumentReference documentReference = fStore2.collection("users").document(uid);
                            Map<String, Object> user = new HashMap<>();
                            user.put("email", email);
                            user.put("name", name);
                            documentReference.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(Register.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                }
                            });
                        } else {
                            Toast.makeText(Register.this, "Something happened", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }
}
