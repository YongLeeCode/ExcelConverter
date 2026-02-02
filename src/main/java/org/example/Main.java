package org.example;

import org.example.gui.MainFrame;

import javax.swing.*;

/**
 * 엑셀 → CSV 변환기 메인 클래스
 */
public class Main {

    public static void main(String[] args) {
        runGUI();
    }

    /**
     * GUI 모드 실행
     */
    private static void runGUI() {
        try {
            // 시스템 룩앤필 사용
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 기본 룩앤필 사용
        }

        // 한글 폰트 설정
        setUIFont();

        // EDT에서 GUI 생성
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /**
     * UI 기본 폰트 설정
     */
    private static void setUIFont() {
        try {
            java.awt.Font font = new java.awt.Font("맑은 고딕", java.awt.Font.PLAIN, 12);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
                }
            }
        } catch (Exception e) {
            // 폰트 설정 실패 시 무시
        }
    }
}
