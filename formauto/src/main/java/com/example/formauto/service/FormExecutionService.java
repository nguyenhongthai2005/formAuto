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

    // --- DATA FEEDBACK ---
    private static final String[] ANS_GOP_Y = {
            "Mình thấy dịch vụ rất tốt, hy vọng thương hiệu sẽ giữ vững phong độ.",
            "Nên có thêm nhiều chương trình ưu đãi hoặc coupon cho khách hàng thân thiết.",
            "Sản phẩm chất lượng tốt, mong cửa hàng cập nhật thêm nhiều hương vị mới.",
            "Mọi thứ hiện tại đều rất ổn, quy trình mua sắm rất nhanh chóng và tiện lợi.",
            "Cần đẩy mạnh dịch vụ giao hàng nhanh hơn vào các khung giờ cao điểm."
    };

    private static final String[] ANS_LY_DO = {
            "Vì chất lượng sản phẩm luôn ổn định, hương vị thơm ngon đặc trưng.",
            "Thái độ phục vụ của nhân viên cực kỳ nhiệt tình, chu đáo và thân thiện.",
            "Thương hiệu uy tín, quy trình đóng gói sạch sẽ và đảm bảo vệ sinh.",
            "Mức giá hợp lý, nhiều chương trình khuyến mãi hấp dẫn và giao hàng siêu tốc.",
            "Trải nghiệm mua sắm online rất mượt mà, nhân viên hỗ trợ nhiệt tình."
    };

    private static final String[] ANS_CHUNG = {
            "Rất hài lòng với trải nghiệm sản phẩm và dịch vụ tại đây.",
            "Sản phẩm dùng rất ổn, sẽ giới thiệu cho bạn bè và người thân cùng sử dụng.",
            "Đánh giá 5 sao cho chất lượng sản phẩm và thái độ của đội ngũ.",
            "Mọi thứ đều hoàn hảo, cảm ơn thương hiệu rất nhiều."
    };

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
                    page.waitForLoadState(LoadState.LOAD);
                    try { page.waitForSelector(QUESTION_BLOCK, new Page.WaitForSelectorOptions().setTimeout(5000)); } catch (Exception ignored) {}

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

            // 1. Điền dữ liệu cho trang hiện tại
            fillCurrentPage(page, configQuestions, currentPage);

            // 2. Tìm nút điều hướng
            Locator btn = findNavigationButton(page);
            if (btn == null) return isSuccessPage(page);

            String btnText = btn.textContent().trim();
            boolean isSubmitBtn = isSubmitButton(btnText);

            log.debug("🖱️ Click nút [{}] trên trang {}", btnText, currentPage);

            String firstQTitleBefore = getFirstQuestionTitle(page);

            try {
                btn.click(new Locator.ClickOptions().setForce(true));
            } catch (Exception e) {
                btn.evaluate("e => e.click()");
            }

            try {
                page.waitForLoadState(LoadState.LOAD);
                Thread.sleep(2000);
                page.waitForSelector(QUESTION_BLOCK, new Page.WaitForSelectorOptions().setTimeout(3000));
            } catch (Exception ignored) {}

            if (isSubmitBtn) return true;
            if (isSuccessPage(page)) return true;

            String firstQTitleAfter = getFirstQuestionTitle(page);
            if (firstQTitleBefore != null && firstQTitleBefore.equals(firstQTitleAfter)) {
                log.error("❌ Không thể chuyển trang từ trang {}. Có thể do lỗi validation thiếu trường bắt buộc.", currentPage);
                return false;
            }

            currentPage++;
        }

        return false;
    }

    private String getFirstQuestionTitle(Page page) {
        try {
            Locator blocks = page.locator(QUESTION_BLOCK);
            if (blocks.count() > 0) {
                return blocks.nth(0).locator("div[role='heading']").first().textContent().trim();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void fillCurrentPage(Page page, List<QuestionDTO> configQuestions, int currentPage) {
        Locator pageBlocks = page.locator(QUESTION_BLOCK);

        for (QuestionDTO qConfig : configQuestions) {
            // Lọc các câu hỏi thuộc trang hiện tại (mặc định <= 0 là trang 1)
            int qPageIndex = qConfig.getPageIndex() <= 0 ? 1 : qConfig.getPageIndex();
            if (qPageIndex != currentPage) continue;

            int localIndex = qConfig.getIndex();
            if (localIndex < 0 || localIndex >= pageBlocks.count()) {
                log.warn("⚠️ Index cục bộ {} vượt quá số lượng block {} trên trang {}", localIndex, pageBlocks.count(), currentPage);
                continue;
            }

            Locator block = pageBlocks.nth(localIndex);
            if (block.count() == 0 || !block.isVisible()) continue;

            String type = qConfig.getType();

            // RADIO
            if ("RADIO".equals(type)) {
                WeightedRandom randomizer = new WeightedRandom(qConfig.getOptions());
                OptionDTO selected = randomizer.next();

                if (selected != null) {
                    int optIndex = selected.getDomIndex();
                    if (optIndex >= 0) {
                        Locator option = block.locator("div[role='radio']").nth(optIndex);
                        if (option.isVisible()) {
                            option.click(new Locator.ClickOptions().setForce(true));
                            if ("__other_option__".equals(selected.getValue())) {
                                Locator otherInput = block.locator("input[type='text']:not([type='hidden'])");
                                if (otherInput.count() > 0 && otherInput.first().isVisible()) {
                                    otherInput.first().fill("Lý do khác " + random.nextInt(100));
                                }
                            }
                        }
                    }
                }
            }
            // CHECKBOX
            else if ("CHECKBOX".equals(type)) {
                List<OptionDTO> options = qConfig.getOptions();
                Locator checkLocators = block.locator("div[role='checkbox']");
                
                boolean clickedAny = false;
                for (int i = 0; i < options.size(); i++) {
                    OptionDTO opt = options.get(i);
                    int chance = random.nextInt(100);
                    
                    int optIndex = opt.getDomIndex();
                    if (optIndex >= checkLocators.count()) break;
                    Locator option = checkLocators.nth(optIndex);

                    if (option.isVisible()) {
                        boolean checked = "true".equals(option.getAttribute("aria-checked"));
                        if (chance < opt.getWeight()) {
                            clickedAny = true; // Sẽ được check
                            if (!checked) {
                                option.click(new Locator.ClickOptions().setForce(true));
                                if ("__other_option__".equals(opt.getValue())) {
                                    Locator otherInput = block.locator("input[type='text']:not([type='hidden'])");
                                    if (otherInput.count() > 0 && otherInput.first().isVisible()) {
                                        otherInput.first().fill("Lý do khác " + random.nextInt(100));
                                    }
                                }
                            }
                        } else {
                            if (checked) {
                                option.click(new Locator.ClickOptions().setForce(true)); // Bỏ check
                            }
                        }
                    }
                }
                
                if (!clickedAny && options.size() > 0) {
                    java.util.List<OptionDTO> validOptions = options.stream().filter(o -> o.getWeight() > 0).collect(java.util.stream.Collectors.toList());
                    
                    if (!validOptions.isEmpty()) {
                        OptionDTO fallbackOpt = validOptions.get(random.nextInt(validOptions.size()));
                        int optIndex = fallbackOpt.getDomIndex();
                        if (optIndex < checkLocators.count()) {
                            Locator option = checkLocators.nth(optIndex);
                            if (option.isVisible()) {
                                boolean checked = "true".equals(option.getAttribute("aria-checked"));
                                if (!checked) {
                                    option.click(new Locator.ClickOptions().setForce(true));
                                    if ("__other_option__".equals(fallbackOpt.getValue())) {
                                        Locator otherInput = block.locator("input[type='text']:not([type='hidden'])");
                                        if (otherInput.count() > 0 && otherInput.first().isVisible()) {
                                            otherInput.first().fill("Lý do khác " + random.nextInt(100));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // TEXT
            else if ("TEXT".equals(type)) {
                Locator input = block.locator("input[type='text']:not([style*='display: none']), input:not([type]), textarea").first();
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

        List<String> banned = Arrays.asList("xoa", "clear", "huy", "cancel", "back", "quay lai");
        List<String> priority = Arrays.asList("gui", "submit", "tiep", "next", "send", "tiep tuc");

        Locator allButtons = page.locator("div[role='button']");
        int count = allButtons.count();

        Locator bestCandidate = null;

        for (int i = 0; i < count; i++) {
            Locator btn = allButtons.nth(i);
            if (!btn.isVisible()) continue;

            String text = btn.textContent().trim();
            String normText = removeAccents(text).toLowerCase();

            boolean isBanned = false;
            for (String b : banned)
                if (normText.contains(b))
                    isBanned = true;

            if (isBanned) continue;

            for (String p : priority)
                if (normText.equals(p) || normText.contains(p))
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

        if (t.contains("góp ý") || t.contains("cải thiện") || t.contains("nhận xét") || t.contains("feedback") || t.contains("ý kiến") || t.contains("improve")) {
            return "Không";
        }

        if (t.contains("yếu tố") || t.contains("lý do") || t.contains("tiếp tục") || t.contains("tương lai") || t.contains("lựa chọn") || t.contains("why") || t.contains("reason")) {
            return ANS_LY_DO[random.nextInt(ANS_LY_DO.length)];
        }

        return ANS_CHUNG[random.nextInt(ANS_CHUNG.length)];
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
