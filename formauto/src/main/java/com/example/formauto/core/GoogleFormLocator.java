package com.example.formauto.core;

public class GoogleFormLocator {
    // Block bao quanh 1 câu hỏi
    public static final String QUESTION_BLOCK = "div[role='listitem']";

    // Tiêu đề câu hỏi (thường nằm trong role='heading')
    public static final String QUESTION_TITLE = "div[role='heading']";

    // Option dạng Radio Button
    public static final String RADIO_OPTION = "div[role='radio']";

    // Nút Gửi (Submit) - Tìm theo role button và check text sau
    public static final String SUBMIT_BUTTON_XPATH = "//div[@role='button']//span[contains(text(),'Gửi') or contains(text(),'Submit')]";

    // Link "Gửi phản hồi khác" sau khi submit xong
    public static final String RELOAD_LINK_XPATH = "//a[contains(@href, 'viewform')]";
}