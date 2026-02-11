package com.fuxi.script.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class UploadController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/upload/image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("image") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("code", 1);
            result.put("msg", "上传文件为空");
            return result;
        }

        try {
            // Ensure upload directory exists
            File uploadDir = new File(System.getProperty("user.dir"), UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path path = Paths.get(uploadDir.getAbsolutePath(), newFilename);
            Files.write(path, file.getBytes());

            // Return success response with URL
            result.put("code", 0);
            result.put("msg", "上传成功");
            Map<String, String> data = new HashMap<>();
            data.put("url", "/uploads/" + newFilename);
            data.put("filename", newFilename);
            result.put("data", data);

        } catch (IOException e) {
            e.printStackTrace();
            result.put("code", 1);
            result.put("msg", "上传失败: " + e.getMessage());
        }

        return result;
    }
}
