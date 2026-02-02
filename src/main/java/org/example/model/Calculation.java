package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 계산 컬럼 정의
 * 예: 상품매출 + 제품매출 = 총매출
 */
public class Calculation {

    @JsonProperty("newColumn")
    private String newColumn;  // 새로 생성할 컬럼명

    @JsonProperty("formula")
    private String formula;  // 계산식 (예: "${상품 매출} + ${제품 매출}")

    @JsonProperty("insertAfter")
    private String insertAfter;  // 이 컬럼 뒤에 삽입 (null이면 맨 뒤)

    @JsonProperty("format")
    private String format;  // 출력 포맷 (예: "%.2f", null이면 기본값)

    public Calculation() {}

    public Calculation(String newColumn, String formula) {
        this.newColumn = newColumn;
        this.formula = formula;
    }

    public String getNewColumn() {
        return newColumn;
    }

    public void setNewColumn(String newColumn) {
        this.newColumn = newColumn;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getInsertAfter() {
        return insertAfter;
    }

    public void setInsertAfter(String insertAfter) {
        this.insertAfter = insertAfter;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return newColumn + " = " + formula;
    }
}
