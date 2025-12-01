package com.example.formauto.controller;

import com.example.formauto.model.FormExecuteRequest;
import com.example.formauto.model.FormRequest;
import com.example.formauto.model.QuestionDTO;
import com.example.formauto.service.FormAnalysisService;
import com.example.formauto.service.FormExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form")
public class FormController {

    private final FormAnalysisService formAnalysisService;
    private final FormExecutionService formExecutionService;

    // Inject cả 2 service: 1 cái để quét, 1 cái để chạy
    public FormController(FormAnalysisService formAnalysisService, FormExecutionService formExecutionService) {
        this.formAnalysisService = formAnalysisService;
        this.formExecutionService = formExecutionService;
    }

    /**
     * BƯỚC 1: Phân tích Form
     * POST /api/form/analyze
     * Input: {"formUrl": "..."}
     * Output: JSON danh sách câu hỏi
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeForm(@RequestBody FormRequest request) {
        if (request.getFormUrl() == null || request.getFormUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("URL Form không hợp lệ.");
        }

        try {
            System.out.println("-> Đang phân tích URL: " + request.getFormUrl());
            // Gọi Service quét form trả về List QuestionDTO
            List<QuestionDTO> questions = formAnalysisService.analyzeForm(request.getFormUrl());

            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi phân tích form: " + e.getMessage());
        }
    }

    /**
     * BƯỚC 2: Thực thi (Chạy Auto)
     * POST /api/form/execute
     * Input: JSON chứa URL, số lần chạy, và LIST câu hỏi kèm % trọng số
     */
    @PostMapping("/execute")
    public ResponseEntity<String> executeForm(@RequestBody FormExecuteRequest request) {
        if (request.getFormUrl() == null || request.getNumSubmissions() <= 0) {
            return ResponseEntity.badRequest().body("Thông tin không hợp lệ.");
        }

        System.out.println("-> Bắt đầu chạy tool. Số lần: " + request.getNumSubmissions());

        try {
            // Gọi Service chạy vòng lặp điền form
            formExecutionService.executeAutoFill(
                    request.getFormUrl(),
                    request.getQuestions(),
                    request.getNumSubmissions()
            );

            return ResponseEntity.ok("Đã chạy xong " + request.getNumSubmissions() + " lần.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi trong quá trình chạy: " + e.getMessage());
        }
    }
}