package org.example.service.writer;

import org.example.model.Profile;

import java.io.File;
import java.util.List;

/**
 * 데이터 쓰기 인터페이스
 */
public interface DataWriter extends AutoCloseable {

    /**
     * 출력 파일 초기화
     * @param file 출력 파일
     * @param profile 프로필 (출력 옵션)
     */
    void open(File file, Profile profile) throws Exception;

    /**
     * 헤더 행 쓰기
     * @param headers 헤더 목록
     */
    void writeHeader(List<String> headers) throws Exception;

    /**
     * 데이터 행 쓰기
     * @param values 값 목록 (헤더 순서와 동일)
     */
    void writeRow(List<String> values) throws Exception;

    /**
     * 파일 닫기
     */
    @Override
    void close() throws Exception;

    /**
     * 지원하는 파일 확장자
     */
    String getExtension();

    /**
     * 출력 형식 이름
     */
    String getFormatName();
}
