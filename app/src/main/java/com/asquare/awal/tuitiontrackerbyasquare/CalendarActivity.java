package com.asquare.awal.tuitiontrackerbyasquare;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private int classId;
    private String className;
    private DatabaseHelper dbHelper;

    private TextView txtMonthYear;
    private GridView calendarGridView;
    private Calendar currentCalendarContext;

    private ArrayList<Date> dayCells;
    private CalendarCellsAdapter cellsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        dbHelper = new DatabaseHelper(this);

        // Capture configurations routed from list interface selections
        classId = getIntent().getIntExtra("CLASS_ID", -1);
        className = getIntent().getStringExtra("CLASS_NAME");
        TextView cn = (TextView) findViewById(R.id.class_name);
        cn.setText(className);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(className + " - Log");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        txtMonthYear = findViewById(R.id.txtMonthYear);
        calendarGridView = findViewById(R.id.calendarGridView);
        Button btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        Button btnNextMonth = findViewById(R.id.btnNextMonth);

        currentCalendarContext = Calendar.getInstance();
        dayCells = new ArrayList<>();

        cellsAdapter = new CalendarCellsAdapter(dayCells, getApplicationContext());
        calendarGridView.setAdapter(cellsAdapter);

        updateCalendarGrid();

        btnPreviousMonth.setOnClickListener(v -> {
            currentCalendarContext.add(Calendar.MONTH, -1);
            updateCalendarGrid();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendarContext.add(Calendar.MONTH, 1);
            updateCalendarGrid();
        });

        // Trigger Click Actions on Grid Cell items
        calendarGridView.setOnItemClickListener((parent, view, position, id) -> {
            Date targetedDate = dayCells.get(position);

            // Check that user didn't click whitespace block offsets
            if (targetedDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateString = sdf.format(targetedDate);
                handleDateClick(dateString);
            }
        });
    }

    private void updateCalendarGrid() {
        dayCells.clear();
        Calendar tempCal = (Calendar) currentCalendarContext.clone();

        // Format Month Header Label
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        txtMonthYear.setText(monthYearFormat.format(tempCal.getTime()));

        // Position grid to day 1 of month to calculate grid-cell offset padding
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeekOffset = tempCal.get(Calendar.DAY_OF_WEEK) - 1;

        // Populate empty spaces for preceding days
        for (int i = 0; i < firstDayOfWeekOffset; i++) {
            dayCells.add(null);
        }

        // Fill array list with days of targeted tracking month iteration
        int totalDaysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        while (dayCells.size() < (totalDaysInMonth + firstDayOfWeekOffset)) {
            dayCells.add(tempCal.getTime());
            tempCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        cellsAdapter.notifyDataSetChanged();
    }

    private void handleDateClick(String selectedDateStr) {
        boolean isMarked = dbHelper.isDateMarked(classId, selectedDateStr);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Date Action: " + selectedDateStr);

        if (!isMarked) {
            builder.setMessage("Mark this class date as taken?").setPositiveButton("Mark as Taken", (dialog, which) -> {
                dbHelper.markDate(classId, selectedDateStr);
                updateCalendarGrid();
                Toast.makeText(this, "Class Logged!", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel", null);
        } else {
            builder.setMessage("This class date is already marked as taken.").setPositiveButton("Unmark Date", (dialog, which) -> {
                // Launch downstream explicit deletion confirmation challenge
                triggerUnmarkSafetyVerification(selectedDateStr);
            }).setNegativeButton("Close", null);
        }
        builder.show();
    }

    private void triggerUnmarkSafetyVerification(String targetDateStr) {
        new AlertDialog.Builder(this).setTitle("⚠️ Warning Confirmation Required").setMessage("Are you entirely sure you want to unmark " + targetDateStr + "? This record will be erased.").setPositiveButton("Yes, Unmark Record", (dialog, which) -> {
            dbHelper.unmarkDate(classId, targetDateStr);
            updateCalendarGrid();
            Toast.makeText(this, "Record cleared.", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Cancel Operation", null).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // --- INNER SYSTEM INFRASTRUCTURE: GRID VIEW CELL ADAPTER ---
    private class CalendarCellsAdapter extends BaseAdapter {
        private final ArrayList<Date> days;
        private final Context context;
        private final LayoutInflater inflater;

        private final Calendar calendar;


        public CalendarCellsAdapter(ArrayList<Date> days, Context context) {
            this.days = days;
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.calendar = Calendar.getInstance();
        }

        @Override
        public int getCount() {
            return days.size();
        }

        @Override
        public Object getItem(int position) {
            return days.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                // 3. Reuse the class-level inflater here safely
                convertView = inflater.inflate(R.layout.square_layout, parent, false);

                holder = new ViewHolder();
                holder.textView = convertView.findViewById(R.id.grid_item_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

//            TextView cellView = (TextView) convertView;
//            if (cellView == null) {
//                cellView = new TextView(CalendarActivity.this);
//                cellView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 140)); // Cell aspect height bounds block
//                cellView.setGravity(android.view.Gravity.CENTER);
//                cellView.setTextSize(16);
//
////                // Fetch current layout parameters and update height to match width
////                ViewGroup.LayoutParams params = cellView.getLayoutParams();
////                params.height = cellView.getWidth();
////                cellView.setLayoutParams(params);
//            }

            Date dateEntity = days.get(position);
            if (dateEntity != null) {

                calendar.setTime(dateEntity);
                String dateStr=String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
                holder.textView.setText(dateStr);


                // Check Database to evaluate if date should show marked highlight status tints
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                if (dbHelper.isDateMarked(classId, sdf.format(dateEntity))) {
                    //cellView.setBackgroundColor(Color.parseColor("#81C784")); // Light structural pastel green
                    holder.textView.setTextColor(Color.WHITE);
                    //cellView.setTextColor(Color.BLACK);
                    holder.textView.setBackgroundResource(R.drawable.bg_circle);
                } else {
                    //cellView.setBackgroundColor(Color.TRANSPARENT);
                    holder.textView.setTextColor(Color.BLACK);
                    holder.textView.setBackgroundResource(R.drawable.bg_normal);
                }
            } else {
                holder.textView.setText("");
                holder.textView.setBackgroundColor(Color.TRANSPARENT);
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView textView;
    }
}