package com.example.demo.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class StorageUtil {

    // 本機檔案儲存在專案根目錄的 uploads 資料夾。
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads";

    /**
     * 💻 本地主機儲存核心方法
     * @param file 前端傳來的實體檔案
     * @param folder 子資料夾 (例如: "covers", "images")
     * @return 儲存成功後，瀏覽器可以直接讀取的「本地相對虛擬網址」 (例如: "/uploads/covers/abc.jpg")
     */
    public static String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 1. 生成目標實體資料夾路徑 (例如: C:\your-project\\uploads\covers)
            Path targetFolder = Paths.get(UPLOAD_DIR, folder);
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder); // 隨開隨建，資料夾不存在自動幫你生出來
            }

            // 2. 生成隨機不重複檔名，防止覆蓋
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String cleanFilename = UUID.randomUUID().toString() + extension;

            // 3. 定義實體檔案存放在硬碟的最終落腳點
            Path targetPath = targetFolder.resolve(cleanFilename);

            // 將上傳內容寫入產生的目標檔案。
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 5. 回傳給瀏覽器讀取的「虛擬網址路徑」 (重要：前面多加 /uploads/)
            String virtualUrl = "/uploads/" + folder + "/" + cleanFilename;
            System.out.println("💻 [本地主機儲存] 檔案成功寫入實體硬碟！虛擬路徑：" + virtualUrl);

            return virtualUrl;

        } catch (IOException e) {
            System.err.println("❌ [本地主機儲存] 寫入硬碟失敗！原因: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    // 🧹 【實體檔案移除工具】
    public static void deleteLocalImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return; // 根本沒圖片，直接放行
        }

        try {
            // 1. 假設你的上傳圖片網址都長這樣：/uploads/xxxx.jpg
            // 我們要把前面的 "/uploads/" 剝離，取得純檔名
            if (imageUrl.startsWith("/uploads/")) {
                String fileName = imageUrl.substring("/uploads/".length());

                // 2. 對齊你當初在 Java 設定的實體儲存目錄（請根據你實際的實體資料夾路徑修改，例如 "./uploads/"）
                Path targetFolder = Paths.get("./uploads/").toAbsolutePath().normalize();
                Path fileToDestroy = targetFolder.resolve(fileName);

                // 3. 施展物理消滅魔法！
                if (Files.deleteIfExists(fileToDestroy)) {
                    System.out.println("🗑️ [物理粉碎成功] 已成功將硬碟上的實體圖片移除：" + fileToDestroy);
                } else {
                    System.out.println("⚠️ [物理粉碎失敗] 檔案可能早就不存在了：" + fileToDestroy);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ [硬碟操作異常] 刪除實體檔案時發生崩潰：" + e.getMessage());
        }
    }
}
