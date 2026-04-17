package com.example.ragexample.controller;

import com.example.ragexample.service.IngestService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final IngestService service;

    public UploadController(IngestService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        service.processPdf(file);
        return "Uploaded successfully!";
    }
}