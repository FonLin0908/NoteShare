# NoteShare

NoteShare 是以 Spring Boot 建置的筆記分享與學習社群平台，將內容管理、社群互動與遊戲化機制整合在同一套系統中。使用者可以整理與分享學習筆記，並透過任務、成就、排行榜及個人空間獲得持續學習的回饋。

## 專案亮點

- 採用 MVC 與分層架構，區分 Controller、Service、Repository 及資料模型
- 使用 Spring Data JPA 管理會員、筆記、留言、收藏及遊戲資料的關聯
- 以 Session 搭配攔截器實作登入狀態與角色權限控制
- 支援筆記可見範圍、分享連結、多媒體內容及檔案上傳
- 透過排程與事件邏輯實作每日任務、成就、經驗值及排行榜
- 將商城、外觀收藏與自習室配置整合為遊戲化學習體驗

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

| 分類 | 使用技術 |
| --- | --- |
| Backend | Java 21、Spring Boot 4.0.6、Spring MVC |
| Frontend | Thymeleaf、HTML、CSS、JavaScript、Bootstrap |
| Database | PostgreSQL、Spring Data JPA、Hibernate |
| Security | Spring Security、BCrypt、Session、Interceptor |
| Tools | Maven、Lombok、Jsoup、Git |

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

使用者上傳的檔案預設放在專案根目錄的 `uploads/`，並透過 `.gitignore` 排除本機檔案與敏感設定。
