package com.example.demo;

import com.example.demo.model.Note;
import com.example.demo.model.NoteType;
import com.example.demo.model.User;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@EnableScheduling
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
    // 使用 Bean 注入 CommandLineRunner，專案啟動時會自動跑這裡
    @Bean
    public CommandLineRunner initData(UserRepository userRepository,  NoteRepository noteRepository) {
        return args -> {
            userUsing(userRepository);
            noteUsing(userRepository, noteRepository);
        };
    }
    //帳號系統預設資料
    private void userUsing(UserRepository userRepository){
        if (userRepository.count() == 0) {
            System.out.println("🌱 [系統提示] 偵測到 Supabase 帳號表為空，開始初始化預設帳號...");

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("123456");
            admin.setNickname("大總管");
            admin.setRole("ADMIN");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setPassword("123456");
            user.setNickname("一般小明");
            user.setRole("USER");
            userRepository.save(user);

            System.out.println("✅ [系統提示] 預設帳號初始化完成！");
        }
    }
    //筆記功能預設資料
    private void noteUsing(UserRepository userRepository, NoteRepository noteRepository){
        // 2. 筆記平台資料初始化（多媒體升級版）
        if (noteRepository.count() == 0) {
            System.out.println("🌱 [系統提示] 開始注入富文本多媒體模擬筆記數據...");

            // 筆記 A：Java 集合框架（塞入 3 張圖片、1 個附件檔案）
            Note n1 = new Note();
            n1.setTitle("Java 集合框架（Collection）全解析期末衝刺筆記");
            n1.setContent("這篇筆記詳細記錄了 List、Set、Map 的底層結構與常考面試題...\n\n" +
                    "【學習重點】\n" +
                    "1. ArrayList 擴容機制是原本的 1.5 倍。\n" +
                    "2. HashMap 在 Java 8 之後，當鏈結長度大於 8 且陣列長度大於 64 時會轉為紅黑樹。");
            n1.setType(NoteType.TECH);
            n1.setTags("Java,資料結構,期末考");
            n1.setCoverImageUrl("https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=500&q=80");
            n1.setViewsCount(1450);
            n1.setLikesCount(120);

            // 📝 附加檔案預留
            n1.setAttachmentUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
            n1.setAttachmentFileName("Java_Collection_CheatSheet.pdf");
            noteRepository.save(n1);

            // 📸 為 ⚙️ 筆記 A 綁定 3 張內容插圖（注意：透過雙向關聯綁定）
            com.example.demo.model.NoteImage img1 = new com.example.demo.model.NoteImage();
            img1.setImageUrl("https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&q=80");
            img1.setNote(n1);

            com.example.demo.model.NoteImage img2 = new com.example.demo.model.NoteImage();
            img2.setImageUrl("https://images.unsplash.com/photo-1607799279861-4dd421887fb3?w=600&q=80");
            img2.setNote(n1);

            com.example.demo.model.NoteImage img3 = new com.example.demo.model.NoteImage();
            img3.setImageUrl("https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=600&q=80");
            img3.setNote(n1);

            n1.getImages().add(img1);
            n1.getImages().add(img2);
            n1.getImages().add(img3);
            noteRepository.save(n1); // 再次儲存，連帶寫入圖片

            // 調整發布時間
            n1.setCreatedAt(com.example.demo.config.AppClock.now().minusMinutes(10));
            noteRepository.save(n1);


            // 筆記 B：密碼學類（塞入 1 張圖片、1 個真實 YouTube 影片嵌入）
            Note n2 = new Note();
            n2.setTitle("RSA 加密演算法公式推導與 Meet-in-the-middle 攻擊防範");
            n2.setContent("關於大質數相乘的質因數分解難題，以及非對稱加密的核心邏輯推導...\n\n" +
                    "【數學公式核心】\n" +
                    "找兩個超大質數 p 和 q，計算 n = p * q，以及歐拉函數 φ(n) = (p-1)*(q-1)。\n" +
                    "選擇一個與 φ(n) 互質的 e，並計算 e 關於 φ(n) 的模反元素 d。");
            n2.setType(NoteType.TECH);
            n2.setTags("資安,RSA,數學");
            n2.setCoverImageUrl("https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500&q=80");
            n2.setViewsCount(890);
            n2.setLikesCount(340);

            // 🎥 嵌入 YouTube 影片（這裡使用一個資安教學影片作為示範 ID）
            n2.setYoutubeVideoId("3xgBnD_VsM8");
            noteRepository.save(n2);

            com.example.demo.model.NoteImage img4 = new com.example.demo.model.NoteImage();
            img4.setImageUrl("https://images.unsplash.com/photo-1563206767-5b18f218e8de?w=600&q=80");
            img4.setNote(n2);
            n2.getImages().add(img4);
            noteRepository.save(n2);

            n2.setCreatedAt(com.example.demo.config.AppClock.now().minusHours(2));
            noteRepository.save(n2);


            // 筆記 C：微積分類（保持純文字，測試無多媒體時的相容性）
            Note n3 = new Note();
            n3.setTitle("大一微積分：羅必達法則與泰勒級數展開精選題庫");
            n3.setContent("收錄工學院大一必考的微積分考古題，含詳細的極限變換與逼近解法...");
            n3.setType(NoteType.TECH);
            n3.setTags("微積分,考古題,工學院");
            n3.setCoverImageUrl("https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=500&q=80");
            n3.setViewsCount(320);
            n3.setLikesCount(45);
            noteRepository.save(n3);
            n3.setCreatedAt(com.example.demo.config.AppClock.now().minusDays(3));
            noteRepository.save(n3);

            System.out.println("✅ [系統提示] 富文本多媒體模擬數據已注入 Supabase！");
        }
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
