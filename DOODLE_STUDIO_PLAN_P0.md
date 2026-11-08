# KẾ HOẠCH TRIỂN KHAI: DOODLE STUDIO (ANDROID)

> Tên tạm thời. App tô màu và vẽ tự do, có phát lại timelapse và chia sẻ. Sản phẩm gốc cùng thể loại, không sao chép app nào.

---

## 0. Cách dùng tài liệu này với Antigravity

- Đây là tài liệu chuẩn cho P0. Không tự đổi kiến trúc nếu chưa nêu lý do và chờ duyệt.
- Chỉ dùng 3 nhãn trạng thái: **"Xong có log"**, **"Đã code, chưa kiểm chứng chạy"**, **"Chưa làm"**.
- Không nêu số liệu chưa đo (FPS, thời gian xuất video, độ mượt) như sự thật.
- Không chỉnh kỳ vọng test chỉ để test pass. Nếu phải đổi, giải thích lý do.
- Không làm tính năng ngoài danh sách.
- Ổ C gần đầy: không tạo file lớn trên C, không tải asset nặng. Hết dung lượng thì dừng và báo.
- Không có emulator. Kiểm chứng trên điện thoại thật khi người dùng cung cấp, ghi tên máy + phiên bản Android.
- Chuỗi giao diện đặt trong `strings.xml` (values, values-vi).

## 1. Mục tiêu và ranh giới

**Mục tiêu:** app tô màu và vẽ tự do, có phát lại thành video timelapse và chia sẻ.

**Ranh giới bản quyền (bắt buộc):**
- Tên, logo, giao diện, tranh và âm thanh đều tự làm.
- Tranh: vector tự vẽ bằng code, hoặc nguồn CC0/public domain (ghi nguồn trong `CREDITS.md`).
- Không dùng nhân vật phim, hoạt hình hay thương hiệu có bản quyền.
- Không tải, cài, giải nén, decompile hay bóc tài nguyên của bất kỳ app nào khác.
- Không mô phỏng giao diện của app khác từng chi tiết.

**Chỉ số thành công của bản đầu:** tô mượt, hoàn tác đúng, xuất được video MP4 mở được trên máy thật, chia sẻ được.

## 2. Tính năng

1. **Thư viện tranh:** nhóm theo chủ đề (động vật, hoa, đồ ăn, phong cảnh, mandala...), tối thiểu 40 tranh line-art, chia độ khó (ít / nhiều chi tiết).
2. **Tô màu:** chạm để đổ màu vùng, bảng màu + chọn màu tùy ý, tẩy, hoàn tác/làm lại (>= 30 bước), phóng to/thu nhỏ/kéo, ghim tranh yêu thích.
3. **Vẽ tự do:** canvas trắng, bút (độ dày, màu, độ mờ), tẩy, lớp nền, hoàn tác/làm lại.
4. **Phát lại:** ghi thao tác có timestamp, xuất timelapse MP4 (1080x1080 hoặc 9:16), nhạc nền nhẹ (CC0), watermark tên app.
5. **Chia sẻ:** lưu PNG và video vào Pictures/Movies, share sheet 1 chạm (FileProvider).
6. **Thư viện cá nhân:** danh sách tác phẩm đã lưu (Room), tiếp tục chỉnh sửa, xóa.
7. **Cài đặt:** âm thanh bật/tắt, ngôn ngữ EN/VI, giới thiệu, credits.

**Màn hình:** Splash, Onboarding, Home (chủ đề + gợi ý), Danh sách tranh, Editor (tô/vẽ), Kết quả (xem video, chia sẻ), Thư viện cá nhân, Cài đặt.

## 3. Tech stack

Kotlin, Jetpack Compose, MVVM + StateFlow, Room (tác phẩm), DataStore (cài đặt), Koin DI, Canvas/Path để vẽ, Media3 để phát video, MediaCodec/MediaMuxer để xuất MP4. minSdk 26.

## 4. Quyết định kiến trúc cốt lõi (chốt ở P0)

So sánh ba phương án cho engine tô màu:

| | A. Bitmap flood fill thuần | B. Vector regions | **C. Label map (đề xuất)** |
|---|---|---|---|
| Cách hoạt động | Chạm là lan màu theo pixel (scanline) | Tranh là tập vùng vector, tô = đổi màu vùng | Tính **một lần** bản đồ nhãn vùng từ line-art, tô = đổ màu cả vùng theo nhãn |
| Tốc độ tô | Chậm với vùng lớn | Tức thì | Tức thì sau khi tính nhãn |
| Viền răng cưa / hở màu | Dễ lỗi | Sạch | Sạch nếu nới màu dưới nét 1-2px |
| Hỗ trợ tranh PNG CC0 | Có | Không (phải tự dựng vùng) | **Có** |
| Hoàn tác | Tốn RAM nếu chụp cả bitmap | Rất nhẹ | **Nhẹ**: lưu (mã vùng, màu cũ, màu mới) |
| Timelapse | Phải ghi pixel | Dễ | **Dễ**: phát lại chuỗi lệnh |
| Rủi ro | Hết RAM, chậm | Tốn công làm art | Vùng bị rách nếu viền hở |

