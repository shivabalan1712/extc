package com.example.expensetrackerr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DashboardFragment extends Fragment {

    private PieChart pieChart;
    private ToggleButton toggleButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        toggleButton = view.findViewById(R.id.toggleButton);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadDashboardData();

        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> loadDashboardData());

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
                            List<PieEntry> pieEntries = new ArrayList<>();
                            float total = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (Objects.equals(document.getString("type"), showIncome ? "Income" : "Expenditure")) {
                                    float amount = Float.parseFloat(Objects.requireNonNull(document.getString("amount")));
                                    String category = document.getString("category");
                                    pieEntries.add(new PieEntry(amount, category));
                                    total += amount;
                                }
                            }

                            PieDataSet pieDataSet = new PieDataSet(pieEntries, showIncome ? "Income" : "Expenditure");
                            pieDataSet.setColors(Color.GREEN, Color.RED,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.YELLOW,Color.rgb(128,150,200));
                            PieData pieData = new PieData(pieDataSet);
                            pieData.setValueFormatter(new PercentFormatter(pieChart));
                            pieChart.setData(pieData);
                            pieChart.setUsePercentValues(true);
                            pieChart.invalidate(); // refresh
                            pieChart.setCenterText(String.format("Total\nRs %.2f", total));
                        }
                    });
        }
    }
}
