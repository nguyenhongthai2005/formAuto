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

    private final List<OptionDTO> options;

    public WeightedRandom(List<OptionDTO> options) {
        this.options = options;
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
        if (options == null || options.isEmpty()) return null;
        if (total <= 0) {
            // Nếu người dùng lỡ để tất cả là 0%, bắt buộc phải chọn bừa 1 cái thay vì bỏ trống (tránh lỗi Validation)
            return options.get(random.nextInt(options.size()));
        }
        double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }
}