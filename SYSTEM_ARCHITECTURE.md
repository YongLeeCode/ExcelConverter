# ExcelConverterMusinsa 시스템 아키텍처 및 동작 설명서

## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [프로젝트 구조](#3-프로젝트-구조)
4. [애플리케이션 실행 흐름](#4-애플리케이션-실행-흐름)
5. [GUI 레이어 상세 설명](#5-gui-레이어-상세-설명)
6. [Model 레이어 상세 설명](#6-model-레이어-상세-설명)
7. [Service 레이어 상세 설명](#7-service-레이어-상세-설명)
8. [Reader/Writer 레이어 상세 설명](#8-readerwriter-레이어-상세-설명)
9. [데이터 변환 프로세스 상세](#9-데이터-변환-프로세스-상세)
10. [클래스 간 상호작용](#10-클래스-간-상호작용)

---

## 1. 프로젝트 개요

### 1.1 목적
ExcelConverterMusinsa는 **Excel(XLSX)과 CSV 파일 간의 변환 도구**입니다. 사용자 정의 프로필을 통해 다음 기능을 제공합니다:

- 컬럼 매핑 (원본 컬럼명 → 출력 컬럼명 변경)
- 데이터 필터링 (필요한 컬럼만 선택)
- 계산 컬럼 추가 (수식 기반 새 컬럼 생성)
- 중복 제거 (특정 키 기준)
- 여러 파일 병합
- 대용량 파일 스트리밍 처리

### 1.2 주요 특징
- **Java 17 기반** Swing GUI 데스크톱 애플리케이션
- **JSON 기반 프로필 설정**으로 재사용 가능한 변환 규칙 관리
- **SAX/SXSSF 스트리밍**으로 메모리 효율적인 대용량 파일 처리
- **드래그 앤 드롭** 지원
- **배치 처리** (여러 파일 동시 변환)

---

## 2. 기술 스택

### 2.1 핵심 기술

| 분류 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **언어** | Java | 17 | 메인 개발 언어 |
| **GUI** | Java Swing | 내장 | 데스크톱 GUI 프레임워크 |
| **Excel 처리** | Apache POI | 5.2.5 | XLSX 읽기/쓰기 |
| **CSV 처리** | OpenCSV | 5.9 | CSV 읽기/쓰기 |
| **JSON 파싱** | Jackson | 2.16.1 | 프로필 설정 파일 처리 |
| **수식 계산** | exp4j | 0.4.8 | 계산 컬럼 수식 평가 |
| **빌드** | Gradle (Kotlin DSL) | 9.0 | 빌드 자동화 |

### 2.2 Swing 사용 이유
- JDK에 내장되어 별도 의존성 불필요
- 크로스 플랫폼 지원 (Windows, macOS, Linux)
- 데스크톱 애플리케이션에 적합한 성숙한 프레임워크
- 시스템 Look & Feel 적용으로 네이티브 느낌 제공

### 2.3 Apache POI 스트리밍 방식
- **읽기**: SAX 기반 이벤트 처리 (XSSFReader)
- **쓰기**: SXSSF (Streaming Usermodel API)
- **장점**: 수백만 행 파일도 메모리 부족 없이 처리

---

## 3. 프로젝트 구조

```
ExcelConverterMusinsa/
├── src/main/java/org/example/
│   │
│   ├── Main.java                           # [진입점] 애플리케이션 시작
│   │
│   ├── gui/                                # [GUI 레이어] Swing 컴포넌트
│   │   ├── MainFrame.java                  # 메인 윈도우 (517줄)
│   │   ├── ConversionWorker.java           # 백그라운드 변환 작업자 (133줄)
│   │   ├── ProfileEditorDialog.java        # 프로필 편집 다이얼로그 (424줄)
│   │   ├── ProgressInfo.java               # 진행률 정보 DTO (40줄)
│   │   ├── FileItem.java                   # 파일 목록 아이템 (27줄)
│   │   ├── FileItemRenderer.java           # 파일 목록 렌더러 (19줄)
│   │   └── FileDrop.java                   # 드래그 앤 드롭 핸들러 (81줄)
│   │
│   ├── model/                              # [Model 레이어] 데이터 모델
│   │   ├── Profile.java                    # 변환 프로필 (134줄)
│   │   ├── ColumnMapping.java              # 컬럼 매핑 정의 (85줄)
│   │   ├── Calculation.java                # 계산 컬럼 정의 (66줄)
│   │   ├── OutputOptions.java              # 출력 옵션 (77줄)
│   │   └── ConversionResult.java           # 변환 결과 (238줄)
│   │
│   └── service/                            # [Service 레이어] 비즈니스 로직
│       ├── ExcelConverterService.java      # 메인 변환 서비스 (631줄)
│       ├── ProfileManager.java             # 프로필 관리자 (217줄)
│       ├── CalculationEngine.java          # 수식 계산 엔진 (238줄)
│       │
│       ├── reader/                         # 파일 읽기 모듈
│       │   ├── DataReader.java             # 리더 인터페이스 (47줄)
│       │   ├── XlsxReader.java             # XLSX 읽기 (228줄)
│       │   └── CsvReader.java              # CSV 읽기 (112줄)
│       │
│       └── writer/                         # 파일 쓰기 모듈
│           ├── DataWriter.java             # 라이터 인터페이스 (47줄)
│           ├── CsvDataWriter.java          # CSV 쓰기 (72줄)
│           └── XlsxDataWriter.java         # XLSX 쓰기 (96줄)
│
├── profiles/                               # 프로필 저장 폴더 (JSON 파일)
├── build.gradle.kts                        # Gradle 빌드 설정
├── config.json                             # 샘플 설정 파일
├── run.sh / run.bat                        # 실행 스크립트
└── settings.gradle.kts                     # Gradle 설정
```

---

## 4. 애플리케이션 실행 흐름

### 4.1 시작 단계 (Main.java)

```
┌─────────────────────────────────────────────────────────┐
│                    Main.main()                          │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  1. setUIFont("맑은 고딕", 12)                          │
│     → 모든 Swing 컴포넌트에 한글 폰트 적용              │
│     → UIManager의 모든 폰트 관련 키 업데이트            │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  2. UIManager.setLookAndFeel(getSystemLookAndFeelClass) │
│     → 운영체제 네이티브 룩앤필 적용                     │
│     → Windows: Windows L&F, macOS: Aqua L&F            │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  3. SwingUtilities.invokeLater()                        │
│     → EDT(Event Dispatch Thread)에서 GUI 생성          │
│     → Swing 스레드 안전성 보장                          │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  4. new MainFrame().setVisible(true)                    │
│     → 메인 윈도우 생성 및 표시                          │
└─────────────────────────────────────────────────────────┘
```

**Main.java 핵심 코드:**
```java
public class Main {
    public static void main(String[] args) {
        // 1. 한글 폰트 설정
        setUIFont(new FontUIResource("맑은 고딕", Font.PLAIN, 12));

        // 2. 시스템 룩앤필 적용
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // 3. EDT에서 GUI 실행
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }

    // 모든 UI 컴포넌트에 폰트 적용
    private static void setUIFont(FontUIResource font) {
        for (Enumeration<Object> keys = UIManager.getDefaults().keys();
             keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }
}
```

### 4.2 MainFrame 초기화 단계

```
┌─────────────────────────────────────────────────────────┐
│                 MainFrame 생성자                         │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  1. ProfileManager 생성                                 │
│     → profiles/ 폴더 경로 결정                          │
│     → JSON 프로필 파일들 로드                            │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  2. ExcelConverterService 생성                          │
│     → 변환 서비스 인스턴스 준비                          │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  3. initUI() - UI 컴포넌트 초기화                        │
│     ├─ createProfilePanel()      → 프로필 선택 패널     │
│     ├─ createFileListPanel()     → 파일 목록 패널       │
│     ├─ createBottomPanel()       → 하단 제어 패널       │
│     └─ FileDrop 설정             → 드래그 앤 드롭        │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  4. loadProfiles()                                      │
│     → 프로필 목록을 콤보박스에 로드                      │
└─────────────────────────────────────────────────────────┘
```

### 4.3 사용자 인터랙션 흐름

```
┌──────────────────────────────────────────────────────────────────────┐
│                         MainFrame (GUI)                              │
├──────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │ Profile Panel                                                    │ │
│  │  [▼ 프로필 선택 ─────────────▼]  [+] [✎] [📁]                  │ │
│  │                                                                  │ │
│  │  (+): 새 프로필 생성 → ProfileEditorDialog                       │ │
│  │  (✎): 선택된 프로필 편집 → ProfileEditorDialog                   │ │
│  │  (📁): profiles 폴더 열기                                        │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │ File List Panel (드래그 앤 드롭 지원)                            │ │
│  │  ┌─────────────────────────────────────────────────────────────┐ │ │
│  │  │ ● file1.xlsx (2.5 MB)                                       │ │ │
│  │  │ ● file2.csv (1.2 MB)                                        │ │ │
│  │  │ ● file3.xlsx (5.0 MB)                                       │ │ │
│  │  └─────────────────────────────────────────────────────────────┘ │ │
│  │  [Add Files...] [Remove] [Clear]                                 │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │ Bottom Panel                                                     │ │
│  │  Output: [────────────────────────────] [Browse]                 │ │
│  │                                                                  │ │
│  │  [████████████████░░░░░░░░░░░░░░░░] 50% - Processing file 2/4   │ │
│  │                                                                  │ │
│  │  Format: (●) CSV  ( ) Excel    [☑] Merge all files into one     │ │
│  │                                                                  │ │
│  │                            [Convert] [Cancel]                    │ │
│  └─────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 5. GUI 레이어 상세 설명

### 5.1 MainFrame.java (메인 윈도우)

**목적**: 애플리케이션의 메인 UI를 제공하고 사용자 인터랙션을 처리

**주요 컴포넌트:**
```java
public class MainFrame extends JFrame {
    // 서비스 레이어
    private final ProfileManager profileManager;
    private final ExcelConverterService converterService;

    // UI 컴포넌트
    private JComboBox<String> profileCombo;        // 프로필 선택
    private DefaultListModel<FileItem> fileListModel;  // 파일 목록 모델
    private JList<FileItem> fileList;              // 파일 목록 뷰
    private JTextField outputDirField;             // 출력 경로
    private JProgressBar progressBar;              // 진행률
    private JLabel statusLabel;                    // 상태 메시지
    private JRadioButton csvRadio, excelRadio;     // 출력 포맷
    private JCheckBox mergeCheckBox;               // 병합 옵션
    private JButton convertButton, cancelButton;   // 제어 버튼

    // 상태
    private ConversionWorker currentWorker;        // 현재 변환 작업
}
```

**핵심 메서드:**

| 메서드 | 역할 |
|--------|------|
| `initUI()` | UI 레이아웃 초기화 |
| `createProfilePanel()` | 프로필 선택 패널 생성 |
| `createFileListPanel()` | 파일 목록 패널 생성 (드래그앤드롭 포함) |
| `createBottomPanel()` | 하단 제어 패널 생성 |
| `loadProfiles()` | 프로필 목록 로드 및 콤보박스 업데이트 |
| `openProfileEditor(Profile)` | 프로필 편집 다이얼로그 열기 |
| `startConversion()` | 변환 시작 (검증 후 ConversionWorker 실행) |
| `createConversionCallback()` | 진행률/완료 콜백 생성 |

**변환 시작 로직 (startConversion):**
```java
private void startConversion() {
    // 1. 검증
    String profileName = (String) profileCombo.getSelectedItem();
    if (profileName == null) {
        showError("프로필을 선택하세요");
        return;
    }

    if (fileListModel.isEmpty()) {
        showError("파일을 추가하세요");
        return;
    }

    File outputDir = new File(outputDirField.getText());
    if (!outputDir.isDirectory()) {
        showError("유효한 출력 디렉토리를 선택하세요");
        return;
    }

    // 2. 프로필 로드
    Profile profile = profileManager.getProfile(profileName);

    // 3. 출력 포맷 설정
    String format = csvRadio.isSelected() ? "csv" : "xlsx";
    profile.getOptions().setOutputFormat(format);

    // 4. 입력 파일 목록 생성
    List<File> inputFiles = new ArrayList<>();
    for (int i = 0; i < fileListModel.size(); i++) {
        inputFiles.add(fileListModel.get(i).getFile());
    }

    // 5. UI 상태 변경
    convertButton.setEnabled(false);
    cancelButton.setEnabled(true);
    progressBar.setValue(0);

    // 6. 백그라운드 변환 시작
    boolean merge = mergeCheckBox.isSelected();
    currentWorker = new ConversionWorker(
        converterService, profile, inputFiles, outputDir,
        merge, createConversionCallback()
    );
    currentWorker.execute();
}
```

### 5.2 ConversionWorker.java (백그라운드 변환)

**목적**: GUI 블로킹 없이 백그라운드에서 파일 변환 수행

**클래스 구조:**
```java
public class ConversionWorker
    extends SwingWorker<List<ConversionResult>, ProgressInfo> {

    private final ExcelConverterService service;
    private final Profile profile;
    private final List<File> inputFiles;
    private final File outputDir;
    private final boolean mergeFiles;
    private final ConversionCallback callback;

    // 콜백 인터페이스
    public interface ConversionCallback {
        void onProgress(ProgressInfo info);      // 진행률 업데이트
        void onComplete(List<ConversionResult> results);  // 완료
        void onError(Exception e);               // 오류
        void onCancelled();                      // 취소됨
    }
}
```

**SwingWorker 동작 방식:**

```
┌────────────────────────────────────────────────────────────────────┐
│                    Worker Thread (백그라운드)                       │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  doInBackground() {                                                │
│      service.convert(profile, files, outputDir, listener, merge)  │
│                          │                                         │
│                          ▼                                         │
│              ProgressListener.onProgress()                         │
│                          │                                         │
│                          ▼                                         │
│              publish(ProgressInfo)  ──────────────────────┐       │
│  }                                                         │       │
│                                                            │       │
└────────────────────────────────────────────────────────────│───────┘
                                                             │
                                                             ▼
┌────────────────────────────────────────────────────────────────────┐
│                    EDT (Event Dispatch Thread)                     │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  process(List<ProgressInfo> chunks) {                              │
│      // GUI 업데이트 (스레드 안전)                                  │
│      callback.onProgress(chunks.get(chunks.size() - 1))            │
│  }                                                                 │
│                                                                    │
│  done() {                                                          │
│      if (isCancelled()) callback.onCancelled()                     │
│      else if (exception) callback.onError(exception)               │
│      else callback.onComplete(results)                             │
│  }                                                                 │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**doInBackground 구현:**
```java
@Override
protected List<ConversionResult> doInBackground() throws Exception {
    // ProgressListener를 통해 진행률을 publish
    ExcelConverterService.ProgressListener listener =
        (fileIndex, totalFiles, currentRow, totalRows, status) -> {
            if (isCancelled()) {
                throw new InterruptedException("변환 취소됨");
            }
            // EDT로 진행률 전송
            publish(new ProgressInfo(fileIndex, totalFiles,
                                    currentRow, totalRows, status));
        };

    return service.convert(profile, inputFiles, outputDir,
                          listener, mergeFiles);
}
```

### 5.3 ProfileEditorDialog.java (프로필 편집)

**목적**: 새 프로필 생성 또는 기존 프로필 편집

**UI 구성:**
```
┌──────────────────────────────────────────────────────────────────┐
│                    Profile Editor                                │
├──────────────────────────────────────────────────────────────────┤
│  Basic Info                                                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Profile Name: [_________________________]                   │  │
│  │ Description:  [_________________________]                   │  │
│  │ Output File:  [_________________________] (패턴: %name%)    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Column Mappings                                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Source Column │ Target Column │ Type   │ Required │ UniqueKey│ │
│  │───────────────│───────────────│────────│──────────│──────────│ │
│  │ 기간/연도      │ 연도          │ string │ ☑        │ ☐        │ │
│  │ 매출금액       │ 매출          │ number │ ☑        │ ☐        │ │
│  │ 상품코드       │ 상품코드      │ string │ ☑        │ ☑        │ │
│  └────────────────────────────────────────────────────────────┘  │
│  [+ Add Row] [- Remove Row]                                      │
│                                                                  │
│  Calculated Columns                                              │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ New Column │ Formula                    │ Insert After│ Format│ │
│  │────────────│────────────────────────────│─────────────│───────│ │
│  │ 마진율      │ ${매출} - ${원가}) / ${매출} │ 매출        │ %.2f  │ │
│  └────────────────────────────────────────────────────────────┘  │
│  [+ Add Row] [- Remove Row] [? Formula Help]                     │
│                                                                  │
│                                    [Save] [Cancel]               │
└──────────────────────────────────────────────────────────────────┘
```

**저장 로직:**
```java
private void saveProfile() {
    // 1. 기본 정보 수집
    String name = nameField.getText().trim();
    String description = descField.getText().trim();
    String outputFileName = outputFileField.getText().trim();

    // 2. 컬럼 매핑 수집
    List<ColumnMapping> columns = new ArrayList<>();
    for (int i = 0; i < columnTableModel.getRowCount(); i++) {
        ColumnMapping mapping = new ColumnMapping();
        mapping.setSource((String) columnTableModel.getValueAt(i, 0));
        mapping.setTarget((String) columnTableModel.getValueAt(i, 1));
        mapping.setType((String) columnTableModel.getValueAt(i, 2));
        mapping.setRequired((Boolean) columnTableModel.getValueAt(i, 3));
        mapping.setUniqueKey((Boolean) columnTableModel.getValueAt(i, 4));
        columns.add(mapping);
    }

    // 3. 계산 컬럼 수집
    List<Calculation> calculations = new ArrayList<>();
    for (int i = 0; i < calcTableModel.getRowCount(); i++) {
        Calculation calc = new Calculation();
        calc.setNewColumn((String) calcTableModel.getValueAt(i, 0));
        calc.setFormula((String) calcTableModel.getValueAt(i, 1));
        calc.setInsertAfter((String) calcTableModel.getValueAt(i, 2));
        calc.setFormat((String) calcTableModel.getValueAt(i, 3));
        calculations.add(calc);
    }

    // 4. 프로필 객체 생성 및 저장
    Profile profile = new Profile();
    profile.setProfileName(name);
    profile.setDescription(description);
    profile.setOutputFileName(outputFileName);
    profile.setColumns(columns);
    profile.setCalculations(calculations);
    profile.setOptions(new OutputOptions());  // 기본 옵션

    profileManager.saveProfile(profile);
    dispose();
}
```

### 5.4 FileDrop.java (드래그 앤 드롭)

**목적**: 파일 탐색기에서 파일을 드래그하여 목록에 추가

**구현:**
```java
public class FileDrop {
    public interface Listener {
        void filesDropped(List<File> files);
    }

    public FileDrop(Component component, Listener listener) {
        DropTarget dropTarget = new DropTarget(component, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                event.acceptDrop(DnDConstants.ACTION_COPY);

                Transferable transferable = event.getTransferable();
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable
                        .getTransferData(DataFlavor.javaFileListFlavor);

                    // xlsx, csv 파일만 필터링
                    List<File> validFiles = files.stream()
                        .filter(f -> f.getName().toLowerCase()
                                     .matches(".*\\.(xlsx|csv)$"))
                        .collect(Collectors.toList());

                    listener.filesDropped(validFiles);
                }

                event.dropComplete(true);
            }
        });
    }
}
```

### 5.5 ProgressInfo.java (진행률 정보)

**목적**: 변환 진행 상태를 담는 DTO

```java
public class ProgressInfo {
    private final int fileIndex;      // 현재 파일 인덱스 (1부터)
    private final int totalFiles;     // 전체 파일 수
    private final long currentRow;    // 현재 처리 행
    private final long totalRows;     // 전체 행 수 (추정치)
    private final String status;      // 상태 메시지

    // 전체 진행률 계산
    public int getOverallPercent() {
        if (totalFiles == 0) return 0;
        double fileProgress = (fileIndex - 1.0) / totalFiles;
        double rowProgress = totalRows > 0
            ? (double) currentRow / totalRows / totalFiles
            : 0;
        return (int) ((fileProgress + rowProgress) * 100);
    }
}
```

---

## 6. Model 레이어 상세 설명

### 6.1 Profile.java (변환 프로필)

**목적**: 변환 규칙을 정의하는 핵심 설정 모델

**JSON 구조:**
```json
{
  "profileName": "매출데이터_변환",
  "description": "월별 매출 데이터를 정제된 형식으로 변환",
  "version": "1.0",
  "columns": [
    {
      "source": "기간/연도",
      "target": "연도",
      "type": "string",
      "required": true,
      "uniqueKey": false
    },
    {
      "source": "매출금액",
      "target": "매출",
      "type": "number",
      "required": true,
      "uniqueKey": false
    }
  ],
  "calculations": [
    {
      "newColumn": "마진율",
      "formula": "(${매출} - ${원가}) / ${매출}",
      "insertAfter": "매출",
      "format": "%.2f"
    }
  ],
  "options": {
    "skipEmptyRows": true,
    "trimWhitespace": true,
    "outputEncoding": "UTF-8-BOM",
    "delimiter": ",",
    "quoteAll": false,
    "outputFormat": "csv"
  },
  "outputFileName": "%name%_converted"
}
```

**핵심 메서드:**
```java
public class Profile {
    // 최종 출력 컬럼 순서 계산 (매핑 + 계산 컬럼)
    public List<String> getOutputColumnNames() {
        List<String> result = new ArrayList<>();

        // 1. 매핑된 컬럼 추가
        for (ColumnMapping col : columns) {
            result.add(col.getTarget() != null ? col.getTarget() : col.getSource());
        }

        // 2. 계산 컬럼을 지정된 위치에 삽입
        for (Calculation calc : calculations) {
            String insertAfter = calc.getInsertAfter();
            int insertIndex = result.size();  // 기본: 맨 뒤

            if (insertAfter != null && !insertAfter.isEmpty()) {
                int afterIndex = result.indexOf(insertAfter);
                if (afterIndex >= 0) {
                    insertIndex = afterIndex + 1;
                }
            }

            result.add(insertIndex, calc.getNewColumn());
        }

        return result;
    }
}
```

### 6.2 ColumnMapping.java (컬럼 매핑)

**목적**: 원본 컬럼과 출력 컬럼 간의 매핑 정의

```java
public class ColumnMapping {
    private String source;       // 원본 컬럼명 (필수)
    private String target;       // 출력 컬럼명 (null이면 source 사용)
    private String type;         // 데이터 타입: string, number, date
    private boolean required;    // 필수 컬럼 여부
    private boolean uniqueKey;   // 중복 제거 기준 컬럼

    // target이 null이면 source 반환
    public String getEffectiveTarget() {
        return target != null && !target.isEmpty() ? target : source;
    }
}
```

**사용 예시:**
- `source: "기간/연도", target: "연도"` → 컬럼명 변경
- `source: "매출금액", target: null` → 원본 컬럼명 유지
- `uniqueKey: true` → 이 컬럼 값 기준 중복 제거

### 6.3 Calculation.java (계산 컬럼)

**목적**: 수식 기반 새 컬럼 생성 정의

```java
public class Calculation {
    private String newColumn;     // 새 컬럼명
    private String formula;       // 수식 (예: "${매출} - ${원가}")
    private String insertAfter;   // 삽입 위치 (특정 컬럼 뒤)
    private String format;        // 출력 포맷 (예: "%.2f")
}
```

**지원 수식:**
- **수학 연산**: `${col1} + ${col2}`, `${col1} * 0.1`
- **문자열 함수**:
  - `LEFT(${col}, 3)` → 왼쪽 3자
  - `RIGHT(${col}, 4)` → 오른쪽 4자
  - `SUBSTR(${col}, 2, 5)` 또는 `MID(${col}, 2, 5)` → 부분 문자열
  - `TRIM(${col})` → 공백 제거

### 6.4 OutputOptions.java (출력 옵션)

**목적**: 출력 파일 설정

```java
public class OutputOptions {
    private boolean skipEmptyRows = true;     // 빈 행 건너뛰기
    private boolean trimWhitespace = true;    // 앞뒤 공백 제거
    private String outputEncoding = "UTF-8-BOM";  // 인코딩
    private String delimiter = ",";           // CSV 구분자
    private boolean quoteAll = false;         // 모든 값 따옴표 감싸기
    private String outputFormat = "csv";      // 출력 포맷: csv, xlsx
}
```

**인코딩 옵션:**
- `UTF-8`: 표준 UTF-8
- `UTF-8-BOM`: UTF-8 with BOM (Excel 한글 호환)
- `EUC-KR`: 레거시 한글 인코딩

### 6.5 ConversionResult.java (변환 결과)

**목적**: 변환 작업 결과 및 통계

```java
public class ConversionResult {
    public enum Status {
        SUCCESS,    // 성공
        FAILED,     // 실패
        CANCELLED,  // 취소됨
        SKIPPED     // 건너뜀
    }

    private Status status;
    private File inputFile;
    private File outputFile;
    private String errorMessage;

    // 통계
    private long inputRows;       // 입력 행 수
    private long outputRows;      // 출력 행 수
    private long duplicateRows;   // 중복 제거된 행 수
    private long emptyRows;       // 빈 행 수
    private long startTime;       // 시작 시간
    private long endTime;         // 종료 시간

    // 처리 시간 계산
    public long getProcessingTimeMs() {
        return endTime - startTime;
    }

    // 요약 문자열
    public String getSummary() {
        return String.format(
            "%s: %d rows → %d rows (duplicates: %d, empty: %d) in %dms",
            inputFile.getName(), inputRows, outputRows,
            duplicateRows, emptyRows, getProcessingTimeMs()
        );
    }
}
```

---

## 7. Service 레이어 상세 설명

### 7.1 ExcelConverterService.java (메인 변환 서비스)

**목적**: 파일 변환의 핵심 비즈니스 로직

**주요 구성:**
```java
public class ExcelConverterService {
    private final List<DataReader> readers;   // 파일 리더 목록
    private final CalculationEngine calcEngine;  // 계산 엔진

    public ExcelConverterService() {
        readers = Arrays.asList(new XlsxReader(), new CsvReader());
        calcEngine = new CalculationEngine();
    }
}
```

**변환 메서드:**
```java
public List<ConversionResult> convert(
    Profile profile,
    List<File> inputFiles,
    File outputDir,
    ProgressListener listener,
    boolean mergeFiles
) {
    if (mergeFiles) {
        return convertMerged(profile, inputFiles, outputDir, listener);
    } else {
        return convertIndividual(profile, inputFiles, outputDir, listener);
    }
}
```

**개별 변환 로직 (convertIndividual):**

```
입력 파일 목록
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  for each inputFile:                                    │
│                                                         │
│  1. DataReader 선택                                     │
│     findReader(inputFile) → XlsxReader 또는 CsvReader   │
│                                                         │
│  2. DataWriter 생성                                     │
│     profile.outputFormat → CsvDataWriter 또는 XlsxWriter│
│                                                         │
│  3. 출력 파일명 결정                                     │
│     resolveOutputFileName(profile, inputFile)           │
│                                                         │
│  4. 변환 실행                                            │
│     reader.read(file, profile, headerCallback, rowCallback)│
│                                                         │
│  5. 결과 수집                                            │
│     ConversionResult 생성                               │
└─────────────────────────────────────────────────────────┘
    │
    ▼
결과 목록 반환
```

**헤더 처리 콜백:**
```java
Consumer<List<String>> headerCallback = headers -> {
    // 1. 누락 컬럼 검증
    List<String> missingColumns = new ArrayList<>();
    for (ColumnMapping col : profile.getColumns()) {
        if (!headers.contains(col.getSource())) {
            if (col.isRequired()) {
                missingColumns.add(col.getSource());
            }
        }
    }
    if (!missingColumns.isEmpty()) {
        throw new RuntimeException("Missing required columns: " + missingColumns);
    }

    // 2. 소스 컬럼 인덱스 매핑
    Map<String, Integer> columnIndexMap = new HashMap<>();
    for (int i = 0; i < headers.size(); i++) {
        columnIndexMap.put(headers.get(i), i);
    }

    // 3. 출력 헤더 생성 및 쓰기
    List<String> outputHeaders = profile.getOutputColumnNames();
    writer.writeHeader(outputHeaders);
};
```

**행 처리 콜백:**
```java
Consumer<Map<String, String>> rowCallback = row -> {
    // 1. 빈 행 건너뛰기
    if (profile.getOptions().isSkipEmptyRows()) {
        boolean isEmpty = row.values().stream()
            .allMatch(v -> v == null || v.trim().isEmpty());
        if (isEmpty) {
            emptyRowCount.incrementAndGet();
            return;
        }
    }

    // 2. 중복 체크 (uniqueKey 기반)
    String uniqueKeyValue = buildUniqueKey(profile, row);
    if (uniqueKeyValue != null) {
        if (seenKeys.contains(uniqueKeyValue)) {
            duplicateCount.incrementAndGet();
            return;
        }
        seenKeys.add(uniqueKeyValue);
    }

    // 3. 컬럼 값 추출
    List<String> values = new ArrayList<>();
    for (ColumnMapping col : profile.getColumns()) {
        String value = row.get(col.getSource());
        if (profile.getOptions().isTrimWhitespace() && value != null) {
            value = value.trim();
        }
        values.add(value);
    }

    // 4. 계산 컬럼 값 추가
    for (Calculation calc : profile.getCalculations()) {
        String result = calcEngine.evaluate(calc.getFormula(), row);
        if (calc.getFormat() != null && result != null) {
            try {
                double num = Double.parseDouble(result);
                result = String.format(calc.getFormat(), num);
            } catch (NumberFormatException e) {
                // 숫자가 아니면 포맷 무시
            }
        }
        values.add(calcPosition, result);
    }

    // 5. 행 쓰기
    writer.writeRow(values);
    outputRowCount.incrementAndGet();
};
```

### 7.2 ProfileManager.java (프로필 관리자)

**목적**: JSON 프로필 파일의 로드/저장/관리

**프로필 경로 우선순위:**
```java
private File findProfilesDirectory() {
    // 1. 사용자 홈 디렉토리
    File userHome = new File(System.getProperty("user.home"),
                            ".ExcelConverter/profiles");
    if (userHome.isDirectory()) return userHome;

    // 2. JAR 파일과 같은 디렉토리
    try {
        File jarDir = new File(getClass().getProtectionDomain()
            .getCodeSource().getLocation().toURI()).getParentFile();
        File jarProfiles = new File(jarDir, "profiles");
        if (jarProfiles.isDirectory()) return jarProfiles;
    } catch (Exception e) {
        // 무시
    }

    // 3. 현재 작업 디렉토리
    File currentDir = new File("profiles");
    if (!currentDir.exists()) {
        currentDir.mkdirs();
    }
    return currentDir;
}
```

**프로필 로드:**
```java
public List<Profile> loadAllProfiles() {
    List<Profile> profiles = new ArrayList<>();
    File[] jsonFiles = profilesDir.listFiles(
        (dir, name) -> name.toLowerCase().endsWith(".json")
    );

    if (jsonFiles != null) {
        ObjectMapper mapper = new ObjectMapper();
        for (File file : jsonFiles) {
            try {
                Profile profile = mapper.readValue(file, Profile.class);
                profiles.add(profile);
                cache.put(profile.getProfileName(), profile);
            } catch (Exception e) {
                System.err.println("Failed to load profile: " + file.getName());
            }
        }
    }

    return profiles;
}
```

**프로필 저장:**
```java
public void saveProfile(Profile profile) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    // 파일명: 프로필명.json
    String fileName = profile.getProfileName()
        .replaceAll("[^a-zA-Z0-9가-힣_-]", "_") + ".json";
    File file = new File(profilesDir, fileName);

    mapper.writeValue(file, profile);
    cache.put(profile.getProfileName(), profile);
}
```

### 7.3 CalculationEngine.java (수식 계산 엔진)

**목적**: 프로필의 계산 컬럼 수식 평가

**지원 기능:**
- **수학 연산**: exp4j 라이브러리 사용
- **문자열 함수**: 정규식으로 사전 처리

**수식 평가 흐름:**
```
수식 입력: "(${매출} - ${원가}) / ${매출}"
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  1. 변수 추출                                            │
│     Pattern: \$\{([^}]+)\}                              │
│     결과: [매출, 원가, 매출]                             │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  2. 변수명 변환 (exp4j는 한글 미지원)                     │
│     매출 → v0, 원가 → v1                                │
│     수식: "(v0 - v1) / v0"                              │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  3. 값 바인딩                                            │
│     v0 = row.get("매출") = 1000                         │
│     v1 = row.get("원가") = 700                          │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  4. exp4j 평가                                           │
│     Expression exp = new ExpressionBuilder("(v0-v1)/v0")│
│         .variables("v0", "v1")                          │
│         .build()                                        │
│         .setVariable("v0", 1000)                        │
│         .setVariable("v1", 700);                        │
│     double result = exp.evaluate();  // 0.3             │
└─────────────────────────────────────────────────────────┘
    │
    ▼
결과: "0.3"
```

**문자열 함수 처리:**
```java
private String evaluateTextFunctions(String formula, Map<String, String> row) {
    // LEFT(${col}, n)
    Pattern leftPattern = Pattern.compile(
        "LEFT\\(\\$\\{([^}]+)\\}\\s*,\\s*(\\d+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    Matcher leftMatcher = leftPattern.matcher(formula);
    while (leftMatcher.find()) {
        String colName = leftMatcher.group(1);
        int length = Integer.parseInt(leftMatcher.group(2));
        String value = row.getOrDefault(colName, "");
        String result = value.length() > length
            ? value.substring(0, length)
            : value;
        formula = formula.replace(leftMatcher.group(0), "\"" + result + "\"");
    }

    // RIGHT, SUBSTR/MID, TRIM도 유사하게 처리...

    return formula;
}
```

---

## 8. Reader/Writer 레이어 상세 설명

### 8.1 DataReader 인터페이스

**목적**: 파일 읽기 추상화

```java
public interface DataReader {
    // 지원 확장자 확인
    boolean supports(File file);

    // 파일 읽기 (콜백 기반)
    long read(
        File file,
        Profile profile,
        Consumer<List<String>> headerCallback,    // 헤더 읽을 때 호출
        Consumer<Map<String, String>> rowCallback, // 각 행마다 호출
        Consumer<Long> progressCallback            // 진행률 (10000행마다)
    ) throws Exception;
}
```

### 8.2 XlsxReader.java (XLSX 읽기)

**목적**: Excel XLSX 파일을 SAX 스트리밍 방식으로 읽기

**SAX 기반 스트리밍의 장점:**
- 전체 파일을 메모리에 로드하지 않음
- 수백만 행 파일도 처리 가능
- DOM 방식 대비 메모리 효율 10배 이상

**동작 방식:**
```
XLSX 파일 (실제로는 ZIP 파일)
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  OPCPackage.open(file)                                  │
│  → XLSX 내부 XML 파일들에 접근                          │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  SharedStringsTable (xl/sharedStrings.xml)              │
│  → 셀 문자열 값 조회용 테이블                           │
│  → 셀에는 인덱스만 저장, 실제 값은 이 테이블에          │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  StylesTable (xl/styles.xml)                            │
│  → 셀 스타일 정보 (날짜 형식 감지에 사용)               │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  XSSFReader.SheetIterator                               │
│  → 각 시트를 InputStream으로 순회                       │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│  XMLReader (SAX Parser)                                 │
│  → SheetHandler (ContentHandler 구현)                   │
│  → 셀 단위로 이벤트 발생                                │
│                                                         │
│  <row r="1">                                            │
│    <c r="A1" t="s"><v>0</v></c>  → startElement/endElement│
│    <c r="B1" t="n"><v>1000</v></c>                      │
│  </row>                                                 │
└─────────────────────────────────────────────────────────┘
    │
    ▼
콜백 호출 (headerCallback, rowCallback)
```

**SheetHandler 핵심 로직:**
```java
class SheetHandler extends DefaultHandler {
    private List<String> headers;
    private Map<String, String> currentRow;
    private int currentColIndex;
    private String cellType;  // s=string, n=number, b=boolean
    private StringBuilder cellValue = new StringBuilder();

    @Override
    public void startElement(String uri, String localName, String qName,
                            Attributes attributes) {
        if ("row".equals(qName)) {
            currentRow = new LinkedHashMap<>();
            currentColIndex = 0;
        } else if ("c".equals(qName)) {
            // 셀 타입 확인
            cellType = attributes.getValue("t");
            // 셀 위치 확인 (A1, B1, ...)
            String ref = attributes.getValue("r");
            currentColIndex = cellRefToIndex(ref);
        } else if ("v".equals(qName)) {
            cellValue.setLength(0);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        cellValue.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("v".equals(qName)) {
            String value = cellValue.toString();

            // 셀 타입에 따른 값 변환
            if ("s".equals(cellType)) {
                // SharedStringsTable에서 실제 문자열 조회
                int idx = Integer.parseInt(value);
                value = sharedStrings.getItemAt(idx).getString();
            } else if ("n".equals(cellType)) {
                // 날짜 형식인지 확인
                if (isDateFormat(currentStyleIndex)) {
                    value = formatDateValue(Double.parseDouble(value));
                }
            }

            if (rowNumber == 1) {
                headers.add(value);
            } else {
                currentRow.put(headers.get(currentColIndex), value);
            }
        } else if ("row".equals(qName)) {
            if (rowNumber == 1) {
                headerCallback.accept(headers);
            } else {
                rowCallback.accept(currentRow);
            }
            rowNumber++;
        }
    }
}
```

### 8.3 CsvReader.java (CSV 읽기)

**목적**: CSV 파일 읽기 (BOM 및 인코딩 자동 감지)

**BOM 감지 로직:**
```java
private Reader createReader(File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    BufferedInputStream bis = new BufferedInputStream(fis);

    // BOM 확인
    bis.mark(4);
    byte[] bom = new byte[4];
    int read = bis.read(bom);
    bis.reset();

    String encoding;
    int bomLength = 0;

    if (read >= 3 && bom[0] == (byte)0xEF &&
        bom[1] == (byte)0xBB && bom[2] == (byte)0xBF) {
        // UTF-8 BOM
        encoding = "UTF-8";
        bomLength = 3;
    } else if (read >= 2 && bom[0] == (byte)0xFF && bom[1] == (byte)0xFE) {
        // UTF-16 LE BOM
        encoding = "UTF-16LE";
        bomLength = 2;
    } else if (read >= 2 && bom[0] == (byte)0xFE && bom[1] == (byte)0xFF) {
        // UTF-16 BE BOM
        encoding = "UTF-16BE";
        bomLength = 2;
    } else {
        // 기본: EUC-KR (한글 레거시 호환)
        encoding = "EUC-KR";
    }

    // BOM 건너뛰기
    bis.skip(bomLength);

    return new InputStreamReader(bis, encoding);
}
```

**읽기 로직:**
```java
public long read(File file, Profile profile,
                Consumer<List<String>> headerCallback,
                Consumer<Map<String, String>> rowCallback,
                Consumer<Long> progressCallback) throws Exception {

    try (Reader reader = createReader(file);
         CSVReader csvReader = new CSVReader(reader)) {

        // 1. 헤더 읽기
        String[] headerArray = csvReader.readNext();
        List<String> headers = Arrays.asList(headerArray);
        headerCallback.accept(headers);

        // 2. 데이터 행 읽기
        long rowCount = 0;
        String[] row;
        while ((row = csvReader.readNext()) != null) {
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(headers.size(), row.length); i++) {
                rowMap.put(headers.get(i), row[i]);
            }
            rowCallback.accept(rowMap);

            rowCount++;
            if (rowCount % 10000 == 0) {
                progressCallback.accept(rowCount);
            }
        }

        return rowCount;
    }
}
```

### 8.4 DataWriter 인터페이스

**목적**: 파일 쓰기 추상화

```java
public interface DataWriter extends AutoCloseable {
    void open(File file, Profile profile) throws IOException;
    void writeHeader(List<String> headers) throws IOException;
    void writeRow(List<String> values) throws IOException;
    void close() throws IOException;
    String getExtension();
    String getFormatName();
}
```

### 8.5 CsvDataWriter.java (CSV 쓰기)

**목적**: CSV 파일 쓰기 (BOM 및 인코딩 지원)

```java
public class CsvDataWriter implements DataWriter {
    private CSVWriter writer;
    private OutputStreamWriter streamWriter;

    @Override
    public void open(File file, Profile profile) throws IOException {
        String encoding = profile.getOptions().getOutputEncoding();
        FileOutputStream fos = new FileOutputStream(file);

        // UTF-8-BOM인 경우 BOM 바이트 직접 추가
        if ("UTF-8-BOM".equalsIgnoreCase(encoding)) {
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);
            encoding = "UTF-8";
        }

        streamWriter = new OutputStreamWriter(fos, encoding);

        char delimiter = profile.getOptions().getDelimiter().charAt(0);
        writer = new CSVWriter(streamWriter, delimiter,
            CSVWriter.DEFAULT_QUOTE_CHARACTER,
            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
            CSVWriter.DEFAULT_LINE_END);
    }

    @Override
    public void writeHeader(List<String> headers) throws IOException {
        writer.writeNext(headers.toArray(new String[0]));
    }

    @Override
    public void writeRow(List<String> values) throws IOException {
        writer.writeNext(values.toArray(new String[0]));
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
```

### 8.6 XlsxDataWriter.java (XLSX 쓰기)

**목적**: Excel XLSX 파일을 SXSSF 스트리밍 방식으로 쓰기

**SXSSF 스트리밍의 장점:**
- 메모리에 지정된 행 수만 유지 (기본 100행)
- 나머지는 임시 파일로 플러시
- 대용량 파일 생성 가능

```java
public class XlsxDataWriter implements DataWriter {
    private SXSSFWorkbook workbook;
    private SXSSFSheet sheet;
    private int currentRow = 0;
    private CellStyle headerStyle;

    @Override
    public void open(File file, Profile profile) throws IOException {
        // 100행만 메모리에 유지
        workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);  // 임시 파일 압축

        sheet = workbook.createSheet("Data");

        // 헤더 스타일 (굵게)
        headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
    }

    @Override
    public void writeHeader(List<String> headers) throws IOException {
        Row row = sheet.createRow(currentRow++);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    @Override
    public void writeRow(List<String> values) throws IOException {
        Row row = sheet.createRow(currentRow++);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            String value = values.get(i);

            // 숫자인지 확인하여 적절한 타입으로 저장
            if (value != null && !value.isEmpty()) {
                try {
                    double num = Double.parseDouble(value);
                    cell.setCellValue(num);
                } catch (NumberFormatException e) {
                    cell.setCellValue(value);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (workbook != null) {
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
            workbook.dispose();  // 임시 파일 정리
        }
    }
}
```

---

## 9. 데이터 변환 프로세스 상세

### 9.1 전체 변환 흐름도

```
┌─────────────────────────────────────────────────────────────────────┐
│                      사용자가 Convert 클릭                           │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  MainFrame.startConversion()                                        │
│  ├─ 프로필 선택 검증                                                 │
│  ├─ 파일 목록 검증                                                   │
│  ├─ 출력 디렉토리 검증                                               │
│  └─ ConversionWorker 생성 및 실행                                    │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ConversionWorker.doInBackground() [Worker Thread]                  │
│  └─ ExcelConverterService.convert()                                 │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  각 입력 파일에 대해:                                                │
│                                                                     │
│  1. Reader 선택                                                     │
│     ├─ .xlsx → XlsxReader                                          │
│     └─ .csv  → CsvReader                                           │
│                                                                     │
│  2. Writer 생성                                                     │
│     ├─ csv 포맷  → CsvDataWriter                                   │
│     └─ xlsx 포맷 → XlsxDataWriter                                  │
│                                                                     │
│  3. 변환 실행                                                       │
│     reader.read(file, profile, headerCallback, rowCallback)         │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  헤더 콜백 (headerCallback)                                         │
│  ├─ 필수 컬럼 존재 여부 검증                                         │
│  ├─ 컬럼 인덱스 매핑 생성                                            │
│  └─ writer.writeHeader(outputHeaders)                               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  각 행에 대해 (rowCallback):                                         │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 1. 빈 행 체크 (skipEmptyRows)                                │   │
│  │    → 빈 행이면 건너뛰기                                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                          │                                          │
│                          ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 2. 중복 체크 (uniqueKey 컬럼 기반)                           │   │
│  │    → 이미 본 키면 건너뛰기                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                          │                                          │
│                          ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 3. 매핑된 컬럼 값 추출                                        │   │
│  │    → trimWhitespace 적용                                      │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                          │                                          │
│                          ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 4. 계산 컬럼 값 계산                                          │   │
│  │    → CalculationEngine.evaluate()                            │   │
│  │    → format 적용                                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                          │                                          │
│                          ▼                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 5. writer.writeRow(values)                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  변환 완료                                                          │
│  ├─ writer.close()                                                  │
│  ├─ ConversionResult 생성 (통계 포함)                               │
│  └─ 콜백: onComplete(results)                                       │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ConversionWorker.done() [EDT]                                      │
│  └─ 결과 다이얼로그 표시                                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 병합 변환 모드

**병합 모드 차이점:**
- 여러 파일을 하나의 출력 파일로 통합
- 첫 번째 파일에서만 헤더 쓰기
- 중복 제거는 모든 파일 통합 기준

```java
private List<ConversionResult> convertMerged(...) {
    DataWriter writer = createWriter(profile);
    File outputFile = new File(outputDir,
        profile.getOutputFileName() + "_merged." + writer.getExtension());
    writer.open(outputFile, profile);

    Set<String> allSeenKeys = new HashSet<>();  // 전체 파일 공유
    boolean headerWritten = false;

    for (File inputFile : inputFiles) {
        DataReader reader = findReader(inputFile);

        reader.read(inputFile, profile,
            headers -> {
                if (!headerWritten) {
                    writer.writeHeader(profile.getOutputColumnNames());
                    headerWritten = true;
                }
            },
            row -> {
                // 전체 파일 기준 중복 체크
                String key = buildUniqueKey(profile, row);
                if (key != null && allSeenKeys.contains(key)) {
                    return;  // 중복 건너뛰기
                }
                if (key != null) allSeenKeys.add(key);

                // 값 추출 및 쓰기
                List<String> values = extractValues(profile, row);
                writer.writeRow(values);
            },
            progress -> {}
        );
    }

    writer.close();
    return Collections.singletonList(mergedResult);
}
```

---

## 10. 클래스 간 상호작용

### 10.1 전체 클래스 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Main                                       │
│                                │                                        │
│                                ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                         MainFrame                                 │  │
│  │  ┌─────────────────┐  ┌────────────────────┐                     │  │
│  │  │ ProfileManager  │  │ ExcelConverterService │                   │  │
│  │  └────────┬────────┘  └──────────┬─────────┘                     │  │
│  │           │                      │                                │  │
│  │           │                      │                                │  │
│  └───────────│──────────────────────│────────────────────────────────┘  │
│              │                      │                                   │
│              ▼                      ▼                                   │
│  ┌───────────────────┐   ┌────────────────────────────────────────┐    │
│  │   Profile (JSON)  │   │           변환 엔진                     │    │
│  │  ├─ ColumnMapping │   │  ┌──────────────────────────────────┐  │    │
│  │  ├─ Calculation   │   │  │        DataReader (interface)    │  │    │
│  │  └─ OutputOptions │   │  │  ├─ XlsxReader                   │  │    │
│  └───────────────────┘   │  │  └─ CsvReader                    │  │    │
│                          │  └──────────────────────────────────┘  │    │
│                          │  ┌──────────────────────────────────┐  │    │
│                          │  │        DataWriter (interface)    │  │    │
│                          │  │  ├─ CsvDataWriter                │  │    │
│                          │  │  └─ XlsxDataWriter               │  │    │
│                          │  └──────────────────────────────────┘  │    │
│                          │  ┌──────────────────────────────────┐  │    │
│                          │  │      CalculationEngine           │  │    │
│                          │  └──────────────────────────────────┘  │    │
│                          └────────────────────────────────────────┘    │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    ConversionWorker (SwingWorker)                 │  │
│  │                           │                                       │  │
│  │                           ▼                                       │  │
│  │                   ConversionResult                                │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    ProfileEditorDialog                            │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 10.2 의존성 관계

```
MainFrame
  ├── depends on → ProfileManager (프로필 로드/저장)
  ├── depends on → ExcelConverterService (변환 실행)
  ├── creates → ConversionWorker (백그라운드 작업)
  └── creates → ProfileEditorDialog (프로필 편집)

ConversionWorker
  ├── uses → ExcelConverterService.convert()
  └── produces → List<ConversionResult>

ExcelConverterService
  ├── uses → DataReader implementations (XlsxReader, CsvReader)
  ├── uses → DataWriter implementations (CsvDataWriter, XlsxDataWriter)
  ├── uses → CalculationEngine (수식 계산)
  └── reads → Profile (변환 규칙)

ProfileManager
  ├── manages → Profile objects
  ├── reads/writes → JSON files in profiles/ directory
  └── uses → Jackson ObjectMapper

CalculationEngine
  ├── uses → exp4j (수학 수식 평가)
  └── uses → Pattern/Matcher (문자열 함수 처리)

XlsxReader
  └── uses → Apache POI (OPCPackage, XSSFReader, SAX)

XlsxDataWriter
  └── uses → Apache POI (SXSSFWorkbook, SXSSF streaming)

CsvReader / CsvDataWriter
  └── uses → OpenCSV
```

### 10.3 데이터 흐름

```
[입력 파일] → DataReader → Map<String, String> (행 데이터)
                              │
                              ▼
                    ColumnMapping (컬럼 선택/변환)
                              │
                              ▼
                    CalculationEngine (계산 컬럼)
                              │
                              ▼
                    List<String> (출력 값)
                              │
                              ▼
              DataWriter → [출력 파일]
```

---

## 부록: 빌드 및 실행

### A. Gradle 빌드 명령

```bash
# 컴파일
./gradlew build

# Fat JAR 생성 (모든 의존성 포함)
./gradlew jar

# 실행
./gradlew run

# jpackage로 설치 파일 생성
./gradlew jpackage

# 배포 패키지 (ZIP) 생성
./gradlew distPackage
```

### B. 직접 실행

```bash
# JAR 실행
java -jar build/libs/ExcelConverter-1.0.jar

# 또는 스크립트 사용
./run.sh      # macOS/Linux
run.bat       # Windows
```

### C. 프로필 예시

`profiles/sample_profile.json`:
```json
{
  "profileName": "매출데이터_변환",
  "description": "월별 매출 데이터를 정제된 형식으로 변환",
  "version": "1.0",
  "columns": [
    {"source": "기간/연도", "target": "연도", "type": "string", "required": true},
    {"source": "매출금액", "target": "매출", "type": "number", "required": true},
    {"source": "원가", "type": "number", "required": true},
    {"source": "상품코드", "type": "string", "required": true, "uniqueKey": true}
  ],
  "calculations": [
    {
      "newColumn": "마진",
      "formula": "${매출} - ${원가}",
      "insertAfter": "원가"
    },
    {
      "newColumn": "마진율",
      "formula": "(${매출} - ${원가}) / ${매출} * 100",
      "insertAfter": "마진",
      "format": "%.1f"
    }
  ],
  "options": {
    "skipEmptyRows": true,
    "trimWhitespace": true,
    "outputEncoding": "UTF-8-BOM",
    "outputFormat": "csv"
  },
  "outputFileName": "%name%_converted"
}
```

---

*이 문서는 ExcelConverterMusinsa 프로젝트의 시스템 아키텍처와 동작 방식을 상세히 설명합니다.*
*작성일: 2026-02-01*
