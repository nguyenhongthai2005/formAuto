package com.example.formauto.model;

import java.util.ArrayList;
import java.util.List;

public class QuestionDTO {
    private int index;              // Số thứ tự câu hỏi
    private String title;           // Tiêu đề câu hỏi
    private String type;            // RADIO, CHECKBOX, TEXT
    private List<OptionDTO> options = new ArrayList<>();

    public void addOption(OptionDTO option) {
        this.options.add(option);
    }

    // Getters & Setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<OptionDTO> getOptions() { return options; }
    public void setOptions(List<OptionDTO> options) { this.options = options; }
}