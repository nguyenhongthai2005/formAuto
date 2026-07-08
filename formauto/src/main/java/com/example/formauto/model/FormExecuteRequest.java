package com.example.formauto.model;

import java.util.List;

public class FormExecuteRequest {
    private String formUrl;
    private int numSubmissions;
    private List<QuestionDTO> questions; // Danh sách câu hỏi kèm trọng số %
    private boolean fastMode;
    private boolean useCakeShopData;

    // Getters & Setters
    public String getFormUrl() { return formUrl; }
    public void setFormUrl(String formUrl) { this.formUrl = formUrl; }

    public int getNumSubmissions() { return numSubmissions; }
    public void setNumSubmissions(int numSubmissions) { this.numSubmissions = numSubmissions; }

    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) { this.questions = questions; }

    public boolean isFastMode() { return fastMode; }
    public void setFastMode(boolean fastMode) { this.fastMode = fastMode; }

    public boolean isUseCakeShopData() { return useCakeShopData; }
    public void setUseCakeShopData(boolean useCakeShopData) { this.useCakeShopData = useCakeShopData; }
}