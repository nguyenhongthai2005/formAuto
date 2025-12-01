package com.example.formauto.model;

import java.util.List;

public class FormExecuteRequest {
    private String formUrl;
    private int numSubmissions;
    private List<QuestionDTO> questions; // Danh sách câu hỏi kèm trọng số %

    // Getters & Setters
    public String getFormUrl() { return formUrl; }
    public void setFormUrl(String formUrl) { this.formUrl = formUrl; }

    public int getNumSubmissions() { return numSubmissions; }
    public void setNumSubmissions(int numSubmissions) { this.numSubmissions = numSubmissions; }

    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) { this.questions = questions; }
}