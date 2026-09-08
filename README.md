# NoteShare

NoteShare 是以 Spring Boot 建置的筆記分享與學習社群平台。除了筆記管理，也整合留言、收藏、追蹤、排行榜、成就任務、商城與個人自習室等遊戲化功能。

## 主要功能

- 會員註冊、登入、登出與個人資料
- 筆記建立、編輯、刪除、搜尋與分類
- 公開、私人及連結分享權限
- 封面、內文圖片、附件連結與 YouTube 影片
- 按讚、收藏、留言與追蹤
- 排行榜、成就、每日任務與獎勵
- 外觀商城、裝備與個人儀表板
- 自習室家具、背包、房間配置與每日商店
- 內容檢舉與管理員頁面

## 技術棧

- Java 21
- Spring Boot 4.0.6
- Spring MVC、Thymeleaf
- Spring Data JPA
- Spring Security
- PostgreSQL（可使用 Supabase）
- Maven
- Lombok
- Jsoup

## 環境需求

- JDK 21
- Maven 3.9 或可正常使用的 Maven Wrapper
- PostgreSQL 資料庫
- Supabase 專案（使用相關儲存功能時需要）

## 本機設定

1. 複製範例設定檔：

   ```powershell
   Copy-Item src/main/resources/application-example.properties src/main/resources/application.properties
   ```

2. 編輯 `src/main/resources/application.properties`，填入自己的連線資訊：

   ```properties
   spring.datasource.url=YOUR_DATABASE_URL
   spring.datasource.username=YOUR_DATABASE_USERNAME
   spring.datasource.password=YOUR_DATABASE_PASSWORD
   supabase.url=YOUR_SUPABASE_URL
   supabase.key=YOUR_SUPABASE_KEY
   ```

`application.properties` 已列入 `.gitignore`。請勿將真實密碼或 API Key 提交至版本控制。

## 啟動方式

使用 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

若電腦已安裝 Maven：

```powershell
mvn spring-boot:run
```

啟動完成後開啟：

```text
http://localhost:8080
```

## 測試

```powershell
.\mvnw.cmd test
```

或：

```powershell
mvn test
```

## 專案結構

```text
src/main/java/com/example/demo/
├── config/        # Spring MVC、Security 與應用程式設定
├── controller/    # 網頁與 REST API 控制器
├── dto/           # API 與畫面資料傳輸物件
├── initializer/   # 初始資料與啟動工作
├── interceptor/   # Session 登入與權限攔截
├── model/         # JPA 實體與列舉
├── repository/    # Spring Data JPA Repository
├── scheduler/     # 排程工作
├── service/       # 業務邏輯
└── util/          # 檔案與網址處理工具

src/main/resources/
├── static/        # CSS、JavaScript 與靜態圖片
├── templates/     # Thymeleaf 頁面
└── application-example.properties
```

使用者上傳的檔案預設放在專案根目錄的 `uploads/`，此資料夾不會提交到 Git。

## 開發注意事項

- 專案目前使用 Session 與攔截器管理登入狀態。
- CSRF 目前處於停用狀態，正式部署前應恢復保護並讓 AJAX 請求攜帶 CSRF Token。
- `DemoApplication` 包含開發用初始帳號與範例筆記資料，正式環境應移除或改成僅在開發 Profile 執行。
- 自習室購買流程仍有餘額驗證與扣款 TODO，完成前不建議開放正式交易。
- 正式部署時應驗證上傳檔案的 MIME type、內容與大小。

## Git 提交

```powershell
git add README.md
git commit -m "docs: add project README"
git push
```

## License

目前尚未指定授權條款。如要開放他人使用或貢獻，建議加入適合的 `LICENSE`。
