package com.example.formauto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Khi vào trang chủ localhost:8080/
    @GetMapping("/")
    public String index() {
        // Trả về file index.html
        return "index";
    }
}