package com.asquare.awal.tuitiontrackerbyasquare;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ArrayList<String> classList;
    private ArrayList<Integer> classIds;
    private ArrayAdapter<String> adapter;
    private ListView listViewClasses;

    // Launchers for Backup and Restore System file pickers
    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        listViewClasses = findViewById(R.id.listViewClasses);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        FloatingActionButton fabBackup = findViewById(R.id.fabBackup);
        FloatingActionButton fabRestore = findViewById(R.id.fabRestore);

        classList = new ArrayList<>();
        classIds = new ArrayList<>();

        // Simple built-in layout for listing strings. Swap with custom item if needed.
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classList);
        listViewClasses.setAdapter(adapter);

        // Register Activity Results for Storage Pickers
        initStorageLaunchers();

        // Load data from DB
        loadClasses();

        // Item Click: Open Calendar View for that Class
        listViewClasses.setOnItemClickListener((parent, view, position, id) -> {
            int selectedClassId = classIds.get(position);
            String selectedClassName = classList.get(position);

            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            intent.putExtra("CLASS_ID", selectedClassId);
            intent.putExtra("CLASS_NAME", selectedClassName);
            startActivity(intent);
        });

        // Add Tuition Class Button
        fabAdd.setOnClickListener(v -> showAddClassDialog());

        // Backup and Restore Actions
        fabBackup.setOnClickListener(v -> triggerBackupFileCreation());
        fabRestore.setOnClickListener(v -> triggerRestoreFilePicker());
    }

    private void loadClasses() {
        classList.clear();
        classIds.clear();
        // Assuming dbHelper has a method fetching all classes
        dbHelper.getAllClasses(classList, classIds);
        adapter.notifyDataSetChanged();
    }

    private void showAddClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Tuition/Class");

        final EditText input = new EditText(this);
        input.setHint("e.g., Rahim - Math Class");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String className = input.getText().toString().trim();
            if (!className.isEmpty()) {
                dbHelper.addClass(className);
                loadClasses();
                Toast.makeText(this, "Class Added!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void initStorageLaunchers() {
        // Handle saving backup payload
        backupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            dbHelper.exportDatabaseToBackup(this, uri);
                        }
                    }
                });

        // Handle reading backup payload
        restoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            if (dbHelper.importDatabaseFromBackup(this, uri)) {
                                loadClasses(); // Refresh data layout after update
                            }
                        }
                    }
                });
    }

    private void triggerBackupFileCreation() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "tuition_tracker_backup.json");
        backupLauncher.launch(intent);
    }

    private void triggerRestoreFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        restoreLauncher.launch(intent);
    }
}