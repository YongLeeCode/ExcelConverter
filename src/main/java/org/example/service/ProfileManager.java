package org.example.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 프로필 파일 관리자
 * profiles/ 폴더에서 JSON 프로필을 로드하고 관리
 */
public class ProfileManager {

    private static final String PROFILES_FOLDER = "profiles";
    private final ObjectMapper objectMapper;
    private final Path profilesPath;
    private Map<String, Profile> profileCache;

    public ProfileManager() {
        this.objectMapper = new ObjectMapper();
        // 알 수 없는 필드 무시 (하위 호환성)
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.profilesPath = findProfilesPath();
        this.profileCache = new LinkedHashMap<>();
    }

    /**
     * profiles 폴더 경로 찾기
     * 우선순위:
     * 1. 사용자 홈 디렉토리의 .ExcelConverter/profiles (앱 독립적)
     * 2. JAR과 같은 디렉토리의 profiles/
     * 3. 현재 작업 디렉토리의 profiles/ (IDE 실행 시)
     */
    private Path findProfilesPath() {
        // 1. 사용자 홈 디렉토리 (앱 설치와 무관하게 유지됨)
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path userProfiles = userHome.resolve(".ExcelConverter").resolve(PROFILES_FOLDER);

        System.out.println("사용자 profiles 경로: " + userProfiles.toAbsolutePath());

        if (Files.exists(userProfiles)) {
            System.out.println("→ 사용자 profiles 폴더 발견!");
            return userProfiles;
        }

        // 2. JAR 파일 위치 기준 (배포 환경)
        try {
            Path jarLocation = Paths.get(
                ProfileManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            );

            Path basePath = Files.isDirectory(jarLocation) ? jarLocation : jarLocation.getParent();
            Path jarProfiles = basePath.resolve(PROFILES_FOLDER);

            System.out.println("JAR 위치: " + basePath.toAbsolutePath());
            System.out.println("JAR profiles 경로 확인: " + jarProfiles.toAbsolutePath());

            if (Files.exists(jarProfiles)) {
                System.out.println("→ JAR profiles 폴더 발견! 사용자 폴더로 복사합니다.");
                copyDefaultProfiles(jarProfiles, userProfiles);
                return userProfiles;
            }
        } catch (Exception e) {
            System.err.println("JAR 위치 확인 실패: " + e.getMessage());
        }

        // 3. 현재 작업 디렉토리의 profiles (IDE 실행 시)
        Path currentDirProfiles = Paths.get(PROFILES_FOLDER).toAbsolutePath();
        System.out.println("현재 디렉토리 profiles 확인: " + currentDirProfiles);

        if (Files.exists(currentDirProfiles)) {
            System.out.println("→ 현재 디렉토리 profiles 폴더 발견!");
            return currentDirProfiles;
        }

        // 4. 없으면 사용자 홈에 생성
        System.out.println("→ profiles 폴더 없음. 사용자 폴더에 생성: " + userProfiles);
        return userProfiles;
    }

    /**
     * 기본 프로필을 사용자 폴더로 복사
     */
    private void copyDefaultProfiles(Path source, Path dest) {
        try {
            Files.createDirectories(dest);
            try (var files = Files.list(source)) {
                files.filter(p -> p.toString().endsWith(".json"))
                     .forEach(p -> {
                         try {
                             Path target = dest.resolve(p.getFileName());
                             if (!Files.exists(target)) {
                                 Files.copy(p, target);
                                 System.out.println("  복사: " + p.getFileName());
                             }
                         } catch (IOException e) {
                             System.err.println("프로필 복사 실패: " + e.getMessage());
                         }
                     });
            }
        } catch (IOException e) {
            System.err.println("기본 프로필 복사 실패: " + e.getMessage());
        }
    }

    /**
     * 모든 프로필 로드
     */
    public List<Profile> loadAllProfiles() throws IOException {
        profileCache.clear();

        if (!Files.exists(profilesPath)) {
            System.out.println("프로필 폴더가 없습니다: " + profilesPath.toAbsolutePath());
            Files.createDirectories(profilesPath);
            return Collections.emptyList();
        }

        List<Profile> profiles = new ArrayList<>();

        try (Stream<Path> files = Files.list(profilesPath)) {
            List<Path> jsonFiles = files
                .filter(p -> p.toString().toLowerCase().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());

            for (Path jsonFile : jsonFiles) {
                try {
                    Profile profile = loadProfile(jsonFile.toFile());
                    profiles.add(profile);
                    profileCache.put(profile.getProfileName(), profile);
                    System.out.println("프로필 로드: " + profile.getProfileName());
                } catch (Exception e) {
                    System.err.println("프로필 로드 실패: " + jsonFile + " - " + e.getMessage());
                }
            }
        }

        return profiles;
    }

    /**
     * 단일 프로필 파일 로드
     */
    public Profile loadProfile(File file) throws IOException {
        Profile profile = objectMapper.readValue(file, Profile.class);
        profile.setFilePath(file.getAbsolutePath());
        return profile;
    }

    /**
     * 이름으로 프로필 조회 (캐시에서)
     */
    public Profile getProfile(String name) {
        return profileCache.get(name);
    }

    /**
     * 프로필 이름 목록 반환 (드롭다운용)
     */
    public List<String> getProfileNames() {
        if (profileCache.isEmpty()) {
            try {
                loadAllProfiles();
            } catch (IOException e) {
                System.err.println("프로필 로드 오류: " + e.getMessage());
            }
        }
        return new ArrayList<>(profileCache.keySet());
    }

    /**
     * 캐시된 프로필 목록 반환
     */
    public List<Profile> getCachedProfiles() {
        return new ArrayList<>(profileCache.values());
    }

    /**
     * 프로필 저장
     */
    public void saveProfile(Profile profile) throws IOException {
        String fileName = profile.getProfileName() + ".json";
        Path filePath = profilesPath.resolve(fileName);
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(filePath.toFile(), profile);
        profileCache.put(profile.getProfileName(), profile);
    }

    /**
     * 프로필 삭제
     */
    public boolean deleteProfile(String name) throws IOException {
        Profile profile = profileCache.get(name);
        if (profile != null && profile.getFilePath() != null) {
            Files.deleteIfExists(Paths.get(profile.getFilePath()));
            profileCache.remove(name);
            return true;
        }
        return false;
    }

    /**
     * 프로필 폴더 경로 반환
     */
    public Path getProfilesPath() {
        return profilesPath;
    }
}
