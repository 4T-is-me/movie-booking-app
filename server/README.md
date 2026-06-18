# Movie Booking App - Server

Dự án này là phần Backend (Server) cho ứng dụng Đặt vé xem phim (Movie Booking App), được xây dựng bằng Node.js và Express.

## Cấu trúc thư mục và chức năng

Dưới đây là mô tả chi tiết về chức năng của các file và folder chính trong dự án:

### 📁 Các thư mục (Folders)

- **`src/`**: Thư mục chứa toàn bộ mã nguồn chính của server.
  - **`controllers/`**: Chứa các hàm xử lý logic cho các API endpoints (ví dụ: `user.controller.js`, `movie.controller.js`, `booking.controller.js`). Tiếp nhận request từ client và trả về response.
  - **`models/`**: Chứa định nghĩa các cấu trúc dữ liệu (Database schemas/models), thường tương tác với MongoDB (Mongoose) hoặc SQL (Sequelize) (ví dụ: `User.js`, `Movie.js`, `Ticket.js`).
  - **`routes/`**: Chứa các file định nghĩa các route/đường dẫn API của ứng dụng (ví dụ: `api.js`, `auth.routes.js`).

  - **`middlewares/`**: Chứa các hàm trung gian (middleware) để xử lý logic trước khi vào controller (ví dụ: xác thực JWT, phân quyền admin, kiểm tra lỗi dữ liệu).
  - **`config/`**: Chứa các file cấu hình của ứng dụng (ví dụ: cấu hình kết nối Database, cấu hình Passport.js, cấu hình biến môi trường).

  - **`utils/`** (hoặc **`helpers/`**): Chứa các hàm tiện ích dùng chung ở nhiều nơi (ví dụ: format ngày tháng, mã hóa mật khẩu, gửi email xác nhận).
  - **`services/`**: (Tùy chọn) Chứa các logic nghiệp vụ (business logic) phức tạp để giảm tải cho các controllers.

- **`node_modules/`**: Thư mục chứa các gói thư viện và dependencies được cài đặt qua npm. (Không bao giờ được đẩy lên Git).

- **`public/`**: Chứa các tài nguyên tĩnh (static files) như hình ảnh cover phim, CSS, JS nếu server có nhiệm vụ host các file này.

### 📄 Các tệp tin (Files)

- **`server.js`**: Điểm đầu vào (entry point) của ứng dụng. Nơi khởi tạo server Express, cấu hình middleware toàn cục, kết nối Database và lắng nghe các cổng mạng (PORT).

- **`.env`**: File chứa các biến môi trường nhạy cảm (như chuỗi kết nối DB `DB_URI`, `PORT`, `JWT_SECRET`). File này cần được thêm vào `.gitignore`.

- **`.env.example`**: File mẫu chứa tên các biến môi trường (không bao gồm giá trị thật) để thành viên khác trong team biết cần cấu hình những gì khi clone code về.

- **`package.json`**: File quan trọng nhất của Node.js, chứa thông tin dự án, các scripts chạy app (`npm start`, `npm run dev`) và danh sách các thư viện cần thiết (dependencies).

- **`package-lock.json`** (hoặc **`yarn.lock`**): Ghi lại chính xác phiên bản của các thư viện con đã được cài đặt, đảm bảo sự đồng nhất trên mọi môi trường.

- **`.gitignore`**: Chứa danh sách các file và folder không muốn đẩy lên Git (ví dụ: `node_modules`, `.env`).

- **`README.md`**: File tài liệu hướng dẫn tổng quan về dự án (chính là file này).

## Hướng dẫn cài đặt và chạy dự án

Làm theo các bước sau để thiết lập và chạy server trên máy tính của bạn:

### Yêu cầu hệ thống

- Node.js
- npm (hoặc yarn)

### Các bước cài đặt

1. **Clone mã nguồn về máy**

   ```bash
   git clone <url-repository-của-bạn>
   cd movie-booking-app/server
   ```

2. **Cài đặt các gói thư viện (dependencies)**

   ```bash
   npm install
   ```

3. **Cấu hình biến môi trường**
   Tạo file `.env` từ file mẫu `.env.example` có sẵn trong dự án bằng lệnh sau:
   ```bash
   cp .env.example .env
   ```
   _(Nếu bạn sử dụng Windows Command Prompt, dùng lệnh: `copy .env.example .env`)_
   Sau đó, mở file `.env` vừa tạo và cập nhật các giá trị thực tế cho các biến môi trường (Database URI, Cổng chạy server, v.v.).

### Chạy dự án

- **Môi trường Phát triển (Development):** Chạy lệnh dưới đây để khởi động server bằng `nodemon` (tự động tải lại khi có thay đổi mã nguồn).

  ```bash
  npm run dev
  ```

- **Môi trường Sản xuất (Production):** Chạy lệnh dưới đây để build mã nguồn bằng `babel` và khởi chạy server trên code đã được build.
  ```bash
  npm run production
  ```
