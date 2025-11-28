package com.example.formauto.controller;

import com.example.formauto.model.FormRequest;
import com.example.formauto.service.FormAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/form")
public class FormController {

    private final FormAnalysisService formAnalysisService;

    // Dependency Injection
    public FormController(FormAnalysisService formAnalysisService) {
        this.formAnalysisService = formAnalysisService;
    }

    /**
     * Endpoint để bắt đầu quá trình tự động điền Form (Spam/Clone).
     * POST /api/form/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<String> startFormSubmission(@RequestBody FormRequest request) {

        if (request.getFormUrl() == null || request.getFormUrl().trim().isEmpty() || request.getNumSubmissions() <= 0) {
            return ResponseEntity.badRequest().body("URL Form hoặc Số lần gửi không hợp lệ.");
        }

        System.out.println("-> Nhận yêu cầu: URL=" + request.getFormUrl() + ", Số lần=" + request.getNumSubmissions());

        try {
            // Logic tự động hóa được thực hiện trong Service
            formAnalysisService.analyzeAndSubmit(request.getFormUrl(), request.getNumSubmissions());

            return ResponseEntity.ok("Quá trình tự động điền " + request.getNumSubmissions() + " lần đã hoàn tất thành công.");

        } catch (RuntimeException e) {
            System.err.println("Lỗi trong quá trình tự động hóa: " + e.getMessage());
            // Trả về lỗi 500 nếu có bất kỳ RuntimeException nào xảy ra
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi Backend: " + e.getMessage());
        }
    }
}