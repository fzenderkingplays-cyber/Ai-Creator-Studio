# AI Creator Studio v0.3 — English / Urdu + limited free generation backend

## What's included
- Native Kotlin Android UI with English/Urdu toggle, RTL Urdu, remembered language preference.
- Prompt, styles, text-to-image and text-to-video choices.
- Android HTTPS backend requests, image preview, and media saved in app-specific files.
- Flask backend with SQLite UTC daily limits: **3 image attempts and 1 video attempt per device ID per day** by default. Failed provider requests are refunded.
- Image provider adapter uses a configurable Hugging Face inference endpoint. **Actual free provider quota, model availability, and endpoint compatibility are not guaranteed**. You must supply a valid token and compatible endpoint.
- Video adapter uses a **configurable synchronous video provider endpoint** returning video/mp4 or video/webm. It is not functional until a compatible provider URL/token is configured. Many video APIs are asynchronous and require a different adapter.

## Backend setup
```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
export HF_TOKEN='YOUR_HUGGING_FACE_TOKEN'
# optional: export IMAGE_API_URL='https://your-compatible-image-endpoint'
# optional: export VIDEO_API_URL='https://your-compatible-video-endpoint'
# optional: export VIDEO_API_TOKEN='YOUR_VIDEO_TOKEN'
python server.py
```
Test: `curl http://127.0.0.1:8000/health`. Deploy backend behind HTTPS and change `backendUrl` in `MainActivity.kt` to your deployed HTTPS address. Do not embed provider tokens in the Android APK.

## Android build
Open project folder in Android Studio, use SDK 35 / JDK 17, sync and Build > Build APK(s). No Gradle wrapper is included. Not compiled or device-tested here.

## Important production security
Device IDs can be reset or spoofed; these limits are **demo limits, not strong abuse protection**. Before public launch implement authentication, account-bound quotas, IP throttling, bot protection, moderation, secure secret storage, logging, spending caps and provider-specific error handling. SQLite is suitable only for a small single-instance demo. Video endpoint currently requires a compatible synchronous binary-returning API. Image-to-video uploads, gallery export, payment, and job polling are not implemented.

## Build an APK from Android mobile (GitHub Actions)
1. Extract the ZIP. Upload the **contents** of `AI_Creator_Studio` (including `.github/workflows/build-apk.yml`) to the root of a new GitHub repository on the `main` branch. GitHub's web upload may hide dotfolders in some file pickers; use a Git client or create `.github/workflows/build-apk.yml` manually if needed.
2. On GitHub, open **Actions** > **Build Android APK** > **Run workflow** (or wait for the push build).
3. After a successful run, open its **Artifacts**, download `AI-Creator-Studio-debug-APK`, unzip it and install `app-debug.apk` on Android.
4. This is a debug APK, not a signed Play Store release. It has NOT been compiled or verified in this environment.
5. The Android app currently has a placeholder backend URL (`https://YOUR-SERVER.example.com`), so AI generation will NOT work until a real HTTPS backend and compatible provider are configured. Video generation may require a paid provider. Do not embed secret provider API keys in the APK.
