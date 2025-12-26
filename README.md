# 🎥 VirtuCam - Virtual Camera for Android

**Replace your camera feed with custom videos in any app - No Root Required!**

[![Works with MochiCloner](https://img.shields.io/badge/MochiCloner-Compatible-green)](https://mochicloner.com)
[![Works with MetaWolf](https://img.shields.io/badge/MetaWolf-Compatible-blue)](#)
[![No Root](https://img.shields.io/badge/Root-Not%20Required-orange)](#)
[![Xposed](https://img.shields.io/badge/Xposed-Module-red)](#)

---

## 🌟 Features

✅ **No Root Required** - Works perfectly in MochiCloner/MetaWolf  
✅ **Simple Video Picker** - Just select your video, no manual file copying!  
✅ **Universal** - Works with Instagram, TikTok, Zoom, WhatsApp, Firefox, Chrome, etc.  
✅ **One Video for All Apps** - Select once, use everywhere  
✅ **Auto Test Pattern** - Shows colorful pattern when no video selected  
✅ **vcamsx Architecture** - Stable callback-level hooking  

---

## 📱 How It Works

```
1. Install VirtuCam APK
2. Select a video in the app (one time)
3. Enable VirtuCam in MochiCloner for target apps
4. Open camera in cloned app → Your video plays!
```

---

## 🚀 Quick Start (MochiCloner)

### Step 1: Install VirtuCam

```bash
adb install -r app-debug.apk
```

### Step 2: Select Your Video

1. Open **VirtuCam** app
2. Click **"📹 Select Video"**
3. Choose any MP4 video from your phone
4. Done! ✅

### Step 3: Clone Your App

1. Open **MochiCloner**
2. Create a clone of your target app (Instagram, TikTok, etc.)
3. Enable **VirtuCam** module for that clone
4. Restart the cloned app

### Step 4: Test!

1. Open your cloned app
2. Open camera (story, live, video call, etc.)
3. **Your video plays instead of real camera!** 🎉

---

## 📋 Detailed Setup

### Requirements

- Android 8.0+ (API 26+)
- **MochiCloner** or **MetaWolf** installed
- Any MP4 video file

### Installation

#### Option A: Pre-built APK
1. Download latest APK from [Releases](../../releases)
2. Install: `adb install VirtuCam.apk`

#### Option B: Build from Source
```bash
git clone https://github.com/Mynameiscoderpro/VCAMERA.git
cd VCAMERA
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Configuration

1. **Open VirtuCam App**
   - You'll see the status screen
   - Shows if module is active
   - Shows if video is loaded

2. **Select Video**
   - Click "📹 Select Video"
   - Pick any MP4 file
   - App automatically saves it to accessible location
   - No manual copying needed!

3. **Enable in MochiCloner**
   - Open MochiCloner
   - Select your cloned app
   - Go to Xposed Modules section
   - Enable **VirtuCam** ✅
   - Restart the clone

4. **Test**
   - Open camera in cloned app
   - Grant camera permission if asked
   - See your video playing!

---

## 🎯 Use Cases

### Social Media Live Streaming
- **Instagram Live** - Stream pre-recorded video as "live"
- **TikTok** - Use professional footage as your camera
- **YouTube** - Go live with edited content

### Video Calls
- **Zoom/Meet** - Use custom background videos
- **WhatsApp Video** - Fun video effects
- **Skype** - Professional presentations

### Multiple Accounts
- Clone app 3 times
- Different video for each clone
- Manage multiple personas easily

---

## 🔧 Troubleshooting

### "Module not detected"

**Solution:**
- ✅ Open MochiCloner
- ✅ Select your cloned app
- ✅ Check if VirtuCam is enabled in Xposed modules
- ✅ Restart the cloned app (not your device!)

### "Still showing real camera"

**Check Logcat:**
```bash
adb logcat | grep VirtuCam
```

**Expected output:**
```
VirtuCam: ✅ Hooked org.mozilla.firefox
VirtuCam: 📷 Camera opening
VirtuCam: 🎥 Video found: /data/data/virtual.camera.app/files/virtual_camera.mp4
VirtuCam: ✅ Video decoder started
VirtuCam: 🎬 Capture callback wrapped!
VirtuCam: 🎬 Frame 30 delivered
```

### "No video playing"

**Solution:**
- ✅ Make sure video is MP4 format
- ✅ Try selecting the video again in VirtuCam app
- ✅ Check video file size (keep under 100MB)
- ✅ Restart cloned app after selecting video

### "Test pattern shows instead of video"

**This means:**
- Module is working! ✅
- Video file not found/not selected
- Open VirtuCam app and select a video

---

## 📊 Architecture

### vcamsx Approach

VirtuCam uses the **vcamsx architecture**:

1. **Don't replace Surfaces** - Let apps create their own surfaces
2. **Hook CaptureCallback** - Wrap the camera callbacks instead
3. **Inject Frames** - Replace frames in `onCaptureCompleted()`
4. **Centralized Storage** - One video file for all apps

### File Structure

```
app/
├── xposed/
│   ├── VirtuCamXposed.kt       # Main Xposed hook
│   └── VideoDecoder.kt          # MediaCodec video decoder
├── ui/
│   └── MainActivity.kt          # Video picker UI
└── utils/
    └── VideoManager.kt          # Video storage manager
```

### Video Storage

```
/data/data/virtual.camera.app/files/virtual_camera.mp4
  ↑
  └── Accessible by all hooked apps via Xposed
```

---

## 🎬 Video Requirements

| Property | Requirement |
|----------|-------------|
| **Format** | MP4 (H.264 codec) |
| **Resolution** | 720p or 1080p recommended |
| **Size** | Under 100MB for best performance |
| **FPS** | 24-30 FPS |
| **Orientation** | Portrait or Landscape (auto-detected) |

---

## ❓ FAQ

**Q: Do I need root?**  
A: No! Works in MochiCloner/MetaWolf without root.

**Q: Will it work on my main device camera?**  
A: No, only in cloned apps. Your real camera is unaffected.

**Q: Can I use different videos for different apps?**  
A: Currently one video for all apps. Clone VirtuCam app multiple times if you need different videos.

**Q: Does it work with Instagram/TikTok?**  
A: Yes! Clone the app in MochiCloner and enable VirtuCam.

**Q: Why test pattern instead of my video?**  
A: Video not selected or file not accessible. Select video again in VirtuCam app.

---

## 🙏 Credits

- **vcamsx** - Original virtual camera concept
- **LSPosed** - Xposed framework
- **MochiCloner** - Virtual environment support

---

## 📄 License

MIT License - See [LICENSE](LICENSE) file

---

## ⚠️ Disclaimer

This tool is for educational purposes. Use responsibly and respect platform ToS.

---

**Made with ❤️ for the Android community**
