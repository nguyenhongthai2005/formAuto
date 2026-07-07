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
        assertNull(selected, "Phải trả về null nếu tất cả trọng số là 0 (để tôn trọng lựa chọn 0% của user)");
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

    @Test
    void testDistribution_ShouldMatchWeights() {
        OptionDTO opt1 = new OptionDTO("A", "A", 0);
        OptionDTO opt2 = new OptionDTO("B", "B", 1);
        
        opt1.setWeight(70);
        opt2.setWeight(30);

        List<OptionDTO> options = Arrays.asList(opt1, opt2);
        WeightedRandom wr = new WeightedRandom(options);

        int countA = 0;
        int countB = 0;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            OptionDTO selected = wr.next();
            if (selected != null && selected.getText().equals("A")) {
                countA++;
            } else if (selected != null && selected.getText().equals("B")) {
                countB++;
            }
        }

        // Kiểm tra phân phối có đúng tỷ lệ 70-30 không (sai số cho phép ~ 2% để tránh flaky test)
        // 70% của 10000 là 7000. Cho phép dao động 6800 - 7200.
        assertTrue(countA >= 6800 && countA <= 7200, "Số lượng A (" + countA + ") phải dao động quanh 7000 (~70%)");
        // 30% của 10000 là 3000. Cho phép dao động 2800 - 3200.
        assertTrue(countB >= 2800 && countB <= 3200, "Số lượng B (" + countB + ") phải dao động quanh 3000 (~30%)");
    }

    @Test
    void testPartialZeroWeight_ShouldScaleCorrectly() {
        // Có 4 mục, mỗi mục ban đầu 25%. Nhưng người dùng đổi mục A thành 0%.
        // Nghĩa là A (0%), B (25%), C (25%), D (25%). Tổng là 75%.
        OptionDTO opt1 = new OptionDTO("A", "A", 0);
        OptionDTO opt2 = new OptionDTO("B", "B", 1);
        OptionDTO opt3 = new OptionDTO("C", "C", 2);
        OptionDTO opt4 = new OptionDTO("D", "D", 3);

        opt1.setWeight(0);
        opt2.setWeight(25);
        opt3.setWeight(25);
        opt4.setWeight(25);

        List<OptionDTO> options = Arrays.asList(opt1, opt2, opt3, opt4);
        WeightedRandom wr = new WeightedRandom(options);

        int countA = 0;
        int countB = 0;
        int countC = 0;
        int countD = 0;
        int iterations = 9000; // Chia hết cho 3 để dễ đo (kỳ vọng mỗi cái B, C, D là 3000)

        for (int i = 0; i < iterations; i++) {
            OptionDTO selected = wr.next();
            assertNotNull(selected, "Không được trả về null khi tổng trọng số > 0");
            
            if (selected.getText().equals("A")) countA++;
            else if (selected.getText().equals("B")) countB++;
            else if (selected.getText().equals("C")) countC++;
            else if (selected.getText().equals("D")) countD++;
        }

        // A không bao giờ được chọn
        assertEquals(0, countA, "Mục A có trọng số 0% nên không bao giờ được chọn");

        // B, C, D sẽ được phân phối đều (mỗi mục khoảng 33.33% của 9000 -> 3000)
        // Cho phép dao động +-200
        assertTrue(countB >= 2800 && countB <= 3200, "Số lượng B (" + countB + ") dao động quanh 3000");
        assertTrue(countC >= 2800 && countC <= 3200, "Số lượng C (" + countC + ") dao động quanh 3000");
        assertTrue(countD >= 2800 && countD <= 3200, "Số lượng D (" + countD + ") dao động quanh 3000");
    }

    @Test
    void testFiveOptions_ZeroWeightNeverSelected() {
        // Mô phỏng đúng kịch bản của người dùng: 5 đáp án. 
        // Đáp án 1, 2, 3: 0%
        // Đáp án 4, 5: 50%
        OptionDTO opt1 = new OptionDTO("1", "1", 0);
        OptionDTO opt2 = new OptionDTO("2", "2", 1);
        OptionDTO opt3 = new OptionDTO("3", "3", 2);
        OptionDTO opt4 = new OptionDTO("4", "4", 3);
        OptionDTO opt5 = new OptionDTO("5", "5", 4);

        opt1.setWeight(0);
        opt2.setWeight(0);
        opt3.setWeight(0);
        opt4.setWeight(50);
        opt5.setWeight(50);

        List<OptionDTO> options = Arrays.asList(opt1, opt2, opt3, opt4, opt5);
        WeightedRandom wr = new WeightedRandom(options);

        int count1 = 0, count2 = 0, count3 = 0, count4 = 0, count5 = 0;
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            OptionDTO selected = wr.next();
            assertNotNull(selected, "Phải trả về giá trị vì tổng trọng số = 100");
            
            String text = selected.getText();
            if (text.equals("1")) count1++;
            else if (text.equals("2")) count2++;
            else if (text.equals("3")) count3++;
            else if (text.equals("4")) count4++;
            else if (text.equals("5")) count5++;
        }

        // CÁC MỤC 0% TUYỆT ĐỐI KHÔNG ĐƯỢC CHỌN (Phải bằng 0)
        assertEquals(0, count1, "Mục 1 (0%) không bao giờ được chọn");
        assertEquals(0, count2, "Mục 2 (0%) không bao giờ được chọn");
        assertEquals(0, count3, "Mục 3 (0%) không bao giờ được chọn");

        // Các mục 50% sẽ chia đều (khoảng 5000 mỗi bên)
        assertTrue(count4 >= 4800 && count4 <= 5200, "Mục 4 (50%) phải dao động quanh 5000");
        assertTrue(count5 >= 4800 && count5 <= 5200, "Mục 5 (50%) phải dao động quanh 5000");
    }
}
