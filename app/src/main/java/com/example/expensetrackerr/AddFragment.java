package com.example.expensetrackerr;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.text.TextUtils;

import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.Calendar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private LinearLayout transactionContainer;
    private Button sendSmsButton;

    private String[] incomeCategories = {"Salary", "Gifts", "Refunds"};
    private String[] expenseCategories = {"Food", "Travel", "Bills", "Shopping", "Vehicle", "Lifestyle", "Others"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        // Existing initializations
        amountEditText = view.findViewById(R.id.amountEditText);
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        addButton = view.findViewById(R.id.addButton);
        datePickerButton = view.findViewById(R.id.datePickerButton);
        transactionContainer = view.findViewById(R.id.transactionContainer);
        sendSmsButton = view.findViewById(R.id.sendSmsButton);  // New SMS button
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        createNotificationChannel();

        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateCategorySpinner());
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());
        addButton.setOnClickListener(v -> {
            applyFadeInAnimation(addButton);
            addTransaction();
        });
        sendSmsButton.setOnClickListener(v -> sendSmsWithLatestTransaction());  // Set click listener for SMS button
        updateCategorySpinner();
        loadTransactions();

        return view;
    }

    private void sendSmsWithLatestTransaction() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        } else {
            sendSms();
        }
    }

    private void sendSms() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("expenses").whereEqualTo("uid", uid).orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            String amount = document.getString("amount");
                            String category = document.getString("category");
                            long timestamp = document.getLong("timestamp");

                            String message = String.format("Amount: %s\nCategory: %s\nDate: %s",
                                    amount,
                                    category,
                                    android.text.format.DateFormat.format("dd/MM/yyyy", new java.util.Date(timestamp)));

                            String phoneNumber = "8903995382"; // Replace with the actual phone number

                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                            Toast.makeText(getContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSms();
            } else {
                Toast.makeText(getContext(), "Permission denied to send SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Existing methods

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
                        loadTransactions();
                        showNotification(amount, category);
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error adding transaction", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadTransactions() {
        transactionContainer.removeAllViews();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("expenses").whereEqualTo("uid", uid).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            addTransactionCard(document);
                        }
                    });
        }
    }

    private void addTransactionCard(DocumentSnapshot document) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, transactionContainer, false);

        TextView transactionAmountTextView = cardView.findViewById(R.id.transactionAmountTextView);
        TextView transactionDateTextView = cardView.findViewById(R.id.transactionDateTextView);
        ImageView categoryIconImageView = cardView.findViewById(R.id.categoryIconImageView);
        ImageView editTransactionImageView = cardView.findViewById(R.id.editTransactionImageView);
        ImageView deleteTransactionImageView = cardView.findViewById(R.id.deleteTransactionImageView);

        String amount = document.getString("amount");
        String category = document.getString("category");
        long timestamp = document.getLong("timestamp");
        String documentId = document.getId();

        transactionAmountTextView.setText("₹" + amount);
        transactionDateTextView.setText(android.text.format.DateFormat.format("dd/MM/yyyy", new java.util.Date(timestamp)));

        // Set category icon based on the category
        switch (category) {
            case "Salary":
                categoryIconImageView.setImageResource(R.drawable.ic_salary); // Use appropriate drawable resource
                break;
            case "Gifts":
                categoryIconImageView.setImageResource(R.drawable.ic_gifts);
                break;
            case "Refunds":
                categoryIconImageView.setImageResource(R.drawable.ic_refunds);
                break;
            case "Food":
                categoryIconImageView.setImageResource(R.drawable.ic_food);
                break;
            case "Travel":
                categoryIconImageView.setImageResource(R.drawable.ic_travel);
                break;
            case "Bills":
                categoryIconImageView.setImageResource(R.drawable.ic_bills);
                break;
            case "Shopping":
                categoryIconImageView.setImageResource(R.drawable.ic_shopping);
                break;
            case "Vehicle":
                categoryIconImageView.setImageResource(R.drawable.ic_vehicle);
                break;
            case "Lifestyle":
                categoryIconImageView.setImageResource(R.drawable.ic_lifestyle);
                break;
            default:
                categoryIconImageView.setImageResource(R.drawable.ic_others);
                break;
        }

        // Edit button click listener
        editTransactionImageView.setOnClickListener(v -> showEditTransactionDialog(amount, category, documentId));

        // Delete button click listener
        deleteTransactionImageView.setOnClickListener(v -> showDeleteTransactionDialog(documentId));

        transactionContainer.addView(cardView);
    }

    private void showEditTransactionDialog(String amount, String category, String documentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Transaction");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_transaction, null);
        builder.setView(view);

        EditText editAmountEditText = view.findViewById(R.id.editAmountEditText);
        Spinner editCategorySpinner = view.findViewById(R.id.editCategorySpinner);
        Button updateButton = view.findViewById(R.id.updateButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        editAmountEditText.setText(amount);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, category.split(","));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategorySpinner.setAdapter(adapter);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        updateButton.setOnClickListener(v -> {
            String newAmount = editAmountEditText.getText().toString().trim();
            String newCategory = editCategorySpinner.getSelectedItem().toString();

            if (TextUtils.isEmpty(newAmount)) {
                Toast.makeText(getContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updatedTransaction = new HashMap<>();
            updatedTransaction.put("amount", newAmount);
            updatedTransaction.put("category", newCategory);

            db.collection("expenses").document(documentId)
                    .update(updatedTransaction)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Transaction updated", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                        loadTransactions();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update transaction", Toast.LENGTH_SHORT).show());
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());
    }

    private void showDeleteTransactionDialog(String documentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Transaction");
        builder.setMessage("Are you sure you want to delete this transaction?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            db.collection("expenses").document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                        loadTransactions();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete transaction", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void applyFadeInAnimation(View view) {
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_animation);
        view.startAnimation(fadeInAnimation);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Expense Tracker Notifications";
            String description = "Notifications for new transactions in the expense tracker";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String amount, String category) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_income)
                .setContentTitle("New Transaction Added")
                .setContentText(String.format("Amount: %s, Category: %s", amount, category))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }
}
