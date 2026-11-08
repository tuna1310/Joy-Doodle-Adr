# QUY TẮC TƯƠNG THÍCH THIẾT BỊ (DEVICE COMPATIBILITY) - DOODLE STUDIO

**Mục tiêu:** Ứng dụng hoạt động mượt mà, ổn định trên mọi thiết bị Android từ `minSdk 26` (Android 8.0 Oreo) trở lên, không phụ thuộc vào bất kỳ dòng máy cụ thể nào, hỗ trợ từ điện thoại cấu hình yếu (Android Go) đến máy tính bảng màn hình lớn.

---

## 1. NGUYÊN TẮC THIẾT KẾ CỐT LÕI

### 1.1. Layout co giãn & Đa kích thước màn hình
- **Không hardcode kích thước pixel:** Mọi kích thước giao diện dùng đơn vị `dp` hoặc tỷ lệ `weight`/`aspectRatio`.
- **Hỗ trợ điện thoại nhỏ và máy tính bảng:**
  - Trên màn hình nhỏ: Danh sách template tự động co giãn 2 cột.
  - Trên máy tính bảng / màn hình lớn: Hệ thống lưới Compose Grid tự động dàn trải dựa trên không gian sẵn có.
- **Xoay màn hình (Orientation Change):** Trạng thái vẽ và dữ liệu ViewModel được bảo toàn qua `rememberSaveable` và `ViewModel` (không bị mất trạng thái khi xoay màn hình).

### 1.2. Độ phân giải Canvas thích ứng theo RAM (Adaptive Resolution)
- **Cơ chế:** Tách thành hàm thuần `ResolutionPolicy.determineCanvasResolution(memoryClassInMb, isLowRamDevice)`:
  - Máy tiêu chuẩn (Heap > 192MB): Độ phân giải **1024x1024**.
  - Máy trung bình (Heap 97MB - 192MB): Hạ xuống **768x768**.
  - Máy cấu hình yếu / Android Go (`isLowRamDevice == true` hoặc Heap $\le$ 96MB): Hạ xuống **512x512**.
- Đã được kiểm thử 100% bằng Unit Test thuần (`ResolutionPolicyTest.kt`).

### 1.3. Cơ chế bắt lỗi OutOfMemoryError (OOM Safe Allocation)
- Khi cấp phát Bitmap cho các layer đồ họa, luôn bọc trong khối `try-catch (e: OutOfMemoryError)`.
- Khi xảy ra thiếu bộ nhớ:
  1. Thu hồi các Bitmap đệm không dùng.
  2. Tự động giảm một bậc độ phân giải (ví dụ từ 1024 xuống 768, hoặc 768 xuống 512).
  3. Hiển thị thông báo nhẹ nhàng (Snackbar/Toast) giải thích cho người dùng, tuyệt đối **không để crash app**.

### 1.4. Xuất Video an toàn (MediaCodecList & PNG Fallback - Áp dụng ở P5)
- Trước khi khởi tạo `MediaCodec`, kiểm tra danh sách codec phần cứng khả dụng qua `MediaCodecList(MediaCodecList.REGULAR_CODECS)`.
- Kiểm tra tính tương thích của kích thước xuất (1080x1080 hoặc 720x720) với giới hạn profile của chip.
- **Dự phòng an toàn (Fallback):** Nếu quá trình mã hóa video thất bại do lỗi phần cứng hoặc thiếu bộ nhớ, app tự động xuất tác phẩm dưới dạng ảnh PNG độ phân giải cao và thông báo lý do rõ ràng cho người dùng.

### 1.5. Lưu trữ hiện đại qua MediaStore & Kiểm tra dung lượng trống
- **Không dùng đường dẫn tệp tuyệt đối:** Mọi thao tác lưu ảnh PNG vào `Pictures/DoodleStudio` và video MP4 vào `Movies/DoodleStudio` đều thông qua Android `MediaStore API` và `FileProvider` (đáp ứng trọn vẹn Scoped Storage từ Android 10+).
- **Kiểm tra dung lượng trước khi ghi:** Sử dụng `StatFs` kiểm tra dung lượng bộ nhớ trống trên máy trước khi tiến hành render timelapse hoặc lưu file lớn. Nếu dung lượng trống $< 50\text{MB}$, cảnh báo người dùng giải phóng bộ nhớ trước.

### 1.6. Tương thích API theo phiên bản hệ điều hành
- App hỗ trợ từ `minSdk 26` đến `targetSdk 35` (Android 8.0 đến Android 15).
- Mọi hàm hoặc thuộc tính chỉ có trên các phiên bản Android mới hơn (ví dụ Android 10 Q, Android 13 Tiramisu, Android 14 UpsideDownCake) bắt buộc phải được bọc trong điều kiện:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { ... }
  ```

---

## 2. BẢNG THEO DÕI TƯƠNG THÍCH THIẾT BỊ

> **Ghi chú nghiệm thu:**
> Vì hiện tại **chưa có điện thoại thật** do người dùng cung cấp, mọi tính năng chạy trên runtime Android đều được gắn nhãn **"Đã code, chưa kiểm chứng chạy"** cho tới khi chạy thử trên thiết bị thật theo danh mục kiểm thử.

| Nguyên tắc | Hiện thực mã nguồn | Trạng thái nghiệm thu | Ghi chú kiểm chứng |
|---|---|---|---|
| **1. Layout co giãn & Xoay màn hình** | Compose Adaptive Scaffold, aspectRatio, ViewModel state preservation. | **Đã code, chưa kiểm chứng chạy** | Chờ máy thật để xoay ngang/dọc và test trên màn hình nhỏ. |
| **2. Độ phân giải thích ứng RAM** | `com.doodlestudio.app.util.ResolutionPolicy` | **Xong có log** | Đã pass 4 unit tests độc lập (`ResolutionPolicyTest`). |
| **3. Bắt OutOfMemoryError** | Chuẩn bị pattern trong Editor Canvas Engine (kích hoạt ở P2). | **Đã code, chưa kiểm chứng chạy** | Sẽ kiểm thử trong P2 khi tải tranh lớn. |
| **4. Kiểm tra MediaCodecList & Fallback** | Thiết kế kiến trúc video export (triển khai ở P5). | **Chưa làm** | Thực hiện tại Phase P5. |
| **5. Lưu trữ MediaStore & Kiểm tra dung lượng** | `MediaStore` URI insert & `StatFs` check (triển khai ở P4/P5). | **Chưa làm** | Thực hiện tại Phase P4. |
| **6. Kiểm tra Build.VERSION bọc API mới** | Tuân thủ toàn bộ code trong `app/`. | **Đã code, chưa kiểm chứng chạy** | Lint check sẽ kiểm tra tự động. |

