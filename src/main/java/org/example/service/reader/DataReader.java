package org.example.service.reader;

import org.example.model.Profile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 데이터 읽기 인터페이스
 */
public interface DataReader {

    /**
     * 파일에서 데이터를 읽어 행 단위로 콜백 호출
     * @param file 입력 파일
     * @param profile 프로필 (컬럼 매핑 정보)
     * @param headerCallback 헤더 처리 콜백 (원본 헤더 목록)
     * @param rowCallback 행 처리 콜백 (컬럼명 -> 값 맵)
     * @param progressCallback 진행률 콜백 (처리된 행 수)
     * @return 처리된 행 수
     */
    long read(File file,
              Profile profile,
              Consumer<List<String>> headerCallback,
              Consumer<Map<String, String>> rowCallback,
              Consumer<Long> progressCallback) throws Exception;

    /**
     * 지원하는 파일 확장자
     */
    String[] getSupportedExtensions();

    /**
     * 해당 파일을 읽을 수 있는지 확인
     */
    default boolean canRead(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : getSupportedExtensions()) {
            if (name.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
