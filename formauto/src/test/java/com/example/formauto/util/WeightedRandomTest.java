package com.example.formauto.util;

import com.example.formauto.model.OptionDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeightedRandomTest {

    @Test
    void testEmptyOptions() {
        WeightedRandom wr = new WeightedRandom(Collections.emptyList());
        assertNull(wr.next(), "Phải trả về null nếu danh sách rỗng");
    }

    @Test
    void testAllZeroWeights_ShouldReturnRandomOption() {
        OptionDTO opt1 = new OptionDTO("A", "A", 0);
        OptionDTO opt2 = new OptionDTO("B", "B", 1);
        OptionDTO opt3 = new OptionDTO("C", "C", 2);
        
        opt1.setWeight(0);
        opt2.setWeight(0);
        opt3.setWeight(0);

        List<OptionDTO> options = Arrays.asList(opt1, opt2, opt3);
        WeightedRandom wr = new WeightedRandom(options);

        OptionDTO selected = wr.next();
        assertNotNull(selected, "Phải trả về 1 mục ngẫu nhiên nếu tất cả trọng số là 0");
        assertTrue(options.contains(selected), "Mục được chọn phải nằm trong danh sách");
    }

    @Test
    void testWithWeights_ShouldReturnOption() {
        OptionDTO opt1 = new OptionDTO("A", "A", 0);
        OptionDTO opt2 = new OptionDTO("B", "B", 1);
        
        opt1.setWeight(100);
        opt2.setWeight(0);

        List<OptionDTO> options = Arrays.asList(opt1, opt2);
        WeightedRandom wr = new WeightedRandom(options);

        OptionDTO selected = wr.next();
        assertNotNull(selected);
        assertEquals("A", selected.getText(), "Phải luôn trả về A vì A chiếm 100% trọng số");
    }

    @Test
    void testExcludeOtherOptionLogic() {
        // Mô phỏng logic khi bỏ qua mục 'Khác' trên giao diện (chỉ truyền vào 2 mục hợp lệ)
        OptionDTO opt1 = new OptionDTO("A", "A", 0);
        OptionDTO opt2 = new OptionDTO("B", "B", 1);
        
        opt1.setWeight(50);
        opt2.setWeight(50);

        List<OptionDTO> options = Arrays.asList(opt1, opt2);
        WeightedRandom wr = new WeightedRandom(options);

        OptionDTO selected = wr.next();
        assertNotNull(selected);
        assertTrue(selected.getText().equals("A") || selected.getText().equals("B"));
    }
}
