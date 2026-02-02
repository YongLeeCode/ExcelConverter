package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CSV 출력 옵션
 */
public class OutputOptions {

    @JsonProperty("skipEmptyRows")
    private boolean skipEmptyRows = true;  // 빈 행 건너뛰기

    @JsonProperty("trimWhitespace")
    private boolean trimWhitespace = true;  // 공백 제거

    @JsonProperty("outputEncoding")
    private String outputEncoding = "UTF-8-BOM";  // UTF-8-BOM, UTF-8, EUC-KR

    @JsonProperty("delimiter")
    private String delimiter = ",";  // 구분자

    @JsonProperty("quoteAll")
    private boolean quoteAll = false;  // 모든 필드에 따옴표 적용

    @JsonProperty("outputFormat")
    private String outputFormat = "csv";  // csv, xlsx

    public OutputOptions() {}

    public boolean isSkipEmptyRows() {
        return skipEmptyRows;
    }

    public void setSkipEmptyRows(boolean skipEmptyRows) {
        this.skipEmptyRows = skipEmptyRows;
    }

    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }

    public void setTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
    }

    public String getOutputEncoding() {
        return outputEncoding;
    }

    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean isQuoteAll() {
        return quoteAll;
    }

    public void setQuoteAll(boolean quoteAll) {
        this.quoteAll = quoteAll;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}
