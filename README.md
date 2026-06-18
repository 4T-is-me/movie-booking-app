# 🎬 Movie Booking App (Ứng dụng Đặt vé xem phim)

Chào mừng bạn đến với dự án **Movie Booking App**! Đây là một giải pháp toàn diện cho hệ thống đặt vé xem phim, bao gồm một Backend (Server) mạnh mẽ cung cấp RESTful API và ứng dụng Mobile (Client - Android) mang đến trải nghiệm đặt vé mượt mà, trực quan và tiện lợi cho người dùng.

## 🛠 Công nghệ sử dụng

Dự án áp dụng các công nghệ và quy chuẩn hiện đại cho cả hai phía:

**Phía Backend (Server):**

- **Nền tảng & Framework:** Node.js, Express.js.
- **Cơ sở dữ liệu:** MongoDB (thông qua Mongoose).
- **Xác thực & Bảo mật:** JWT (JSON Web Token), mã hóa mật khẩu với bcrypt.

**Phía Mobile App (Client):**

- **Ngôn ngữ & Kiến trúc:** Kotlin, MVVM (Model-View-ViewModel).
- **Mạng & Dữ liệu local:** Retrofit, OkHttp.
- **Xử lý bất đồng bộ:** Kotlin Coroutines & Flow.
- **Dependency Injection:** Hilt (Dagger).

## 🗂 Cấu trúc dự án

Dự án được chia thành hai phần chính, mỗi phần đều có tài liệu hướng dẫn chi tiết đi kèm:

- **[`/server`](./server/README.md):** Phần Backend được xây dựng bằng **Node.js, Express và MongoDB**. Cung cấp các RESTful API phục vụ cho việc quản lý rạp phim, lịch chiếu, thông tin phim, người dùng và xử lý nghiệp vụ đặt vé an toàn.
- **[`/client`](./client/README.md):**Phần Mobile App (Android) được xây dựng bằng **Kotlin** theo kiến trúc **MVVM**, tích hợp Retrofit, Hilt, Room và Coroutines để đem lại trải nghiệm mượt mà.

## 🚀 Hướng dẫn khởi chạy nhanh

Để chạy toàn bộ dự án trên môi trường local, bạn cần thiết lập phần Server trước, sau đó cấu hình Client kết nối tới Server.
