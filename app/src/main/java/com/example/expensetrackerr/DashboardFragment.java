package com.example.expensetrackerr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DashboardFragment extends Fragment {

     PieChart pieChart;
     FirebaseFirestore db;
     FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadChartData();

        return view;
    }

    private void loadChartData() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("expenses").whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
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

                            List<PieEntry> entries = new ArrayList<>();
                            entries.add(new PieEntry(income, "Income"));
                            entries.add(new PieEntry(expenditure, "Expenditure"));

                            PieDataSet dataSet = new PieDataSet(entries, "Financial Summary");
                            dataSet.setColors(Color.GREEN, Color.RED);

                            PieData data = new PieData(dataSet);
                            pieChart.setData(data);
                            pieChart.invalidate(); // refresh
                        }
                    });
        }
    }
}
