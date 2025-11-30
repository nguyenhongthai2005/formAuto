package com.example.formauto.model;

public class OptionDTO {
    private String text;        // Chữ hiển thị (VD: Nam)
    private String value;       // Giá trị ẩn (data-value) dùng để tìm element click
    private double weight;      // Trọng số % (VD: 70.0)

    public OptionDTO() {}

    public OptionDTO(String text, String value) {
        this.text = text;
        this.value = value;
        this.weight = 0; // Mặc định 0
    }

    // Getters & Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}