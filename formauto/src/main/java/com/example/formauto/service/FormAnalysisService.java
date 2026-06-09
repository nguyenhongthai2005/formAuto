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

            int currentPageIndex = 1;
            boolean hasNextPage = true;
            String lastPageFirstQuestionTitle = "";

            while (hasNextPage && currentPageIndex <= 10) {
                log.info("📖 Đang phân tích trang {}...", currentPageIndex);

                try {
                    page.waitForSelector(QUESTION_BLOCK,
                            new Page.WaitForSelectorOptions().setTimeout(5000));
                } catch (Exception ignored) { }

                Locator blocks = page.locator(QUESTION_BLOCK);
                int count = blocks.count();
                int localAdded = 0;

                // Safety Check: Kiểm tra xem trang có thực sự thay đổi không để tránh lặp vô hạn
                String firstQuestionTitleOnNewPage = "";
                for (int i = 0; i < count; i++) {
                    Locator block = blocks.nth(i);
                    if (block.isVisible() && block.locator(QUESTION_TITLE).count() > 0) {
                        firstQuestionTitleOnNewPage = block.locator(QUESTION_TITLE).first().textContent().trim();
                        break;
                    }
                }

                if (currentPageIndex > 1 && !firstQuestionTitleOnNewPage.isEmpty() && firstQuestionTitleOnNewPage.equals(lastPageFirstQuestionTitle)) {
                    log.warn("⚠️ Không thể chuyển trang (có thể do lỗi validation hoặc đã hết trang). Dừng phân tích tại đây.");
                    break;
                }
                lastPageFirstQuestionTitle = firstQuestionTitleOnNewPage;

                int localQuestionIndex = 0;
                for (int i = 0; i < count; i++) {
                    Locator block = blocks.nth(i);
                    if (!block.isVisible()) continue;

                    if (block.locator(QUESTION_TITLE).count() == 0) continue;

                    String title = block.locator(QUESTION_TITLE)
                            .first()
                            .textContent()
                            .trim();
                    if (title.isEmpty()) continue;

                    QuestionDTO q = new QuestionDTO();
                    q.setPageIndex(currentPageIndex);
                    q.setIndex(i); // Chỉ số vật lý của block trên trang này
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
                        localQuestionIndex++;
                        localAdded++;
                        log.debug("✔ Trang {}: Nhận diện: '{}' [{}]", currentPageIndex, q.getTitle(), q.getType());
                    }
                }

                log.info("Trang {} có {} câu hỏi hợp lệ.", currentPageIndex, localAdded);

                // 2. Điền dummy data cho trang hiện tại để có thể ấn "Next"
                fillDummyAnswersForCurrentPage(page);

                // 3. Tìm nút "Tiếp" (Next)
                Locator nextBtn = findNextButton(page);
                if (nextBtn != null) {
                    String btnText = nextBtn.textContent().trim();
                    log.info("👉 Phát hiện nút [{}]. Tiến hành click để chuyển trang.", btnText);
                    try {
                        nextBtn.click(new Locator.ClickOptions().setForce(true));
                    } catch (Exception e) {
                        nextBtn.evaluate("e => e.click()");
                    }

                    // Đợi trang mới load
                    try {
                        page.waitForLoadState(com.microsoft.playwright.options.LoadState.LOAD);
                        Thread.sleep(2000);
                    } catch (Exception ignored) {}

                    currentPageIndex++;
                } else {
                    log.info("🏁 Không thấy nút Tiếp/Next hoặc đã tới trang cuối cùng.");
                    hasNextPage = false;
                }
            }
            browser.close();

        } catch (Exception e) {
            log.error("❌ Lỗi khi phân tích form {}: {}", url, e.getMessage(), e);
        }

        log.info("✅ Phân tích hoàn tất. Tổng số câu hỏi hợp lệ: {}", questions.size());
        return questions;
    }

    private void fillDummyAnswersForCurrentPage(Page page) {
        Locator blocks = page.locator(QUESTION_BLOCK);
        int count = blocks.count();
        log.info("✍️ Đang điền dữ liệu giả cho {} block để mở khóa nút Next...", count);
        for (int i = 0; i < count; i++) {
            Locator block = blocks.nth(i);
            if (!block.isVisible()) continue;

            try {
                // 1. Radio
                if (block.locator(RADIO_OPTION).count() > 0) {
                    Locator opt = block.locator(RADIO_OPTION).first();
                    if (opt.isVisible()) opt.click(new Locator.ClickOptions().setForce(true));
                }
                // 2. Checkbox
                else if (block.locator(CHECKBOX_OPTION).count() > 0) {
                    Locator opt = block.locator(CHECKBOX_OPTION).first();
                    if (opt.isVisible()) opt.click(new Locator.ClickOptions().setForce(true));
                }
                // 3. Date
                else if (block.locator("input[type='date']").count() > 0) {
                    Locator input = block.locator("input[type='date']").first();
                    if (input.isVisible()) input.fill("2026-05-28");
                }
                // 4. Time
                else if (block.locator("input[type='time']").count() > 0 ||
                        block.locator("input[aria-label='Giờ']").count() > 0 ||
                        block.locator("input[aria-label='Hour']").count() > 0) {
                    Locator timeInput = block.locator("input[type='time']");
                    if (timeInput.count() > 0 && timeInput.first().isVisible()) {
                        timeInput.first().fill("12:00");
                    } else {
                        Locator textInputs = block.locator("input[type='text']");
                        Locator numInputs = block.locator("input[type='number']");
                        if (textInputs.count() >= 2) {
                            textInputs.nth(0).fill("12");
                            textInputs.nth(1).fill("00");
                        } else if (numInputs.count() >= 2) {
                            numInputs.nth(0).fill("12");
                            numInputs.nth(1).fill("00");
                        }
                    }
                }
                // 5. Text
                else if (block.locator(TEXT_INPUT).count() > 0) {
                    Locator input = block.locator(TEXT_INPUT).first();
                    if (input.isVisible()) input.fill("Tự động điền");
                }
            } catch (Exception e) {
                log.warn("⚠️ Không thể điền dữ liệu giả cho block thứ {}: {}", i, e.getMessage());
            }
        }
    }

    private Locator findNextButton(Page page) {
        List<String> priority = java.util.Arrays.asList("Tiếp", "Next", "Tiếp tục");
        Locator allButtons = page.locator("div[role='button']");
        int count = allButtons.count();

        for (int i = 0; i < count; i++) {
            Locator btn = allButtons.nth(i);
            if (!btn.isVisible()) continue;

            String text = btn.textContent().trim();
            for (String p : priority) {
                if (text.equalsIgnoreCase(p) || text.toLowerCase().contains(p.toLowerCase())) {
                    return btn;
                }
            }
        }
        return null;
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

            q.addOption(new OptionDTO(text.trim(), dataValue, j));
        }
    }
}
