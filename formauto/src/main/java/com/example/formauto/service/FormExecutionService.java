package com.example.formauto.service;

import com.example.formauto.model.OptionDTO;
import com.example.formauto.model.QuestionDTO;
import com.example.formauto.util.WeightedRandom;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class FormExecutionService {

    private static final Logger log = LoggerFactory.getLogger(FormExecutionService.class);

    private static final String QUESTION_BLOCK = "div[role='listitem']";
    private final Random random = new Random();

    // --- BIẾN LƯU TRỮ TÊN CỦA NGƯỜI ĐANG ĐIỀN (Để đồng bộ với Email) ---
    private String currentSubmissionName = null;

    // --- DATA VIỆT NAM ---
    private static final String[] HO_VN = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"};
    private static final String[] LOT_NAM = {"Văn", "Hữu", "Đức", "Thành", "Công", "Minh", "Quốc", "Thế", "Gia", "Duy", "Quang", "Tuấn"};
    private static final String[] LOT_NU = {"Thị", "Ngọc", "Thu", "Mai", "Thanh", "Phương", "Khánh", "Hương", "Mỹ", "Diệu", "Ánh", "Kim"};
    private static final String[] TEN_NAM = {"Nam", "Hùng", "Tuấn", "Dũng", "Minh", "Hiếu", "Quân", "Long", "Phúc", "Khang", "Bảo", "Đạt", "Sơn", "Huy", "Hoàng", "Thắng"};
    private static final String[] TEN_NU = {"Linh", "Trang", "Lan", "Hương", "Mai", "Hoa", "Thảo", "Hà", "Yến", "Nga", "Vân", "Dung", "Tâm", "Huyền", "Vy", "Anh"};

    public void executeAutoFill(String url, List<QuestionDTO> configQuestions, int quantity) {
        if (configQuestions == null || configQuestions.isEmpty()) {
            log.error("❌ Danh sách câu hỏi bị TRỐNG, hủy auto-fill.");
            return;
        }

        log.info("▶ Bắt đầu auto-fill {} lần cho form {}", quantity, url);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(50));

            for (int i = 1; i <= quantity; i++) {

                log.info("🔁 Lần chạy {}/{}", i, quantity);

                // RESET TÊN NGƯỜI DÙNG MỚI CHO LẦN NÀY
                currentSubmissionName = null;

                BrowserContext context = browser.newContext();
                Page page = context.newPage();

                try {
                    page.navigate(url);
                    page.waitForLoadState(LoadState.NETWORKIDLE);

                    if (page.title().contains("Đăng nhập")) {
                        log.warn("⛔ Form yêu cầu đăng nhập – bỏ qua lần {}", i);
                        continue;
                    }

                    boolean success = processPageLoop(page, configQuestions);

                    if (success) {
                        log.info("✅ Lần {}: gửi thành công", i);
                    } else {
                        if (isSuccessPage(page)) {
                            log.info("✅ Lần {}: gửi thành công (check lại)", i);
                        } else {
                            log.warn("❌ Lần {}: gửi thất bại", i);
                        }
                    }

                    Thread.sleep(1000);

                } catch (Exception e) {
                    log.error("❌ Lỗi ở lần {}: {}", i, e.getMessage(), e);
                } finally {
                    context.close();
                }
            }

            browser.close();
            log.info("🏁 Đã hoàn thành auto-fill {} lần.", quantity);

        } catch (Exception e) {
            log.error("❌ Lỗi khởi tạo Playwright: {}", e.getMessage(), e);
        }
    }

    private boolean processPageLoop(Page page, List<QuestionDTO> configQuestions) {
        int currentPage = 1;

        while (currentPage <= 10) {

            page.keyboard().press("End");
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            // 1. Điền dữ liệu
            fillCurrentPage(page, configQuestions);

            // 2. Tìm nút điều hướng
            Locator btn = findNavigationButton(page);
            if (btn == null) return isSuccessPage(page);

            String btnText = btn.textContent().trim();
            boolean isSubmitBtn = isSubmitButton(btnText);

            log.debug("🖱️ Click nút [{}]", btnText);

            try {
                btn.click(new Locator.ClickOptions().setForce(true));
            } catch (Exception e) {
                btn.evaluate("e => e.click()");
            }

            try {
                page.waitForLoadState(LoadState.NETWORKIDLE);
                Thread.sleep(2000);
            } catch (Exception ignored) {}

            if (isSubmitBtn) return true;
            if (isSuccessPage(page)) return true;

            currentPage++;
        }

        return false;
    }

    private void fillCurrentPage(Page page, List<QuestionDTO> configQuestions) {
        Locator pageBlocks = page.locator(QUESTION_BLOCK);

        for (QuestionDTO qConfig : configQuestions) {

            Locator block = pageBlocks.nth(qConfig.getIndex());
            if (block.count() == 0 || !block.isVisible()) continue;

            String type = qConfig.getType();

            // RADIO
            if ("RADIO".equals(type)) {
                WeightedRandom randomizer = new WeightedRandom(qConfig.getOptions());
                OptionDTO selected = randomizer.next();

                if (selected != null) {
                    Locator option = block.locator("div[role='radio'][aria-label='" + selected.getText() + "']");
                    if (option.count() == 0) option = block.locator("div[role='radio'][data-value='" + selected.getValue() + "']");
                    if (option.isVisible()) option.click(new Locator.ClickOptions().setForce(true));
                }
            }
            // CHECKBOX
            else if ("CHECKBOX".equals(type)) {
                for (OptionDTO opt : qConfig.getOptions()) {
                    int chance = random.nextInt(100);
                    Locator option = block.locator("div[role='checkbox'][aria-label='" + opt.getText() + "']");
                    if (option.count() == 0) option = block.locator("div[role='checkbox'][data-value='" + opt.getValue() + "']");

                    if (option.isVisible()) {
                        if (opt.getText() == null || opt.getText().isEmpty()) continue;
                        boolean checked = "true".equals(option.getAttribute("aria-checked"));

                        if (chance < opt.getWeight()) {
                            if (!checked) option.click(new Locator.ClickOptions().setForce(true));
                        } else {
                            if (checked) option.click(new Locator.ClickOptions().setForce(true));
                        }
                    }
                }
            }
            // TEXT
            else if ("TEXT".equals(type)) {
                Locator input = block.locator("input:not([type='hidden']), textarea").first();
                if (input.isVisible() && input.inputValue().isEmpty()) {

                    String title = qConfig.getTitle().toLowerCase();
                    String valueToFill;

                    if (title.contains("tên") || title.contains("name")) {
                        if (currentSubmissionName == null) currentSubmissionName = generateRandomName();
                        valueToFill = currentSubmissionName;
                    }
                    else if (title.contains("mail")) {
                        if (currentSubmissionName == null) currentSubmissionName = generateRandomName();
                        valueToFill = generateEmailFromName(currentSubmissionName);
                    }
                    else {
                        valueToFill = generateOtherVietnameseData(title);
                    }

                    input.fill(valueToFill);
                }
            }
            // DATE
            else if ("DATE".equals(type)) {
                int year = 1980 + random.nextInt(26);
                int month = 1 + random.nextInt(12);
                int day = 1 + random.nextInt(28);

                String dateForInput = String.format("%d-%02d-%02d", year, month, day);
                String dateForTyping = String.format("%02d%02d%d", day, month, year);

                Locator input = block.locator("input[type='date']");
                if (input.count() > 0) {
                    input.fill(dateForInput);
                } else {
                    Locator textInput = block.locator("input[type='text']").first();
                    if (textInput.isVisible()) {
                        textInput.click();
                        block.page().keyboard().type(dateForTyping);
                    }
                }
            }
            // TIME
            else if ("TIME".equals(type)) {
                String h = String.format("%02d", random.nextInt(24));
                String m = String.format("%02d", random.nextInt(60));

                Locator timeInput = block.locator("input[type='time']");
                if (timeInput.count() > 0) {
                    timeInput.fill(h + ":" + m);
                } else {
                    Locator textInputs = block.locator("input[type='text']");
                    Locator numInputs = block.locator("input[type='number']");

                    if (textInputs.count() >= 2) {
                        textInputs.nth(0).fill(h);
                        textInputs.nth(1).fill(m);
                    }
                    else if (numInputs.count() >= 2) {
                        numInputs.nth(0).fill(h);
                        numInputs.nth(1).fill(m);
                    }
                }
            }
        }
    }

    private boolean isSubmitButton(String text) {
        String t = text.toLowerCase();
        return t.contains("gửi") || t.contains("submit") || t.contains("send");
    }

    private boolean isSuccessPage(Page page) {
        try {
            String content = page.content().toLowerCase();
            return content.contains("câu trả lời của bạn đã được ghi lại")
                    || content.contains("response has been recorded")
                    || content.contains("gửi phản hồi khác");
        } catch (Exception e) {
            return false;
        }
    }

    private Locator findNavigationButton(Page page) {

        List<String> banned = Arrays.asList("Xóa", "Clear", "Hủy", "Cancel", "Back", "Quay lại");
        List<String> priority = Arrays.asList("Gửi", "Submit", "Tiếp", "Next", "Send");

        Locator allButtons = page.locator("div[role='button']");
        int count = allButtons.count();

        Locator bestCandidate = null;

        for (int i = 0; i < count; i++) {
            Locator btn = allButtons.nth(i);
            if (!btn.isVisible()) continue;

            String text = btn.textContent().trim();

            boolean isBanned = false;
            for (String b : banned)
                if (text.toLowerCase().contains(b.toLowerCase()))
                    isBanned = true;

            if (isBanned) continue;

            for (String p : priority)
                if (text.equalsIgnoreCase(p) || text.toLowerCase().contains(p.toLowerCase()))
                    return btn;

            bestCandidate = btn;
        }

        if (bestCandidate != null) {
            log.debug("⚠️ Fallback: chọn nút [{}]", bestCandidate.textContent().trim());
        }

        return bestCandidate;
    }

    // --- DATA GENERATOR ---
    private String generateEmailFromName(String fullName) {
        String unaccented = removeAccents(fullName).toLowerCase();
        String[] parts = unaccented.split("\\s+");

        if (parts.length < 2)
            return unaccented + random.nextInt(9999) + "@gmail.com";

        String ho = parts[0];
        String ten = parts[parts.length - 1];
        return ten + ho + random.nextInt(100, 9999) + "@gmail.com";
    }

    private String generateOtherVietnameseData(String title) {
        String t = title.toLowerCase();

        if (t.contains("sđt") || t.contains("số điện thoại") || t.contains("phone")) {
            String[] dauSo = {"09", "03", "07", "08", "05"};
            return dauSo[random.nextInt(dauSo.length)]
                    + (10000000 + random.nextInt(89999999));
        }

        if (t.contains("địa chỉ") || t.contains("address"))
            return "Số " + random.nextInt(1, 200) + " đường Nguyễn Huệ, TP HCM";

        if (t.contains("tuổi") || t.contains("age"))
            return String.valueOf(random.nextInt(18, 40));

        return "Câu trả lời " + random.nextInt(100);
    }

    private String generateRandomName() {
        String ho = HO_VN[random.nextInt(HO_VN.length)];
        boolean isNam = random.nextBoolean();

        String lot = isNam
                ? LOT_NAM[random.nextInt(LOT_NAM.length)]
                : LOT_NU[random.nextInt(LOT_NU.length)];

        String ten = isNam
                ? TEN_NAM[random.nextInt(TEN_NAM.length)]
                : TEN_NU[random.nextInt(TEN_NU.length)];

        return ho + " " + lot + " " + ten;
    }

    private String removeAccents(String text) {
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        return pattern
                .matcher(nfd)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }
}
