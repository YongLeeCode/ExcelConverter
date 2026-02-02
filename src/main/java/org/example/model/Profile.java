package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 변환 프로필 - 어떤 컬럼을 추출하고 어떻게 변환할지 정의
 */
public class Profile {

    @JsonProperty("profileName")
    private String profileName;  // 프로필 이름 (드롭다운에 표시)

    @JsonProperty("description")
    private String description;  // 프로필 설명

    @JsonProperty("version")
    private String version = "1.0";  // 프로필 버전

    @JsonProperty("columns")
    private List<ColumnMapping> columns = new ArrayList<>();  // 컬럼 매핑 목록

    @JsonProperty("calculations")
    private List<Calculation> calculations = new ArrayList<>();  // 계산 컬럼 목록

    @JsonProperty("options")
    private OutputOptions options = new OutputOptions();  // 출력 옵션

    @JsonProperty("outputFileName")
    private String outputFileName;  // 출력 파일명 패턴 (예: "sales_result.csv")

    // 프로필 파일 경로 (런타임에 설정)
    private transient String filePath;

    public Profile() {}

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ColumnMapping> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMapping> columns) {
        this.columns = columns;
    }

    public List<Calculation> getCalculations() {
        return calculations;
    }

    public void setCalculations(List<Calculation> calculations) {
        this.calculations = calculations;
    }

    public OutputOptions getOptions() {
        return options;
    }

    public void setOptions(OutputOptions options) {
        this.options = options;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 출력 컬럼명 목록 반환 (매핑 컬럼 + 계산 컬럼)
     */
    public List<String> getOutputColumnNames() {
        List<String> names = new ArrayList<>();

        // 매핑 컬럼 추가
        for (ColumnMapping col : columns) {
            names.add(col.getOutputName());
        }

        // 계산 컬럼 추가 (insertAfter 위치에 맞게)
        for (Calculation calc : calculations) {
            if (calc.getInsertAfter() != null) {
                int insertIndex = names.indexOf(calc.getInsertAfter());
                if (insertIndex >= 0) {
                    names.add(insertIndex + 1, calc.getNewColumn());
                } else {
                    names.add(calc.getNewColumn());
                }
            } else {
                names.add(calc.getNewColumn());
            }
        }

        return names;
    }

    @Override
    public String toString() {
        return profileName + " (" + columns.size() + " 컬럼, " + calculations.size() + " 계산)";
    }
}
