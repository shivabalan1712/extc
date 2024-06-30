package com.example.expensetrackerr;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddFragment extends Fragment {

     EditText amountEditText, descriptionEditText;
     RadioGroup typeRadioGroup;
     Button addButton;
     FirebaseFirestore db;
     FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        amountEditText = view.findViewById(R.id.amountEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup);
        addButton = view.findViewById(R.id.addButton);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        addButton.setOnClickListener(v -> addTransaction());

        return view;
    }

    private void addTransaction() {
        if (auth.getCurrentUser() != null) {
            String amount = amountEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            int selectedTypeId = typeRadioGroup.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(amount) || selectedTypeId == -1) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = selectedTypeId == R.id.incomeRadioButton ? "Income" : "Expenditure";
            String uid = auth.getCurrentUser().getUid();

            Map<String, Object> transaction = new HashMap<>();
            transaction.put("uid", uid);
            transaction.put("amount", amount);
            transaction.put("description", description);
            transaction.put("type", type);

            db.collection("expenses")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Transaction added", Toast.LENGTH_SHORT).show();
                        amountEditText.setText("");
                        descriptionEditText.setText("");
                        typeRadioGroup.clearCheck();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error adding transaction", Toast.LENGTH_SHORT).show());
        }
    }
}
