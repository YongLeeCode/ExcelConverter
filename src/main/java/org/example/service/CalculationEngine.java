package org.example.service;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.example.model.Calculation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 수식 계산 엔진
 * ${컬럼명} 형태의 변수를 치환하고 exp4j로 계산
 * 문자열 함수 지원: LEFT, RIGHT, SUBSTR, MID
 */
public class CalculationEngine {

    // ${컬럼명} 패턴 매칭
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    // 문자열 함수 패턴
    private static final Pattern LEFT_PATTERN = Pattern.compile("LEFT\\s*\\(\\s*\\$\\{([^}]+)\\}\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RIGHT_PATTERN = Pattern.compile("RIGHT\\s*\\(\\s*\\$\\{([^}]+)\\}\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBSTR_PATTERN = Pattern.compile("(?:SUBSTR|MID)\\s*\\(\\s*\\$\\{([^}]+)\\}\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    public CalculationEngine() {
    }

    /**
     * 수식 계산
     * @param formula 수식 (예: "${상품 매출} + ${제품 매출}")
     * @param rowData 현재 행의 데이터 (컬럼명 → 값)
     * @return 계산 결과 문자열
     */
    public String evaluate(String formula, Map<String, String> rowData) {
        try {
            // 먼저 문자열 함수 체크
            String textResult = evaluateTextFunction(formula, rowData);
            if (textResult != null) {
                return textResult;
            }

            // 1. ${컬럼명} 추출
            Set<String> columnNames = extractColumnNames(formula);

            if (columnNames.isEmpty()) {
                // 변수가 없으면 상수 수식으로 처리
                return evaluateExpression(formula, Collections.emptyMap());
            }

            // 2. 변수명을 알파벳 변수로 매핑 (exp4j는 한글 변수명 지원 안함)
            Map<String, String> varMapping = new LinkedHashMap<>();  // 원본 컬럼명 → 알파벳 변수
            Map<String, Double> varValues = new LinkedHashMap<>();   // 알파벳 변수 → 값

            int varIndex = 0;
            for (String colName : columnNames) {
                String varName = "v" + varIndex++;
                varMapping.put(colName, varName);

                // 값 가져오기
                String value = rowData.get(colName);
                double numValue = parseNumber(value);
                varValues.put(varName, numValue);
            }

            // 3. 수식에서 ${컬럼명}을 알파벳 변수로 치환
            String expression = formula;
            for (Map.Entry<String, String> entry : varMapping.entrySet()) {
                expression = expression.replace("${" + entry.getKey() + "}", entry.getValue());
            }

            // 4. exp4j로 계산
            return evaluateExpression(expression, varValues);

        } catch (Exception e) {
            System.err.println("계산 오류 [" + formula + "]: " + e.getMessage());
            return "0";
        }
    }

    /**
     * 문자열 함수 처리
     * LEFT(${Column}, n) - 왼쪽에서 n자
     * RIGHT(${Column}, n) - 오른쪽에서 n자
     * SUBSTR(${Column}, start, length) 또는 MID(${Column}, start, length) - 부분 문자열
     */
    private String evaluateTextFunction(String formula, Map<String, String> rowData) {
        String trimmedFormula = formula.trim();

        // LEFT 함수
        Matcher leftMatcher = LEFT_PATTERN.matcher(trimmedFormula);
        if (leftMatcher.matches()) {
            String colName = leftMatcher.group(1);
            int length = Integer.parseInt(leftMatcher.group(2));
            String value = rowData.getOrDefault(colName, "");
            if (value.length() <= length) {
                return value;
            }
            return value.substring(0, length);
        }

        // RIGHT 함수
        Matcher rightMatcher = RIGHT_PATTERN.matcher(trimmedFormula);
        if (rightMatcher.matches()) {
            String colName = rightMatcher.group(1);
            int length = Integer.parseInt(rightMatcher.group(2));
            String value = rowData.getOrDefault(colName, "");
            if (value.length() <= length) {
                return value;
            }
            return value.substring(value.length() - length);
        }

        // SUBSTR/MID 함수
        Matcher substrMatcher = SUBSTR_PATTERN.matcher(trimmedFormula);
        if (substrMatcher.matches()) {
            String colName = substrMatcher.group(1);
            int start = Integer.parseInt(substrMatcher.group(2));
            int length = Integer.parseInt(substrMatcher.group(3));
            String value = rowData.getOrDefault(colName, "");
            if (start >= value.length()) {
                return "";
            }
            int end = Math.min(start + length, value.length());
            return value.substring(start, end);
        }

        // 문자열 함수 아님
        return null;
    }

    /**
     * exp4j로 수식 계산
     */
    private String evaluateExpression(String expression, Map<String, Double> variables) {
        try {
            ExpressionBuilder builder = new ExpressionBuilder(expression);

            if (!variables.isEmpty()) {
                builder.variables(variables.keySet());
            }

            Expression exp = builder.build();

            for (Map.Entry<String, Double> entry : variables.entrySet()) {
                exp.setVariable(entry.getKey(), entry.getValue());
            }

            double result = exp.evaluate();

            // 정수면 정수로 반환
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }

            return String.valueOf(result);

        } catch (Exception e) {
            throw new RuntimeException("수식 계산 실패: " + expression, e);
        }
    }

    /**
     * 문자열을 숫자로 변환
     */
    private double parseNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // 쉼표 제거 (천 단위 구분자)
            String cleaned = value.replace(",", "").trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Calculation 객체를 사용한 계산
     */
    public String evaluate(Calculation calc, Map<String, String> rowData) {
        String result = evaluate(calc.getFormula(), rowData);

        // 포맷 적용
        if (calc.getFormat() != null && !"ERROR".equals(result)) {
            try {
                double value = Double.parseDouble(result);
                return String.format(calc.getFormat(), value);
            } catch (NumberFormatException e) {
                return result;
            }
        }

        return result;
    }

    /**
     * 수식에 사용된 컬럼명 추출
     */
    public Set<String> extractColumnNames(String formula) {
        Set<String> columns = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(formula);
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    /**
     * 수식 유효성 검증
     */
    public boolean validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return false;
        }

        try {
            // 모든 변수를 1로 치환하고 테스트
            String testFormula = VARIABLE_PATTERN.matcher(formula).replaceAll("1");
            new ExpressionBuilder(testFormula).build().evaluate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
