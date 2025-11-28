package com.example.formauto.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Random;

@Service
public class FormAnalysisService {

    private static final String QUESTION_CONTAINER_SELECTOR = "div[role='listitem']";
    private final Random random = new Random();

    public void analyzeAndSubmit(String formUrl, int numSubmissions) {
        try (Playwright playwright = Playwright.create()) {
            // Giữ nguyên headless=false để bạn quan sát
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(100));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            System.out.println("--- 🚀 Đang truy cập Form: " + formUrl + " ---");
            page.setDefaultNavigationTimeout(60000);
            page.navigate(formUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Chờ load câu hỏi
            try {
                page.waitForSelector(QUESTION_CONTAINER_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(15000));
            } catch (Exception e) {
                System.out.println("⚠️ Không thấy câu hỏi nào. Đang tìm nút điều hướng...");
            }

            for (int i = 1; i <= numSubmissions; i++) {
                System.out.println("\n🔄 --- Bắt đầu lần gửi thứ: " + i + " ---");

                if (i > 1) {
                    page.navigate(formUrl);
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                }

                boolean formCompleted = processMultiPageForm(page);

                if (formCompleted) {
                    System.out.println("✅ Hoàn thành gửi lần " + i);
                } else {
                    System.err.println("❌ Thất bại lần " + i);
                }

                Thread.sleep(1000 + random.nextInt(2000));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Tiến trình bị gián đoạn.", e);
        } catch (PlaywrightException e) {
            throw new RuntimeException("Lỗi Playwright: " + e.getMessage(), e);
        }
    }

    private boolean processMultiPageForm(Page page) {
        int maxPages = 10;
        int currentPage = 1;

        while (currentPage <= maxPages) {
            fillCurrentPage(page);

            // Tìm nút Gửi/Tiếp bằng logic MỚI
            Locator submitBtn = findNavigationButton(page);

            if (submitBtn != null) { // Đã tìm thấy nút
                String btnText = submitBtn.textContent();
                System.out.println("   🖱️ Tìm thấy nút: [" + btnText.trim() + "]");

                // Cuộn tới nút và click
                try {
                    submitBtn.scrollIntoViewIfNeeded();
                    // Click force=true để bỏ qua các lớp phủ nếu có
                    submitBtn.click(new Locator.ClickOptions().setForce(true));
                } catch (Exception e) {
                    System.err.println("   ⚠️ Lỗi khi click nút: " + e.getMessage());
                    // Thử fallback click bằng JS nếu click thường thất bại
                    submitBtn.evaluate("element => element.click()");
                }

                // Chờ trang xử lý
                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
                } catch (Exception e) {}

                // Kiểm tra xem nút vừa bấm có phải là Gửi/Submit không
                // Regex tìm Gửi, Submit (không phân biệt hoa thường)
                if (btnText != null && Pattern.compile("Gửi|Submit|Send", Pattern.CASE_INSENSITIVE).matcher(btnText).find()) {
                    return true;
                }
            } else {
                System.err.println("   ⚠️ Không tìm thấy nút Gửi/Tiếp nào khả dụng!");
                // Chụp ảnh màn hình để debug nếu cần
                // page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("debug_missing_btn.png")));
                return false;
            }
            currentPage++;
        }
        return false;
    }

    private void fillCurrentPage(Page page) {
        Locator questions = page.locator(QUESTION_CONTAINER_SELECTOR);
        int count = questions.count();
        System.out.println("   📝 Điền " + count + " câu hỏi...");

        for (int i = 0; i < count; i++) {
            Locator question = questions.nth(i);
            try {
                if (isRadioQuestion(question)) fillRadio(question);
                else if (isCheckboxQuestion(question)) fillCheckbox(question);
                else if (isTextQuestion(question)) fillText(question);
            } catch (Exception e) {}
        }
    }

    /**
     * LOGIC TÌM NÚT "VÉT CẠN" (BRUTE FORCE)
     * Thử nhiều cách khác nhau để đảm bảo tìm ra nút.
     */
    private Locator findNavigationButton(Page page) {
        // Cách 1: Tìm bằng ngữ nghĩa (GetByRole) - Chính xác nhất
        // Thử danh sách các từ khóa phổ biến
        List<String> keywords = Arrays.asList("Gửi", "Submit", "Tiếp", "Next", "Sau", "Next", "Send");

        for (String kw : keywords) {
            // Tìm nút (button) có tên chứa từ khóa (exact=false cho phép tìm gần đúng)
            Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(kw).setExact(false));
            if (btn.isVisible()) return btn.first();
        }

        // Cách 2: Tìm nút div[role='button'] có chứa text cụ thể (Fallback)
        // Dùng Regex Pattern để tìm text
        Pattern pattern = Pattern.compile("Gửi|Submit|Tiếp|Next|Sau", Pattern.CASE_INSENSITIVE);
        Locator divBtn = page.locator("div[role='button']").filter(new Locator.FilterOptions().setHasText(pattern)).first();
        if (divBtn.isVisible()) return divBtn;

        // Cách 3: NẾU VẪN KHÔNG THẤY -> Lấy nút có role='button' nằm ở CUỐI CÙNG trang
        // (Trong Google Forms, nút Gửi/Tiếp luôn nằm dưới cùng)
        Locator allButtons = page.locator("div[role='button']");
        int count = allButtons.count();
        if (count > 0) {
            // Quét ngược từ dưới lên
            for (int i = count - 1; i >= 0; i--) {
                Locator btn = allButtons.nth(i);
                if (btn.isVisible()) {
                    String text = btn.textContent().trim();
                    // Loại bỏ các nút rác như "Xóa câu trả lời" (Clear form)
                    if (!text.isEmpty() && !text.contains("Xóa") && !text.contains("Clear")) {
                        System.out.println("   ⚠️ Dùng phương án dự phòng: Chọn nút cuối cùng [" + text + "]");
                        return btn;
                    }
                }
            }
        }

        return null;
    }

    // --- CÁC HÀM NHẬN DIỆN & ĐIỀN (GIỮ NGUYÊN) ---
    private boolean isRadioQuestion(Locator q) {
        return q.locator("div[role='radio']").count() > 0;
    }

    private boolean isCheckboxQuestion(Locator q) {
        return q.locator("div[role='checkbox']").count() > 0;
    }

    private boolean isTextQuestion(Locator q) {
        return q.locator("input:not([type='hidden']), textarea").count() > 0;
    }

    private void fillRadio(Locator q) {
        Locator options = q.locator("div[role='radio']");
        if (options.count() > 0) {
            options.nth(random.nextInt(options.count())).click(new Locator.ClickOptions().setForce(true));
        }
    }

    private void fillCheckbox(Locator q) {
        Locator options = q.locator("div[role='checkbox']");
        int count = options.count();
        if (count > 0) {
            int num = 1 + random.nextInt(Math.min(count, 2));
            for (int k = 0; k < num; k++) {
                Locator opt = options.nth(random.nextInt(count));
                if (!"true".equals(opt.getAttribute("aria-checked"))) opt.click(new Locator.ClickOptions().setForce(true));
            }
        }
    }

    private void fillText(Locator q) {
        Locator input = q.locator("input:not([type='hidden']), textarea").first();
        if (input.isVisible()) {
            if ("email".equals(input.getAttribute("type"))) input.fill("auto" + random.nextInt(9999) + "@gmail.com");
            else input.fill("Auto " + random.nextInt(1000));
        }
    }
}