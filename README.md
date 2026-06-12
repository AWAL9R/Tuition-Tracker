# Tuition Tracker 🗓️✍️

A lightweight, robust, and privacy-focused Android application written in native Java to log, manage, and audit tuition schedules and attendance records across multiple months.

The application uses SQLite for relational database storage and leverages Android’s modern Storage Access Framework (SAF) to safely run complete data imports and exports without requiring invasive device runtime permissions.

---

## ✨ Features

- **Class Dashboard**: Create and list multiple tuition paths, private students, or institution schedules in a clean, linear layout.
- **Dynamic Calendar View**: Access logs for any current, past, or future month with fluid navigation buttons.
- **Transactional Verification Actions**:
    - *Mark as Taken*: Instant tap assignment to log a finished session with visual pastel-green tile highlights.
    - *Unmark Safety Challenge*: Guardrails requiring multistep explicit authorization confirmations to prevent accidental log erasures.
- **Robust Storage Interactivity**: Secure, human-readable structural JSON database backups with full offline compatibility.

---

## 🏗️ Application Architecture

The application is structured into three main layers to guarantee high stability and maintain zero external library dependencies (aside from standard AndroidX and Material Components).

- **View Layer (Activities & XML)**: Coordinates user inputs from the dashboard and grid cell taps, shifting states dynamically.
- **Controller Layer (Adapters & Handlers)**: Translates dates into dynamic month view positions and safely flags confirmations before altering state.
- **Storage Layer (SQLite & JSON Engines)**: Handles local tabular insertions, cascade updates, and converts database tracks into serializable JSON byte streams.

### 1. Data Schema
The local database uses an enforced cascading relational schema over two primary tables:

- **Classes Table**: Holds specific entity designations (Unique IDs and Student/Class labels).
- **Attendance Table**: Maps date strings directly against targeted class indexes (formatted as YYYY-MM-DD). A cascade rule triggers automatically to sweep and wipe orphan timestamps if a root class entry is deleted.

### 2. Permissions & Security
This application complies with modern Android sandboxing standards. It requests **zero runtime permissions**. Legally restricted storage calls are entirely bypassed by leveraging native intent system calls to hand off backup file selection handling directly to the native OS folder container.

---

## 🚀 Workspace Installation Checklist

To distribute this project safely into your local Android Studio directory tree, place your written files into the following structural resource paths:

1. **Manifest Blueprint** ➡️ `/app/src/main/AndroidManifest.xml`
2. **Main Screen UI View** ➡️ `/app/src/main/res/layout/activity_main.xml`
3. **Calendar UI View** ➡️ `/app/src/main/res/layout/activity_calendar.xml`
4. **Main Screen Logic Class** ➡️ `/app/src/main/java/com/asquare/awal/tuitiontrackerbyasquare/MainActivity.java`
5. **Calendar Logic Class** ➡️ `/app/src/main/java/com/asquare/awal/tuitiontrackerbyasquare/CalendarActivity.java`
6. **SQLite Storage Engine Handler** ➡️ `/app/src/main/java/com/asquare/awal/tuitiontrackerbyasquare/DatabaseHelper.java`

---

## 🛠️ Usage Guide

### 1. Managing Classes
1. On opening the app, tap the primary **Add (+)** floating action button at the bottom right.
2. Provide a descriptive label inside the dialog prompt and save.
3. Tap on any item in the list view to navigate directly into its dedicated log dashboard.

### 2. Logging Days
- **Logging a class**: Click on any active numeric date tile within the current month view. Select the confirmation option to instantly commit the entry and highlight the cell.
- **Erasing an entry**: Click on a highlighted date tile. Tap to clear. A critical security warning alert dialog will appear; confirm your choice to remove the entry from the database.
- **Auditing past months**: Click the previous or next month navigation arrow headers to sweep historical records.

### 3. Backup and Restore Procedures
- **To Export Data**: Tap the **Save/Backup** icon button on the primary dashboard. The native system folder explorer will appear. Pick your destination path, rename the payload if needed, and confirm.
- **To Import Data**: Tap the **Upload/Restore** icon button on the primary dashboard. Select your previously exported text file. The app will validate structural integrity, clear out current tables, populate old entities, and refresh the dashboard layout immediately.

---

## 📄 License
This project is open-source and free to adapt, expand, or customize.