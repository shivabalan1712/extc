package com.example.expensetrackerr;

import static java.lang.String.*;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class AnalyticsFragment extends Fragment {

    private TextView totalIncomeTextView, totalExpenditureTextView, netSavingsTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        totalIncomeTextView = view.findViewById(R.id.totalIncomeTextView);
        totalExpenditureTextView = view.findViewById(R.id.totalExpenditureTextView);
        netSavingsTextView = view.findViewById(R.id.netSavingsTextView);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadAnalyticsData();

        return view;
    }

    private void loadAnalyticsData() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("expenses").whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(this::onComplete);
        }
    }

    private void onComplete(Task<QuerySnapshot> task) {
        if (task.isSuccessful()) {
            float income = 0;
            float expenditure = 0;
            for (QueryDocumentSnapshot document : task.getResult()) {
                float amount = Float.parseFloat(document.getString("amount"));
                if (Objects.equals(document.getString("type"), "Income")) {
                    income += amount;
                } else if (Objects.equals(document.getString("type"), "Expenditure")) {
                    expenditure += amount;
                }
            }

            totalIncomeTextView.setText(format("Total Income: $%.2f", income));
            totalExpenditureTextView.setText(format("Total Expenditure: $%.2f", expenditure));
            netSavingsTextView.setText(format("Net Savings: $%.2f", income - expenditure));
        }
    }
}
