package com.example.formauto.model;

public class FormRequest {
    private String formUrl;

    // Constructor không tham số
    public FormRequest() {
    }

    // Constructor có tham số
    public FormRequest(String formUrl) {
        this.formUrl = formUrl;
    }

    // Getters và Setters
    public String getFormUrl() {
        return formUrl;
    }

    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }
}