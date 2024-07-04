package com.example.expensetrackerr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AnalyticsFragment extends Fragment {

    private LineChart lineChart;
    private TextView cashFlowTextView, averageSpentTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        cashFlowTextView = view.findViewById(R.id.cashFlowTextView);
        averageSpentTextView = view.findViewById(R.id.averageSpentTextView);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadAnalyticsData();
        return view;
    }

    private void loadAnalyticsData() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("expenses").whereEqualTo("uid", uid).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            float totalIncome = 0;
                            float totalExpenditure = 0;
                            List<Entry> incomeEntries = new ArrayList<>();
                            List<Entry> expenditureEntries = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                float amount = Float.parseFloat(Objects.requireNonNull(document.getString("amount")));
                                Long timestampLong = document.getLong("timestamp");

                                if (timestampLong != null) {
                                    long timestamp = timestampLong;
                                    if (Objects.equals(document.getString("type"), "Income")) {
                                        totalIncome += amount;
                                        incomeEntries.add(new Entry(timestamp, amount));
                                    } else if (Objects.equals(document.getString("type"), "Expenditure")) {
                                        totalExpenditure += amount;
                                        expenditureEntries.add(new Entry(timestamp, amount));
                                    }
                                }
                            }
                            LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Income");
                            incomeDataSet.setColor(Color.GREEN);
                            LineDataSet expenditureDataSet = new LineDataSet(expenditureEntries, "Expenditure");
                            expenditureDataSet.setColor(Color.RED);

                            LineData lineData = new LineData(incomeDataSet, expenditureDataSet);
                            lineChart.setData(lineData);
                            lineChart.invalidate();

                            float averageSpent = totalExpenditure / incomeEntries.size();

                            cashFlowTextView.setText(String.format("Cash Flow: Rs %.2f", totalIncome - totalExpenditure));
                            averageSpentTextView.setText(String.format("Average Spent: Rs %.2f", averageSpent));
                        }
            });
        }
    }
}