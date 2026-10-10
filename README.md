# ODAC26 Marathon Tracker

A native Android application built with Kotlin and Jetpack Compose, designed to streamline marathon management. It features CSV participant import, pre-race attendance tracking, synchronized category start timers, and a highly optimized QR finish-line scanner.

## Features

*   **Admin CSV Upload:** Easily batch-import runners from a Google Form CSV directly into the Firebase database. Includes a "Clear All Data" function for resetting the database between events.
*   **Attendance Management:** Mark runners as `present` before the race. Features a live-searchable list, one-way toggle switches (Absent -> Present), and an inline QR scanner for rapid check-ins.
*   **Synchronized Race Timers:** Trigger a global `startedAt` timestamp for all `present` runners in specific categories (e.g., 5K or 11K) simultaneously via Firestore Batch Writes.
*   **Rapid Finish-Line Scanner:** Uses CameraX and Google ML Kit to instantly scan runner QR codes at the finish line, recording their exact finish time and calculating duration. Supports manual jersey entry as a fallback and utilizes non-blocking Snackbars to allow for continuous scanning.
*   **Live Dashboard:** A real-time, auto-updating leaderboard powered by Kotlin Flows and Firestore snapshots. Groups runners into Accordion menus (Finished, Started, Absent) and ranks finished runners dynamically by duration.

## Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Database:** Firebase Firestore (NoSQL, Real-time updates)
*   **Camera/Scanning:** CameraX, Google ML Kit Barcode Scanning
*   **Architecture:** MVVM-lite (Repository Pattern + Flow state management)
*   **File Parsing:** OpenCSV

## Setup Instructions

### 1. Firebase Configuration
To run this application, you must connect it to a Firebase project.
1.  Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2.  Add an Android App to the project with the package name: `com.example.odac26`.
3.  Download the generated `google-services.json` file.
4.  Place the `google-services.json` file in the `app/` directory of this project.
5.  Enable **Firestore Database** in the Firebase Console and set up your initial security rules (Start in Test Mode for development).

### 2. CSV Import Format
When using the Admin Upload feature, ensure your CSV file contains at least the following column headers (case-insensitive):
*   `name`
*   `jersey`
*   `category` (Must contain the values `5k` or `11k`)
*   `email` (Optional)

*Note: A sample CSV file (`participants_draft.csv`) is provided in the project root directory for quick testing.*
*Note: The system uses the `jersey` number as the unique Firestore Document ID to enforce deduplication upon upload.*

### 3. QR Code Payload Format
The application's scanners are designed to parse JSON payloads embedded within the QR codes. The expected format is:
```json
{
  "name": "John Doe",
  "jersey": "1001"
}
```
*(The scanner will also gracefully fallback to using the raw string value if the QR code only contains the plain jersey number).*

## Future Development Notes

*   **Offline Mode:** Currently, the app relies on an active internet connection to communicate with Firestore. For marathons in remote areas, consider implementing Firestore's local persistence or a manual local-caching queue for finish-line scans.
*   **Authentication:** The app currently relies on the user physically possessing the device. Firebase Authentication (Email/Password or Google Sign-In) should be integrated to secure the Admin Upload and Race Timer actions.
*   **Security Rules:** Remember to update Firestore Security Rules before deploying to production to prevent unauthorized writes from outside the application.
