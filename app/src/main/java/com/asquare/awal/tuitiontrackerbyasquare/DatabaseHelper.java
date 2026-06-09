package com.asquare.awal.tuitiontrackerbyasquare;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TuitionTracker.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_CLASSES = "classes";
    public static final String TABLE_ATTENDANCE = "attendance";

    // Common column names
    public static final String KEY_ID = "id";

    // CLASSES Table columns
    public static final String KEY_CLASS_NAME = "name";

    // ATTENDANCE Table columns
    public static final String KEY_CLASS_REF_ID = "class_id";
    public static final String KEY_DATE = "date"; // Expected format: YYYY-MM-DD

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CLASSES_TABLE = "CREATE TABLE " + TABLE_CLASSES + " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_CLASS_NAME + " TEXT NOT NULL);";

        String CREATE_ATTENDANCE_TABLE = "CREATE TABLE " + TABLE_ATTENDANCE + " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_CLASS_REF_ID + " INTEGER, " +
                KEY_DATE + " TEXT NOT NULL, " +
                "FOREIGN KEY(" + KEY_CLASS_REF_ID + ") REFERENCES " + TABLE_CLASSES + "(" + KEY_ID + ") ON DELETE CASCADE);";

        db.execSQL(CREATE_CLASSES_TABLE);
        db.execSQL(CREATE_ATTENDANCE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSES);
        onCreate(db);
    }

    // Insert a new Class
    public void addClass(String className) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CLASS_NAME, className);
        db.insert(TABLE_CLASSES, null, values);
        db.close();
    }

    // Load all classes into the synchronized UI collection matrices
    public void getAllClasses(ArrayList<String> classList, ArrayList<Integer> classIds) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CLASSES, null);

        if (cursor.moveToFirst()) {
            do {
                classIds.add(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                classList.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CLASS_NAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
    }

    // BACKUP EXTRACTOR: Converts all internal relational data tables into a single structural JSON Array
    private JSONArray getDatabaseAsJsonArray() throws Exception {
        JSONArray mainArray = new JSONArray();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor classCursor = db.rawQuery("SELECT * FROM " + TABLE_CLASSES, null);
        if (classCursor.moveToFirst()) {
            do {
                JSONObject classObject = new JSONObject();
                int classId = classCursor.getInt(classCursor.getColumnIndexOrThrow(KEY_ID));
                String className = classCursor.getString(classCursor.getColumnIndexOrThrow(KEY_CLASS_NAME));

                classObject.put("class_id", classId);
                classObject.put("class_name", className);

                // Fetch nested attendance logs for this specific entity block
                JSONArray attendanceArray = new JSONArray();
                Cursor attCursor = db.rawQuery("SELECT * FROM " + TABLE_ATTENDANCE + " WHERE " + KEY_CLASS_REF_ID + " = ?", new String[]{String.valueOf(classId)});
                if (attCursor.moveToFirst()) {
                    do {
                        attendanceArray.put(attCursor.getString(attCursor.getColumnIndexOrThrow(KEY_DATE)));
                    } while (attCursor.moveToNext());
                }
                attCursor.close();

                classObject.put("attendance_dates", attendanceArray);
                mainArray.put(classObject);
            } while (classCursor.moveToNext());
        }
        classCursor.close();
        db.close();
        return mainArray;
    }

    // SAF Exporter Stream Execution Engine
    public void exportDatabaseToBackup(Context context, Uri uri) {
        try {
            JSONArray backupData = getDatabaseAsJsonArray();
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                fos.write(backupData.toString(4).getBytes()); // Indented formatting for clean structural storage
                fos.close();
                pfd.close();
                Toast.makeText(context, "Backup Saved Successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Backup Failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // SAF Restorer Stream Processing Engine
    public boolean importDatabaseFromBackup(Context context, Uri uri) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return false;

            FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            fis.close();
            pfd.close();

            JSONArray jsonArray = new JSONArray(sb.toString());

            SQLiteDatabase db = this.getWritableDatabase();
            // Clear existing tables before structural overwrite recovery operations
            db.execSQL("DELETE FROM " + TABLE_ATTENDANCE);
            db.execSQL("DELETE FROM " + TABLE_CLASSES);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject classObj = jsonArray.getJSONObject(i);
                String className = classObj.getString("class_name");

                ContentValues classValues = new ContentValues();
                classValues.put(KEY_CLASS_NAME, className);
                long newClassId = db.insert(TABLE_CLASSES, null, classValues);

                JSONArray attendanceArray = classObj.getJSONArray("attendance_dates");
                for (int j = 0; j < attendanceArray.length(); j++) {
                    String dateStr = attendanceArray.getString(j);
                    ContentValues attValues = new ContentValues();
                    attValues.put(KEY_CLASS_REF_ID, newClassId);
                    attValues.put(KEY_DATE, dateStr);
                    db.insert(TABLE_ATTENDANCE, null, attValues);
                }
            }
            db.close();
            Toast.makeText(context, "Data Restored Successfully!", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Restore Failed: Structural JSON match error.", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    // --- ADD THESE THREE METHODS TO YOUR EXISTING DatabaseHelper.java ---
    public boolean isDateMarked(int classId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_ATTENDANCE + " WHERE " + KEY_CLASS_REF_ID + "=? AND " + KEY_DATE + "=?", new String[]{String.valueOf(classId), date});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public void markDate(int classId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CLASS_REF_ID, classId);
        values.put(KEY_DATE, date);
        db.insert(TABLE_ATTENDANCE, null, values);
        db.close();
    }

    public void unmarkDate(int classId, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ATTENDANCE, KEY_CLASS_REF_ID + "=? AND " + KEY_DATE + "=?", new String[]{String.valueOf(classId), date});
        db.close();
    }
}