**Đề xuất: phương án C.** Tô tức thì; hoàn tác và timelapse đều thành phát lại chuỗi lệnh nhẹ; dùng được cả tranh raster CC0.

**Ba lớp khi vẽ (dưới lên):**
1. **Fill layer:** bitmap màu tô (độ phân giải làm việc 1024x1024, ARGB_8888, khoảng 4MB).
2. **Free-draw layer:** nét vẽ tay.
3. **Line-art layer:** nét viền đen gốc, luôn nằm trên cùng.

**Xử lý viền hở:** trước khi gán nhãn, nới nhẹ mask nét viền để bít khe hở nhỏ; vùng quá bé (nhiễu) gộp vào vùng lân cận.

**Giới hạn bộ nhớ:** tối đa khoảng 3 bitmap làm việc; hạ độ phân giải xuống 768 trên máy ít RAM; tái sử dụng bitmap; không giữ bản sao cả ảnh trong history.

## 5. Mô hình dữ liệu

```
Artwork(id, title, sourceType[template|blank], templateId?, createdAt, updatedAt, thumbnailPath, statePath)
Template(id, category, difficulty, assetPath, regionCountHint)
EditAction (sealed): Fill(regionId, color) | Stroke(points, color, width, alpha, tool) | Erase(...) | Undo | Redo
ActionLog(artworkId, List<TimestampedAction>)   // timestamp tính bằng ms từ lúc bắt đầu
```

- `ActionLog` là nguồn dữ liệu duy nhất cho: hoàn tác/làm lại (>= 30 bước), lưu/tiếp tục chỉnh sửa, và timelapse.
- Trạng thái lưu: `statePath` chứa ActionLog (JSON/protobuf) và PNG xem trước.

## 6. Cây thư mục (package `com.<tên>.doodlestudio`)

```
app/src/main/java/.../
├── di/                          # Koin
├── data/
│   ├── local/                   # Room: ArtworkDao, AppDatabase
│   ├── repository/              # ArtworkRepository, TemplateRepository, SettingsRepository
│   └── assets/                  # loader tranh, CREDITS
├── domain/
│   ├── canvas/
│   │   ├── LabelMapBuilder.kt   # tạo bản đồ nhãn vùng
│   │   ├── RegionFiller.kt      # đổ màu theo nhãn
│   │   ├── BrushEngine.kt       # nét vẽ, tẩy, độ dày/mờ
│   │   ├── HistoryManager.kt    # undo/redo trên ActionLog
│   │   └── ActionLog.kt
│   ├── export/
│   │   ├── TimelapseRenderer.kt # phát lại ActionLog ra frame
│   │   └── VideoEncoder.kt      # MediaCodec + MediaMuxer, fallback
│   └── model/
├── ui/
│   ├── navigation/
│   ├── screens/                 # splash, onboarding, home, gallery, editor, result, library, settings
│   └── components/              # CanvasView, ColorPalette, ToolBar, ZoomPanLayer
└── util/
app/src/main/assets/templates/   # tranh line-art
app/src/main/res/values, values-vi
app/src/test/                    # unit test thuần
docs/PHASE_REPORTS/ , docs/TEST_ON_DEVICE.md
```

## 7. Chi tiết kỹ thuật các phần khó

**Label map / flood fill**
- Tính nhãn bằng two-pass connected components hoặc scanline trên mask nét viền, chạy `Dispatchers.Default`, hiển thị loading ngắn khi mở tranh.
- Nhãn lưu `IntArray` (1024x1024, khoảng 4MB), cache theo `templateId` để không tính lại.
- Chạm vào nét viền: chọn vùng gần nhất trong bán kính vài pixel.

**Zoom/pan**
- Ma trận biến đổi một nơi duy nhất; tọa độ chạm đổi ngược về tọa độ canvas trước khi tô/vẽ.

**Vẽ tự do**
- Path mượt (nội suy bậc hai), vẽ vào free-draw layer; nét lưu dạng danh sách điểm nén để phát lại.

**Timelapse**
- Phát lại ActionLog theo thời gian nén (tổng 8-20 giây, tăng tốc đều), vẽ từng frame lên bitmap rồi đẩy vào `MediaCodec` qua Surface, ghép nhạc nền CC0 bằng `MediaMuxer`, thêm watermark tên app.
- **Fallback:** nếu mã hóa video lỗi hoặc hết dung lượng thì vẫn lưu PNG và báo rõ cho người dùng.
- Kiểm tra dung lượng trống trước khi xuất.

