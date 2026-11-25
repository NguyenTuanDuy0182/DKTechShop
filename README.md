# DKTechShop

Ứng dụng thương mại điện tử bán thiết bị công nghệ trên nền tảng Android, phát triển bằng Java theo kiến trúc MVVM kết hợp Firebase Authentication và Cloud Firestore.

## Mục lục

- [Tính năng chính](#tính-năng-chính)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc và cấu trúc dự án](#kiến-trúc-và-cấu-trúc-dự-án)
- [Cấu trúc dữ liệu Cloud Firestore](#cấu-trúc-dữ-liệu-cloud-firestore)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Hướng dẫn cài đặt và chạy dự án](#hướng-dẫn-cài-đặt-và-chạy-dự-án)
- [Kiểm thử](#kiểm-thử)

## Tính năng chính

1. **Xác thực và quản lý tài khoản**
   - Đăng ký, đăng nhập bằng Email/Mật khẩu và Google Sign-In qua Firebase Authentication.
   - Khôi phục mật khẩu qua email với hộp thoại Material 3.
   - Bản địa hóa toàn bộ thông báo lỗi đăng nhập/đăng ký sang tiếng Việt.
   - Quản lý hồ sơ cá nhân (họ tên, số điện thoại), sổ địa chỉ giao hàng (thêm, sửa, xóa, đặt mặc định).
   - Đổi mật khẩu cho tài khoản Email/Mật khẩu và tự động vô hiệu hóa mục đổi mật khẩu đối với phiên đăng nhập bằng Google.

2. **Khám phá, tìm kiếm và sắp xếp sản phẩm**
   - Lọc nhanh theo danh mục (Tất cả, Chuột Gaming, Tai nghe, Bàn phím, Dưới 3.5tr).
   - Sắp xếp sản phẩm theo giá (thấp đến cao, cao đến thấp) và lượt mua (bán chạy nhất, ít đến nhiều).
   - Tìm kiếm hỗ trợ bỏ dấu tiếng Việt, khớp theo tên sản phẩm, thương hiệu, danh mục, thông số kỹ thuật và phiên bản màu sắc.
   - Hiển thị chi tiết sản phẩm kèm thông số kỹ thuật, chính sách bảo hành và lượt bán.

3. **Quản lý tồn kho theo từng màu sắc/biến thể**
   - Mỗi phiên bản màu sắc của sản phẩm có tồn kho riêng biệt (`variants[].stock`) và tổng tồn kho (`stock`) được đồng bộ tự động.
   - Hiển thị tồn kho cụ thể theo từng màu, tự động khóa nút chọn mua khi màu hết hàng và giới hạn số lượng thêm vào giỏ theo tồn kho thực tế.

4. **Giỏ hàng và thanh toán**
   - Đồng bộ giỏ hàng thời gian thực từ Firestore (`users/{uid}/cart`).
   - Hỗ trợ chọn/bỏ chọn từng sản phẩm hoặc chọn tất cả để thanh toán từng phần.
   - Đổi nhanh hoặc thêm mới địa chỉ giao hàng trực tiếp trong màn hình giỏ hàng, bắt buộc số điện thoại hợp lệ trước khi đặt hàng.
   - Hỗ trợ 3 phương thức thanh toán: COD, VNPay QR (mô phỏng mã VietQR động kèm đếm ngược) và Ví MoMo (mô phỏng xác thực mã PIN 6 chữ số).
   - Đặt hàng nguyên tử bằng Firestore Transaction: kiểm tra đủ tồn kho theo đúng màu, trừ kho, tăng lượt bán (`soldCount`), tạo đơn hàng và xóa các món đã đặt khỏi giỏ.

5. **Quản lý đơn hàng và đánh giá**
   - Theo dõi đơn hàng thời gian thực, lọc theo trạng thái: Đang xử lý, Đang giao, Đã giao, Đã hủy.
   - Xem chi tiết đơn hàng, sao chép mã đơn, xác nhận đã nhận hàng và mua lại đơn cũ.
   - Hủy đơn hàng nguyên tử bằng Firestore Transaction: hoàn trả tồn kho về đúng từng màu đã đặt và trừ lại lượt bán tương ứng, chống hủy trùng.
   - Đánh giá sản phẩm sau khi đơn hàng đã giao thành công, giới hạn mỗi đơn hàng chỉ được đánh giá 1 lần cho mỗi sản phẩm.

6. **Giám sát kết nối mạng**
   - Tự động phát hiện trạng thái mất mạng/có mạng theo thời gian thực qua `NetworkMonitor`, hiển thị banner cảnh báo và hỗ trợ thử lại trên các màn hình chính.

## Công nghệ sử dụng

- **Ngôn ngữ:** Java 11
- **Nền tảng:** Android SDK (`minSdk = 24`, `compileSdk = 37`, `targetSdk = 37`)
- **Kiến trúc:** MVVM (Model - View - ViewModel) + Repository Pattern
- **Giao diện:** Material Components for Android (Material 3), RecyclerView, ConstraintLayout, SwipeRefreshLayout
- **Backend & Cloud:** Firebase BoM 35.2.0 (Firebase Authentication, Cloud Firestore), Google Play Services Auth
- **Tải ảnh:** Glide 4.16.0
- **Quản lý vòng đời:** AndroidX Lifecycle (ViewModel, LiveData)
- **Kiểm thử:** JUnit 4, AndroidX Test, Espresso

## Kiến trúc và cấu trúc dự án

```text
app/src/main/java/com/ecommerce/dktechshop/
├── MainActivity.java                 # Điều hướng 4 tab chính (Trang chủ, Giỏ hàng, Đơn hàng, Hồ sơ)
├── data/
│   ├── model/                        # POJO ánh xạ Firestore (Product, ProductVariant, CartItem, Order, OrderItem, Review, User, Address)
│   └── repository/                   # Xử lý dữ liệu Firebase (AuthRepository, ProductRepository, CartRepository, OrderRepository)
├── ui/
│   ├── auth/                         # AuthActivity, AuthViewModel
│   ├── home/                         # HomeFragment, ProductGridAdapter
│   ├── search/                       # SearchActivity, ProductSearchAdapter
│   ├── detail/                       # ProductDetailActivity, ProductVariantBottomSheetDialog, ReviewDialogHelper
│   ├── cart/                         # CartFragment, CartAdapter, CartViewModel, PaymentDemoDialog
│   ├── orders/                       # OrdersFragment, OrderDetailActivity, OrderAdapter, OrderViewModel
│   ├── profile/                      # ProfileFragment
│   └── viewmodel/                    # ShopViewModel
└── utils/                            # CurrencyUtils, NetworkMonitor
```

## Cấu trúc dữ liệu Cloud Firestore

- `products/{productId}`: thông tin sản phẩm (`name`, `brand`, `category`, `categoryLabel`, `price`, `originalPrice`, `discountPercent`, `rating`, `reviewCount`, `stock`, `soldCount`, `thumbnailUrl`, `description`, `specs`, `variants[]`).
  - `variants[]`: mảng object gồm `name`, `colorHex`, `extraPrice`, `stock`.
  - `products/{productId}/reviews/{reviewId}`: danh sách đánh giá theo từng đơn hàng (`orderId`, `productId`, `userId`, `userName`, `rating`, `comment`, `variant`, `createdAt`).
- `orders/{orderId}`: danh sách đơn hàng toàn hệ thống.
- `users/{uid}`: thông tin người dùng (`uid`, `name`, `email`, `phone`, `address`, `tier`, `isLoggedIn`).
  - `users/{uid}/addresses/{addressId}`: sổ địa chỉ giao hàng.
  - `users/{uid}/cart/{itemId}`: giỏ hàng thời gian thực của người dùng.
  - `users/{uid}/orders/{orderId}`: bản sao đơn hàng theo người dùng.

## Yêu cầu môi trường

- Android Studio bản hỗ trợ Android Gradle Plugin 9.x
- JDK 11 trở lên (khuyến nghị dùng JDK tích hợp của Android Studio / OpenJDK có đầy đủ `jlink`)
- Dự án Firebase đã bật **Authentication** (Email/Password và Google) và **Cloud Firestore**

## Hướng dẫn cài đặt và chạy dự án

1. Clone repository về máy:

```bash
git clone <repository-url>
cd DKTechShop
```

2. Cấu hình Firebase:
   - Tạo ứng dụng Android trên Firebase Console với application ID `com.ecommerce.dktechshop`.
   - Thêm SHA-1 của máy phát triển vào Firebase Project Settings để sử dụng Google Sign-In.
   - Tải tệp `google-services.json` và đặt vào thư mục `app/google-services.json`.

3. Mở dự án bằng Android Studio, chờ Gradle đồng bộ hoàn tất và nhấn **Run** để cài đặt lên thiết bị thật hoặc máy ảo Android (API 24+).

4. Build APK debug bằng dòng lệnh:

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

## Kiểm thử

Dự án bao gồm bộ Unit Test trong `app/src/test/java/com/ecommerce/dktechshop/`:

- `CartItemTest`: kiểm tra chuẩn hóa mã giỏ hàng, lọc sản phẩm được chọn, tính tổng tiền và xác thực số điện thoại.
- `OrderTest`: kiểm tra mô hình đơn hàng, parse tồn kho theo từng màu, trừ kho/tăng lượt mua khi đặt hàng, từ chối khi vượt tồn kho của màu và hoàn kho/giảm lượt mua khi hủy đơn.
- `ReviewEligibilityTest`: kiểm tra điều kiện đánh giá đơn hàng đã giao và chống đánh giá trùng.
- `SearchTest`: kiểm tra bỏ dấu tiếng Việt, tìm kiếm đa trường và sắp xếp theo giá/lượt mua.

Chạy toàn bộ Unit Test bằng dòng lệnh:

```bash
# Windows
.\gradlew.bat testDebugUnitTest

# macOS / Linux
./gradlew testDebugUnitTest
```
