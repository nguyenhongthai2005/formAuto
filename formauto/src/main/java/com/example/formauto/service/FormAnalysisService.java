package com.example.formauto.service;

import com.example.formauto.model.OptionDTO;
import com.example.formauto.model.QuestionDTO;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class FormAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FormAnalysisService.class);

    private static final String QUESTION_BLOCK = "div[role='listitem']";
    private static final String QUESTION_TITLE = "div[role='heading']";

    private static final String RADIO_OPTION = "div[role='radio']";
    private static final String CHECKBOX_OPTION = "div[role='checkbox']";
    private static final String TEXT_INPUT =
            "input[type='text']:not([style*='display: none']), input:not([type]), textarea";

    public List<QuestionDTO> analyzeForm(String url) {
        List<QuestionDTO> questions = new ArrayList<>();

        log.info("🔍 Bắt đầu phân tích Google Form: {}", url);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            page.navigate(url);

            try {
                page.waitForSelector(QUESTION_BLOCK,
                        new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception ignored) { }

            Locator blocks = page.locator(QUESTION_BLOCK);
            int count = blocks.count();

            for (int i = 0; i < count; i++) {
                Locator block = blocks.nth(i);

                if (block.locator(QUESTION_TITLE).count() == 0) continue;

                String title = block.locator(QUESTION_TITLE)
                        .first()
                        .textContent()
                        .trim();
                if (title.isEmpty()) continue;

                QuestionDTO q = new QuestionDTO();
                q.setIndex(i);
                q.setTitle(title);

                boolean isValidQuestion = false;

                // --- 1. ƯU TIÊN CHECK TIME TRƯỚC ---
                if (block.locator("input[type='time']").count() > 0 ||
                        block.locator("input[aria-label='Giờ']").count() > 0 ||
                        block.locator("input[aria-label='Hour']").count() > 0) {
                    q.setType("TIME");
                    isValidQuestion = true;
                }
                // --- 2. SAU ĐÓ CHECK CÁC LOẠI KHÁC ---
                else if (block.locator("input[type='date']").count() > 0) {
                    q.setType("DATE");
                    isValidQuestion = true;
                }
                else if (block.locator(RADIO_OPTION).count() > 0) {
                    q.setType("RADIO");
                    parseOptions(block.locator(RADIO_OPTION), q);
                    isValidQuestion = true;
                }
                else if (block.locator(CHECKBOX_OPTION).count() > 0) {
                    q.setType("CHECKBOX");
                    parseOptions(block.locator(CHECKBOX_OPTION), q);
                    isValidQuestion = true;
                }
                else if (block.locator(TEXT_INPUT).count() > 0) {
                    // Double-check cuối
                    if (q.getTitle().toLowerCase().contains("giờ") &&
                            q.getTitle().toLowerCase().contains("phút")) {
                        q.setType("TIME");
                    }
                    else if (q.getTitle().toLowerCase().contains("ngày")) {
                        q.setType("DATE");
                    }
                    else {
                        q.setType("TEXT");
                    }
                    isValidQuestion = true;
                }

                if (isValidQuestion) {
                    questions.add(q);
                    log.debug("✔ Nhận diện: '{}' [{}]", q.getTitle(), q.getType());
                }
            }
            browser.close();

        } catch (Exception e) {
            log.error("❌ Lỗi khi phân tích form {}: {}", url, e.getMessage(), e);
        }

        log.info("✅ Phân tích hoàn tất. Số câu hỏi hợp lệ: {}", questions.size());
        return questions;
    }

    private void parseOptions(Locator optionsLocator, QuestionDTO q) {
        int count = optionsLocator.count();
        for (int j = 0; j < count; j++) {
            Locator opt = optionsLocator.nth(j);
            String dataValue = opt.getAttribute("data-value");
            String text = opt.getAttribute("aria-label");

            if (text == null || text.trim().isEmpty()) {
                text = opt.textContent();
            }
            if (text == null) {
                text = "Lựa chọn " + (j + 1);
            }

            if (text.trim().equalsIgnoreCase("khác")
                    && j > 0
                    && q.getOptions().stream()
                    .anyMatch(o -> o.getText().equalsIgnoreCase("khác"))) {
                continue;
            }

            q.addOption(new OptionDTO(text.trim(), dataValue));
        }
    }
}
