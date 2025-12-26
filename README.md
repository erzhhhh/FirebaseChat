# FirebaseChat

A simple real-time chat app powered by **Firebase**. The goal of the project is to practice building a lightweight messaging app without managing your own backend.

## ✨ Features

* Real-time messaging (messages appear instantly)
* User authentication with Firebase Auth
* Persistent chat history
* Clean, minimal UI
* Error + loading handling

## 🏗️ Tech stack

* **Kotlin / Java (Android)**
* **Firebase Authentication** — login & user management
* **Firebase Realtime Database / Firestore** — message storage & sync
* **RecyclerView** — messages list UI
* **ViewModel + LiveData** (if used) — state handling

## 🚀 Getting started

1. Clone this repo
2. Open it in Android Studio
3. Create a Firebase project at [https://console.firebase.google.com](https://console.firebase.google.com)
4. Add the Android app package name and download `google-services.json`
5. Put the file into the project under:

   ```
   app/google-services.json
   ```
6. Sync and run the project on a device/emulator

You should now be able to sign in and start chatting.

## 📂 Project structure (high level)

```
app/
 ├─ data/        # firebase and models
 ├─ ui/          # chat screens & adapters
 └─ viewmodel/   # presentation logic (if applicable)
```

## 💡 Possible extensions

* Typing indicator
* Image / file messages
* Push notifications (Firebase Cloud Messaging)
* Private chat rooms
* Dark mode

## 🎯 Why this project exists

To demonstrate how easy it is to build a working chat app using Firebase as a backend — fast setup, no servers to manage.

