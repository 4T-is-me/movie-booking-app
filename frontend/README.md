# Movie Booking App - Client

Ứng dụng Android đặt vé xem phim được xây dựng theo kiến trúc MVVM (Model-View-ViewModel).

## 🌟 Tính năng (Dự kiến)

- Xem danh sách các bộ phim đang chiếu và sắp chiếu.
- Xem chi tiết phim, trailer, đánh giá và diễn viên.
- Đặt vé và chọn ghế ngồi trực quan.
- Quản lý tài khoản, lịch sử giao dịch và vé điện tử (QR Code).

## 📂 Cấu trúc Thư mục

Dưới đây là chi tiết về các folder chính trong dự án:

### 1. `data/`

Chứa toàn bộ logic liên quan đến dữ liệu của ứng dụng.

- **`api/`**: Chứa các file định nghĩa giao tiếp mạng và gọi API với Retrofit.
  - **`ApiService.kt`**: Interface định nghĩa các phương thức tương ứng với từng endpoint của API (ví dụ: `getMovies()`).
  - **`RetrofitClient.kt`**: Singleton object dùng để khởi tạo và cấu hình Retrofit instance (Base URL, Converter), đảm bảo chỉ có một instance duy nhất trong toàn bộ ứng dụng.

- **`local/`**: Quản lý dữ liệu lưu trữ cục bộ trên thiết bị (Room Database, DataStore, SharedPreferences). Phụ trách lưu trữ bộ nhớ đệm (cache) hỗ trợ offline, cài đặt người dùng và thông tin phiên đăng nhập.
  - **`TokenManager.kt`**: Quản lý việc lưu trữ an toàn Access Token và Refresh Token trên thiết bị.

- **`model/`**: Định nghĩa các Data Classes (POJO) để ánh xạ dữ liệu từ API hoặc Database (ví dụ: `Movie.kt`).

- **`repository/`**: Lớp trung gian điều phối dữ liệu từ các nguồn khác nhau (Remote API, Local DB) và cung cấp cho ViewModel (ví dụ: `MovieRepositoryImpl.kt`, `AuthRepository.kt`).

### 2. `di/` (Dependency Injection)

Sử dụng **Hilt (Dagger)** để quản lý việc khởi tạo và cung cấp các phụ thuộc (dependencies) trong toàn bộ ứng dụng.

### 3. `ui/`

Chứa các thành phần giao diện người dùng và logic hiển thị.

- **`viewmodel/`**: Xử lý logic nghiệp vụ cho UI và giữ trạng thái dữ liệu.
- **`view/`**: Các Activity, Fragment hoặc Compose Screen.
- **`adapter/`**: Chứa các Adapter để hiển thị dữ liệu lên danh sách (RecyclerView) nếu sử dụng UI truyền thống.

### 4. `utils/`

Chứa các hàm và lớp tiện ích dùng chung (Constants, Extensions, Helpers) cho toàn bộ dự án.

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ**: Kotlin
- **Kiến trúc**: MVVM
- **DI**: Hilt (Dagger)
- **Network**: Retrofit & Gson
- **Bất đồng bộ**: Kotlin Coroutines & Flow
- **UI**: Material Design components

## 🚀 Hướng dẫn cài đặt và chạy

1. **Clone repository:**
   ```bash
   git clone <đường-dẫn-repo-của-bạn>
   ```
2. **Mở dự án:**
   Mở thư mục `client` bằng **Android Studio** (khuyến nghị sử dụng phiên bản mới nhất).
3. **Đồng bộ Gradle:**
   Chờ hệ thống tải về các thư viện cần thiết và hoàn tất index dự án.
4. **Chạy ứng dụng:**
   Chọn thiết bị di động hoặc máy ảo (Emulator) và nhấn **Run** (hoặc tổ hợp phím `Shift + F10`).

## ⚠️ Lưu ý

Hiện tại các phụ thuộc (dependencies) của Hilt và Retrofit cần được khai báo thêm trong `build.gradle.kts` để project có thể build thành công.
Đồng thời, cần kết nối với backend server đang hoạt động hoặc sử dụng mock API để cung cấp dữ liệu phim hiển thị lên ứng dụng.
