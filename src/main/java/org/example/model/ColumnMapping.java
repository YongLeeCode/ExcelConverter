package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 엑셀 컬럼 → CSV 컬럼 매핑 정보
 */
public class ColumnMapping {

    @JsonProperty("source")
    private String source;  // 원본 엑셀 컬럼명

    @JsonProperty("target")
    private String target;  // 출력 CSV 컬럼명 (null이면 source 사용)

    @JsonProperty("required")
    private boolean required = false;  // 필수 컬럼 여부

    @JsonProperty("type")
    private String type = "string";  // number, string, date

    @JsonProperty("uniqueKey")
    private boolean uniqueKey = false;  // 중복 제거 기준 컬럼

    public ColumnMapping() {}

    public ColumnMapping(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(boolean uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    /**
     * 출력용 컬럼명 반환 (target이 없으면 source 반환)
     */
    @JsonIgnore
    public String getOutputName() {
        return (target != null && !target.isEmpty()) ? target : source;
    }

    @Override
    public String toString() {
        return source + " → " + getOutputName();
    }
}
