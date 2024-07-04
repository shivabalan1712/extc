package com.example.expensetrackerr;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddFragment extends Fragment {

    private static final String CHANNEL_ID = "expense_tracker_notifications";

    private EditText amountEditText;
    private RadioGroup typeRadioGroup;
    private Spinner categorySpinner;
    private Button addButton;
    private Button datePickerButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private long selectedDate;

    private String[] incomeCategories = {"Salary", "Gifts", "Refunds"};
    private String[] expenseCategories = {"Food", "Travel", "Bills", "Shopping", "Vehicle", "Lifestyle", "Others"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        amountEditText = view.findViewById(R.id.amountEditText);
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        addButton = view.findViewById(R.id.addButton);
        datePickerButton = view.findViewById(R.id.datePickerButton);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        createNotificationChannel();

        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateCategorySpinner());
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());
        addButton.setOnClickListener(v -> addTransaction());

        // Initialize the spinner with income categories by default
        updateCategorySpinner();

        return view;
    }

    private void updateCategorySpinner() {
        int selectedTypeId = typeRadioGroup.getCheckedRadioButtonId();
        String[] categories = selectedTypeId == R.id.incomeRadioButton ? incomeCategories : expenseCategories;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDate = calendar.getTimeInMillis();
            datePickerButton.setText(String.format("%d/%d/%d", dayOfMonth, month + 1, year));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void addTransaction() {
        if (auth.getCurrentUser() != null) {
            String amount = amountEditText.getText().toString().trim();
            String category = categorySpinner.getSelectedItem().toString();
            int selectedTypeId = typeRadioGroup.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(amount) || selectedTypeId == -1 || selectedDate == 0) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = selectedTypeId == R.id.incomeRadioButton ? "Income" : "Expenditure";
            String uid = auth.getCurrentUser().getUid();

            Map<String, Object> transaction = new HashMap<>();
            transaction.put("uid", uid);
            transaction.put("amount", amount);
            transaction.put("category", category);
            transaction.put("type", type);
            transaction.put("timestamp", selectedDate);

            db.collection("expenses")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Transaction added", Toast.LENGTH_SHORT).show();
                        amountEditText.setText("");
                        typeRadioGroup.clearCheck();
                        updateCategorySpinner(); // Reset spinner to default state
                        datePickerButton.setText("Select Date");
                        selectedDate = 0;

                        // Show notification if the transaction is income
                        if ("Income".equals(type)) {
                            showNotification(amount, category);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error adding transaction", Toast.LENGTH_SHORT).show());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Expense Tracker Notifications";
            String description = "Notifications for new transactions in the expense tracker";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(getContext(), NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String amount, String category) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_income) // Replace with your own icon
                .setContentTitle("New Income Added")
                .setContentText(String.format("Amount: %s, Category: %s", amount, category))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        notificationManager.notify(1, builder.build());
    }
}
