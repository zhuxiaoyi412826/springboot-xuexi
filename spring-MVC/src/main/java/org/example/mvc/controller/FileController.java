package org.example.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.example.mvc.common.Result;
import org.example.mvc.model.FileMeta;
import org.example.mvc.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传/下载：MultipartFile、多文件、@RequestPart(JSON + 文件)、原生 Part、
 * ResponseEntity&lt;Resource&gt; 下载、byte[] 下载、StreamingResponseBody 流式下载。
 */
@Slf4j
@Controller
@RequestMapping("/mvc/file")
public class FileController {

    @Value("${mvc.upload-dir:./uploads}")
    private String uploadDir;

    /** 1. 单文件上传（multipart/form-data） */
    @PostMapping("/upload")
    @ResponseBody
    public Result upload(@RequestPart("file") MultipartFile file) throws IOException {
        Path target = save(file);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("originalName", file.getOriginalFilename());
        data.put("size", file.getSize());
        data.put("contentType", file.getContentType());
        data.put("savedAs", target.toString());
        return Result.ok(data);
    }

    /** 2. 多文件上传：同名多份 */
    @PostMapping("/upload-multi")
    @ResponseBody
    public Result uploadMulti(@RequestPart("files") List<MultipartFile> files) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MultipartFile file : files) {
            Path target = save(file);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", file.getOriginalFilename());
            m.put("size", file.getSize());
            m.put("savedAs", target.toString());
            list.add(m);
        }
        return Result.ok(list);
    }

    /** 3. 文件 + JSON 元数据一起提交（meta 部分需 Content-Type: application/json） */
    @PostMapping("/upload-with-meta")
    @ResponseBody
    public Result uploadWithMeta(@RequestPart("meta") FileMeta meta,
                                 @RequestPart("file") MultipartFile file) throws IOException {
        Path target = save(file);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("meta", meta);
        data.put("file", target.getFileName().toString());
        return Result.ok(data);
    }

    /** 4. 原生 Servlet 3.0 Part API（@RequestPart 的底层就是它） */
    @PostMapping("/upload-part")
    @ResponseBody
    public Result uploadPart(HttpServletRequest request) throws Exception {
        Part part = request.getPart("file");
        String submitted = part.getSubmittedFileName();
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(timestamp() + "_" + submitted);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target);
        }
        return Result.ok(Map.of("submittedFileName", submitted,
                "size", part.getSize(),
                "savedAs", target.toString()));
    }

    /** 5. 下载方式一：ResponseEntity<Resource>（最常用，支持断点续传语义） */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String name) {
        Path path = Paths.get(uploadDir).resolve(name).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** 6. 下载方式二：byte[] 整体读入内存（只适合小文件） */
    @GetMapping("/download-bytes")
    public ResponseEntity<byte[]> downloadBytes(@RequestParam String name) throws IOException {
        Path path = Paths.get(uploadDir).resolve(name).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(Files.readAllBytes(path));
    }

    /** 7. 下载方式三：StreamingResponseBody 边读边写（大文件友好） */
    @GetMapping("/download-stream")
    public ResponseEntity<StreamingResponseBody> downloadStream(@RequestParam String name) {
        Path path = Paths.get(uploadDir).resolve(name).normalize();
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        StreamingResponseBody body = out -> Files.copy(path, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    /** 8. 列出已上传文件 */
    @GetMapping("/list")
    @ResponseBody
    public Result list() throws IOException {
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) {
            return Result.ok(List.of());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.forEach(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", p.getFileName().toString());
                try {
                    m.put("size", Files.size(p));
                } catch (IOException ignored) {
                }
                list.add(m);
            });
        }
        return Result.ok(list);
    }

    /** 9. 演示：把上传的文本内容解析成对象（配合自定义消息转换器思路） */
    @PostMapping(value = "/upload-text", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public Result uploadText(@org.springframework.web.bind.annotation.RequestBody String text) {
        User user = new User();
        String[] arr = text.split(":");
        user.setUsername(arr.length > 0 ? arr[0] : "");
        return Result.ok(user);
    }

    private Path save(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String original = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "unknown.bin";
        Path target = dir.resolve(timestamp() + "_" + original);
        file.transferTo(target);
        log.info("保存上传文件 -> {}", target);
        return target;
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
