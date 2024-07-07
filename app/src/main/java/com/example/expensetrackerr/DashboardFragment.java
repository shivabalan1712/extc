package com.example.expensetrackerr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DashboardFragment extends Fragment {

    private PieChart pieChart;
    private ToggleButton toggleButton;
    private TextView totalIncomeTextView;
    private TextView totalExpenditureTextView;
    private TextView balanceTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        toggleButton = view.findViewById(R.id.toggleButton);
        totalIncomeTextView = view.findViewById(R.id.totalIncomeTextView);
        totalExpenditureTextView = view.findViewById(R.id.totalExpenditureTextView);
        balanceTextView = view.findViewById(R.id.balanceTextView);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadDashboardData();

        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            applyRotationAnimation(toggleButton);
            loadDashboardData();
        });
        return view;
    }

    private void loadDashboardData() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            boolean showIncome = toggleButton.isChecked();
            db.collection("expenses").whereEqualTo("uid", uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Map<String, Float> categoryAmountMap = new HashMap<>();
                            float totalIncome = 0;
                            float totalExpenditure = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                float amount = Float.parseFloat(Objects.requireNonNull(document.getString("amount")));
                                String category = document.getString("category");
                                String type = document.getString("type");

                                if (Objects.equals(type, "Income")) {
                                    totalIncome += amount;
                                } else {
                                    totalExpenditure += amount;
                                }

                                if (Objects.equals(type, showIncome ? "Income" : "Expenditure")) {
                                    categoryAmountMap.put(category, categoryAmountMap.getOrDefault(category, 0f) + amount);
                                }
                            }

                            float balance = totalIncome - totalExpenditure;

                            List<PieEntry> pieEntries = new ArrayList<>();
                            for (Map.Entry<String, Float> entry : categoryAmountMap.entrySet()) {
                                pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
                            }

                            PieDataSet pieDataSet = new PieDataSet(pieEntries, showIncome ? "Income" : "Expenditure");
                            pieDataSet.setColors(showIncome ? ColorTemplate.COLORFUL_COLORS : ColorTemplate.JOYFUL_COLORS
                            );
                            PieData pieData = new PieData(pieDataSet);
                            pieData.setValueFormatter(new PercentFormatter(pieChart));
                            pieChart.setData(pieData);
                            pieChart.setUsePercentValues(true);
                            pieChart.invalidate(); // refresh
                            pieChart.setCenterText(String.format("Total\nRs %.2f", showIncome ? totalIncome : totalExpenditure));

                            totalIncomeTextView.setText(String.format("Total Income: Rs %.2f", totalIncome));
                            totalExpenditureTextView.setText(String.format("Total Expenditure: Rs %.2f", totalExpenditure));
                            balanceTextView.setText(String.format("Balance: Rs %.2f", balance));
                        }
                    });
        }
    }

    private void applyRotationAnimation(ToggleButton button) {
        Animation rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_animation);
        button.startAnimation(rotateAnimation);
    }
}
