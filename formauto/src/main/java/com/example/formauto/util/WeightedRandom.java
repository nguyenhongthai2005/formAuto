package com.example.formauto.util;

import com.example.formauto.model.OptionDTO;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class WeightedRandom {
    private final NavigableMap<Double, OptionDTO> map = new TreeMap<>();
    private final Random random;
    private double total = 0;

    public WeightedRandom(List<OptionDTO> options) {
        this.random = new Random();
        // Xây dựng bản đồ trọng số
        for (OptionDTO opt : options) {
            if (opt.getWeight() > 0) {
                total += opt.getWeight();
                map.put(total, opt);
            }
        }
    }

    public OptionDTO next() {
        if (total == 0) return null; // Không có trọng số nào được set
        double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }
}