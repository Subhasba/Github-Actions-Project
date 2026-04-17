package com.example.ragexample.controller;

import com.example.ragexample.service.IngestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final IngestService service;

    public ChatController(IngestService service) {
        this.service = service;
    }

    @GetMapping
    public Object ask(@RequestParam String query) {
        return service.query(query);
    }
}