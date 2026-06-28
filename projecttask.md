
[Android UI-Seating] Thiết kế màn hình chọn Suất chiếu và Sơ đồ chọn Ghế ngồi theo phòng chiếu

Objective
Xây dựng giao diện chọn ngày chiếu/suất chiếu và sơ đồ chọn ghế ngồi động theo cấu trúc phòng chiếu và loại ghế (VIP/Thường), xử lý validate số lượng ghế đăng ký so với số ghế thực chọn dựa trên mục "3.1.4.1" và "3.1.4.2" của SRS. Tuân thủ quy định không push code trực tiếp vào main/develop, thực hiện tạo branch feature/issue-11-design-seating-map-screen và Merge Request vào develop.

Scope of Work




Thiết kế giao diện chọn lịch chiếu: Thanh hiển thị ngày chiếu (dạng ngang), danh sách các khung giờ chiếu hiển thị bên dưới.   
Task actions

      Convert to child item
     Delete 



Thiết kế sơ đồ ghế ngồi (SeatMapActivity):



      Spinner/Combobox chọn số lượng ghế muốn đặt (từ 1 đến 8 ghế).

        
      Lưới hiển thị danh sách ghế ngồi động (A1, A2... K10) lấy cấu trúc từ API suất chiếu.

        
      Định nghĩa màu sắc biểu diễn ghế: Ghế đang chọn (Màu xanh lá cây), Ghế đã bán (Màu đỏ), Ghế trống thường (Màu xám nhạt), Ghế trống VIP (Màu xanh dương).

   
        Task actions
            
      Convert to child item
     Delete 



Xử lý logic validate số lượng ghế trên giao diện khi nhấn "Continue":



      Nếu số lượng ghế đã click chọn nhỏ hơn số lượng đã chọn ở Combobox, báo lỗi: "Please selecte x seat more".

        
      Nếu số lượng ghế đã click chọn lớn hơn số lượng đã chọn ở Combobox, báo lỗi: "Please select only x seat".

   
        Task actions
            
      Convert to child item
     Delete 



Gọi API Backend lấy danh sách ghế theo lịch chiếu được chọn.   
Task actions

      Convert to child item
     Delete 


Acceptance Criteria




Giao diện hiển thị đúng các Suất chiếu và sơ đồ ghế khớp cấu trúc phòng chiếu từ API.   
Task actions

      Convert to child item
     Delete 



Màu sắc các loại ghế (VIP, thường, đã mua, đang chọn) được hiển thị đúng định màu chuẩn trong SRS.   
Task actions

      Convert to child item
     Delete 



Logic validate số lượng ghế hoạt động chính xác và hiển thị hộp thoại hoặc text cảnh báo lỗi khi không trùng khớp.   
Task actions

      Convert to child item
     Delete 



Tạo nhánh feature/design-seating-map-screen, đẩy code và tạo Merge Request test vào nhánh develop thành công.













# Design Spec — Showtime Selection & Seat Map Screen

Bản kiến trúc design system để paste cho Agent code. Dựa trên screenshot (dark theme, accent vàng) nhưng *điều chỉnh màu ghế theo đúng yêu cầu SRS* (4 loại màu: xanh lá / đỏ / xám / xanh dương).

---

## 1. Design Tokens

### Colors
// Background
bg.primary        #0A0A0A   (nền màn hình)
bg.surface        #1A1A1A   (card, spinner, time chip)
bg.elevated       #242424   (ghế available default)

// Accent (brand)
accent.primary    #F5C518   (vàng - nút, ngày được chọn)
accent.pressed    #D9AD0F

// Seat states (THEO SRS)
seat.selected     #34C759   (xanh lá - ghế đang chọn)
seat.sold         #E5484D   (đỏ - ghế đã bán)
seat.available    #3A3A3A   (xám nhạt - ghế trống thường)
seat.vip          #2F6FED   (xanh dương - ghế trống VIP)
seat.disabled     #2A2A2A   (lối đi / khoảng trống)

// Seat text
seat.text.default #C9C9C9
seat.text.onColor #FFFFFF

// Text
text.primary      #FFFFFF
text.secondary    #9A9A9A
text.muted        #5C5C5C
text.error        #E5484D

// Border / divider
border.subtle     #2A2A2A
border.selected   #F5C518

### Typography (font: SF Pro / Inter / Roboto)
title.screen      24px  Bold      (Select seat)
section.header    20px  SemiBold  (Select Date & Time)
date.month        13px  Medium
date.day          18px  Bold
time.label        15px  SemiBold
seat.label        11px  Medium
legend.label      14px  Regular
total.label       14px  Regular
total.amount      22px  Bold
button.label      18px  Bold
error.text        13px  Medium