**Lưu trữ**
- Room lưu metadata; file `.json` + PNG nằm trong `filesDir`; xuất ra `Pictures/` và `Movies/` bằng MediaStore.

## 8. Lộ trình phase và tiêu chí nghiệm thu

| Phase | Nội dung | Nghiệm thu (có log) |
|---|---|---|
| **P0** | Plan + chọn kiến trúc, cây thư mục | Người dùng duyệt, chưa có code lớn |
| **P1** | Khung app, điều hướng, thư viện 10 tranh đầu, Home + Danh sách | Build sạch, test dữ liệu tranh pass |
| **P2** | Engine tô: label map, fill, hoàn tác/làm lại, zoom/pan | Unit test: vùng kín, viền hở, trùng màu, biên bitmap, undo 30 bước |
| **P3** | Vẽ tự do: bút, tẩy, độ dày/mờ, lớp nền, ActionLog | Test ActionLog tuần tự hóa/phát lại đúng |
| **P4** | Lưu tác phẩm, thư viện cá nhân, tiếp tục chỉnh sửa | Test repository; lưu rồi mở lại đúng trạng thái |
| **P5** | Xuất timelapse MP4, nhạc nền, watermark, fallback | Test tính thời gian/số frame; MP4 mở được **(cần máy thật)** |
| **P6** | 40+ tranh, chủ đề/độ khó, âm thanh, hiệu ứng, màn Kết quả + share | Test dữ liệu: đủ số lượng, không trùng id, tải được |
| **P7** | Settings, EN/VI, edge case, R8 release, tách ABI, Data Safety, README | Build release chạy được, `TEST_ON_DEVICE.md` hoàn chỉnh |

**Quy trình mỗi phase:**
1. `gradlew clean assembleDebug` + toàn bộ test (không lọc).
2. Dán số test lấy từ file XML kết quả (không viết lại định dạng).
3. Cập nhật baseline SHA256.
4. Viết `docs/PHASE_REPORTS/P<n>.md` dạng bảng (Hạng mục | Trạng thái | Bằng chứng | Tồn đọng).
5. **Dừng chờ người dùng duyệt** trước khi sang phase kế.

## 9. Rủi ro và cách giảm

| Rủi ro | Giảm thiểu |
|---|---|
| Hết RAM với bitmap lớn | Độ phân giải 1024 (768 trên máy yếu), tái sử dụng bitmap, history là lệnh chứ không phải ảnh |
| Vùng "rách" do viền hở | Nới mask 1-2px, gộp vùng nhiễu, test case viền hở |
| Viền răng cưa / hở màu | Fill layer nới dưới nét, line-art luôn nằm trên |
| Xuất video lỗi trên một số chip | Fallback PNG, báo rõ, thử trên >= 2 máy |
| Tranh tự vẽ trông đơn giản | Dùng nguồn CC0 chất lượng cao, hoặc người dùng bổ sung asset |
| Dung lượng ổ C thấp | Không tạo file lớn, dọn trước khi build |
| Không có emulator | Kiểm chứng trên điện thoại thật theo `TEST_ON_DEVICE.md` |

## 10. Nhiệm vụ cho P0 (việc agent phải làm ngay)

1. Đọc tài liệu này, chỉ ra chỗ nào chưa hợp lý hoặc thiếu, kèm đề xuất.
2. Xác nhận hoặc phản biện phương án kiến trúc C bằng so sánh về flood fill, hoàn tác, bộ nhớ; nếu đổi thì nêu lý do.
3. Hoàn thiện cây thư mục, tên package, danh sách dependency (kèm phiên bản) và cấu hình Gradle.
4. Liệt kê nguồn tranh và nhạc CC0 khả dụng (kèm giấy phép); nếu không tải được thì nói rõ người dùng cần cung cấp gì.
5. Viết `docs/PHASE_REPORTS/P0.md` và **dừng chờ duyệt, chưa viết code lớn**.

## 11. Việc người dùng cần cung cấp hoặc quyết định

1. **Tên chính thức** (tạm: Doodle Studio).
2. **Nguồn tranh:** để agent tự vẽ bằng code (đơn giản, an toàn) hoặc cung cấp bộ tranh CC0/đã mua bản quyền.
3. **Nhạc nền:** để agent chọn từ nguồn CC0 hoặc tự cung cấp.
4. **Điểm riêng** (ví dụ chủ đề Việt Nam, tô màu kèm học từ vựng tiếng Anh).
5. **Điện thoại thật** để chạy `TEST_ON_DEVICE.md` (tên máy + Android).
6. Dọn ổ C còn tối thiểu 10GB.
