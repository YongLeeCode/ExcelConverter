@echo off
chcp 65001 > nul
title Excel to CSV Converter

echo ===================================
echo   Excel → CSV 변환기
echo ===================================
echo.

java -jar ExcelConverter-1.0.jar

if errorlevel 1 (
    echo.
    echo 오류가 발생했습니다.
    echo Java 17 이상이 설치되어 있는지 확인하세요.
    echo.
    pause
)