### Spacing & Radius
spacing: 4 / 8 / 12 / 16 / 24 / 32
radius.seat       6px
radius.chip       20px   (time/date pill)
radius.card       16px
radius.button     28px   (full pill)
screen.padding    20px (horizontal)

---

## 2. Component Specs

### 2.1 Top Bar
height        56px
back icon     24×24px, color text.primary, left 20px
title         centered, title.screen

### 2.2 Screen Indicator
Trapezoid/curved bar (màn hình chiếu)
top gradient line: accent.primary → transparent
height ~120px, label "SCREEN" 12px text.muted centered (optional)

### 2.3 Seat
size          32×32px (mobile) — auto-scale theo số cột
gap           8px (giữa các ghế), 4px chỉ tiêu (>11 cột giảm xuống 6px)
radius        6px
label         seat.label, centered
states:
available → bg seat.available,  text seat.text.default
vip       → bg seat.vip,        text #FFFFFF
sold      → bg seat.sold,        text #FFFFFF, không click được
selected  → bg seat.selected,    text #FFFFFF, border 1.5px lighten
row label     (A,B,C...) 11px text.muted, optional cột trái
Lưới render động từ API: `rows × cols`, mỗi ô có `id`, `type (NORMAL|VIP)`, `status (AVAILABLE|SOLD)`.


### 2.4 Legend
layout        row, wrap, gap 16px, margin-top 24px
item          [swatch 16×16, radius 4] + [legend.label] gap 8px
items (4 loại theo SRS):
● Available (xám)   ● VIP (xanh dương)
● Sold (đỏ)         ● Selected (xanh lá)

### 2.5 Seat Quantity Selector (Spinner/Combobox) — bổ sung, screenshot chưa có
Đặt phía trên lưới ghế (dưới screen indicator) hoặc cạnh legend.
label         "Number of seats"  14px text.secondary
control       dropdown pill, bg bg.surface, h 44px, radius 22px, padding 16px
text 16px text.primary + chevron icon 16px
values        1 → 8

### 2.6 Date Selector (ngang)
section header  "Select Date & Time", margin-top 32px
scroll          horizontal, gap 12px
item (pill)     width 60px, height 88px, radius 32px
default       bg transparent, border none
selected      bg accent.primary
content:
month  date.month  (selected: #0A0A0A / default: text.secondary)
day-circle 44×44px:
selected → bg #0A0A0A, text accent.primary
default  → bg bg.surface, text text.primary

### 2.7 Time Selector
scroll          horizontal, gap 12px, margin-top 16px
chip            height 48px, padding 24px, radius 24px
default       bg bg.surface, text text.primary
selected      bg transparent, border 1.5px accent.primary, text accent.primary

### 2.8 Footer (sticky bottom)
container       bg bg.primary, top border.subtle, padding 20px, safe-area
left:
"Total"        total.label text.secondary
"210.000 VND"  total.amount accent.primary
right (button):
label          "Continue" / "Buy ticket"  button.label #0A0A0A
bg             accent.primary, radius 28px
height         56px, min-width 200px
pressed        bg accent.pressed

### 2.9 Validation (khi nhấn Continue)
hiển thị        inline text trên footer HOẶC toast/dialog
color           text.error
thiếu ghế  →  "Please select x seat more"   (x = combobox - selected)
thừa ghế  →  "Please select only x seat"    (x = combobox)
đủ        →  proceed

---

## 3. Layout Order (top → bottom)
1. Top bar (back + title)
2. Screen indicator (trapezoid + glow)
3. Seat quantity selector
4. Seat grid (dynamic, scrollable nếu nhiều hàng)
5. Legend (4 states)
6. "Select Date & Time" header
7. Date row (horizontal scroll)
8. Time row (horizontal scroll)
9. Footer sticky (Total + Continue)

---

## 4. Lưu ý cho Agent
- *Màu ghế theo SRS, KHÔNG theo screenshot*: screenshot dùng vàng/nâu, nhưng spec chuẩn là xanh lá (selected) / đỏ (sold) / xám (normal) / xanh dương (VIP).
- Grid ghế *render động* từ response API suất chiếu — không hardcode A1..K10.
- Ghế sold → disable touch.
- Validate so sánh selectedSeats.length vs comboboxValue đúng 2 nhánh lỗi ở trên.
- Branch: feature/issue-11-design-seating-map-screen → MR vào develop, không push thẳng main/develop.

Bạn cần mình xuất thêm bản này dạng JSON token (để import thẳng vào code/Figma tokens) không?