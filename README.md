# Doodle Studio (Android)

Ứng dụng tô màu và vẽ tự do nguyên bản, hỗ trợ xuất video phát lại timelapse và chia sẻ tác phẩm.

## 1. Yêu cầu hệ thống & Môi trường phát triển
- **Hệ điều hành:** Windows 10/11
- **JDK:** Java 17 (Microsoft Hotspot JDK 17.0.20)
- **Android SDK:** Compile SDK 35, Target SDK 35, Min SDK 26 (Android 8.0 Oreo)
- **Vị trí dự án:** `D:\DoodleStudio`

## 2. Hướng dẫn Build dự án an toàn bộ nhớ ổ C
Để bảo vệ dung lượng ổ đĩa C và tối ưu tốc độ build, toàn bộ cache Gradle được lưu trữ trên ổ **D:** (`D:\.gradle`).
Mỗi lệnh build cần truyền cờ `--gradle-user-home D:\.gradle`:

```powershell
# Chạy Unit Tests và sinh tranh line-art
.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest

# Biên dịch gói Debug APK
.\gradlew.bat --gradle-user-home D:\.gradle assembleDebug

# Chạy toàn bộ quá trình Clean, Build và Test
.\gradlew.bat --gradle-user-home D:\.gradle clean assembleDebug testDebugUnitTest
```

*(Hoặc thiết lập biến môi trường trong phiên PowerShell: `$env:GRADLE_USER_HOME="D:\.gradle"` trước khi chạy `.\gradlew.bat`)*

## 3. Kiến trúc dự án
- **Tech stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Material 3, Navigation Compose, Koin 4.0.0, Room 2.6.1, DataStore Preferences 1.1.1.
- **Engine đồ họa:**
  - Lõi tô màu: Deterministic Connected Components Label Map (`ShortArray` 1024x1024) + Morphological Dilation 1-2px + `BlendMode.SrcOver` khử răng cưa.
  - Lõi vẽ tự do: Vector Stroke engine (đường cong Bezier mượt).
- **Tranh mẫu:** 100% sinh bằng thuật toán toán học vector (xem `CREDITS.md`).

