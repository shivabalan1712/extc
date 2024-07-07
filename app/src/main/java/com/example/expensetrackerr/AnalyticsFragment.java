package com.example.expensetrackerr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AnalyticsFragment extends Fragment {

    private BarChart incomeBarChart;
    private BarChart expenseBarChart;
    private TextView cashFlowTextView, averageSpentTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Spinner timePeriodSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        incomeBarChart = view.findViewById(R.id.incomeBarChart);
        expenseBarChart = view.findViewById(R.id.expenseBarChart);
        cashFlowTextView = view.findViewById(R.id.cashFlowTextView);
        averageSpentTextView = view.findViewById(R.id.averageSpentTextView);
        timePeriodSpinner = view.findViewById(R.id.timePeriodSpinner);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set up the spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.time_period_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timePeriodSpinner.setAdapter(adapter);
        timePeriodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAnalyticsData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
                    Map<String, Float> incomeMap = new HashMap<>();
                    Map<String, Float> expenditureMap = new HashMap<>();

                    long startDate = getStartDate();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        float amount = Float.parseFloat(Objects.requireNonNull(document.getString("amount")));
                        Long timestampLong = document.getLong("timestamp");

                        if (timestampLong != null) {
                            long timestamp = timestampLong;
                            if (timestamp >= startDate) {
                                String category = document.getString("category");
                                if (category == null) category = "Other";
                                if (Objects.equals(document.getString("type"), "Income")) {
                                    totalIncome += amount;
                                    incomeMap.put(category, incomeMap.getOrDefault(category, 0f) + amount);
                                } else if (Objects.equals(document.getString("type"), "Expenditure")) {
                                    totalExpenditure += amount;
                                    expenditureMap.put(category, expenditureMap.getOrDefault(category, 0f) + amount);
                                }
                            }
                        }
                    }

                    setUpBarChart(incomeBarChart, incomeMap, "Income", Color.GREEN);
                    setUpBarChart(expenseBarChart, expenditureMap, "Expenditure", Color.RED);

                    float averageSpent = totalExpenditure / expenditureMap.size();

                    cashFlowTextView.setText(String.format("Cash Flow: Rs %.2f", totalIncome - totalExpenditure));
                    averageSpentTextView.setText(String.format("Average Spent: Rs %.2f", averageSpent));
                }
            });
        }
    }

    private void setUpBarChart(BarChart barChart, Map<String, Float> dataMap, String label, int color) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Float> entry : dataMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(color);
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());
        barChart.invalidate();
    }

    private long getStartDate() {
        String selectedPeriod = timePeriodSpinner.getSelectedItem().toString();
        Calendar calendar = Calendar.getInstance();
        switch (selectedPeriod) {
            case "Today":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "This Week":
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            case "This Month":
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                break;
            default:
                return 0;
        }
        return calendar.getTimeInMillis();
    }
}
