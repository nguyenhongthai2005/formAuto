package com.example.formauto.model;

public class FormRequest {
    private String formUrl;
    private int numSubmissions;

    public FormRequest() {
    }

    public FormRequest(String formUrl, int numSubmissions) {
        this.formUrl = formUrl;
        this.numSubmissions = numSubmissions;
    }

    // Getters và Setters
    public String getFormUrl() {
        return formUrl;
    }

    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }

    public int getNumSubmissions() {
        return numSubmissions;
    }

    public void setNumSubmissions(int numSubmissions) {
        this.numSubmissions = numSubmissions;
    }
}