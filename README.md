# DropboxClient

Android app for managing files on Dropbox. Built with Java and Material Design 3.

## Features

- Login with Dropbox OAuth2
- Browse files and folders
- Upload files from device
- Download files to phone
- Preview images, videos, and audio files
- Rename and delete files
- Share files via link
- Search files
- Dark mode
- Sort by name, date, or size

## Requirements

- Android 8.0 (API 26) or higher
- Dropbox account
- Dropbox App Key

## Setup

1. Clone the repo
2. Get your Dropbox App Key from https://www.dropbox.com/developers/apps
3. Open `AuthActivity.java` and replace the APP_KEY
4. Open `AndroidManifest.xml` and replace `db-xxnncso2v6rq3ml` with your app key scheme
5. Build and run

## Tech Stack

- Java
- Material Design 3
- Dropbox Core SDK 5.4.0
- RecyclerView with DiffUtil
- ExecutorService for async operations
- SharedPreferences for settings

## Project Structure

```
app/src/main/java/vn/edu/usth/dropboxclient/
├── activities/          # Main screens
├── adapters/           # RecyclerView adapters
├── fragments/          # Bottom sheets
├── models/            # Data models
└── utils/             # Helper classes
```

## Known Issues

- Sort preference saves but doesn't apply to file list
- No offline mode yet
- Search only works locally

## License

This is a student project for educational purposes.
