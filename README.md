# 통화 비서 (CallRecorder) - Android

소상공인을 위한 AI 통화 비서 앱입니다. 통화가 끝나면 자동으로 녹음 파일을 서버에 업로드하고 STT/요약을 받아옵니다.

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DI**: 수동 DI (AppContainer) — Hilt 없이 가볍게
- **네트워크**: Retrofit + OkHttp + Kotlinx Serialization
- **로컬 DB**: Room
- **저장소**: DataStore (토큰)
- **백그라운드**: WorkManager (주기 스캔/업로드) + Foreground Service (실시간 감지)
- **로그인**: Kakao SDK v2
- **최소 버전**: Android 8.0 (API 26) — 시장 점유율 99%+

## 폴더 구조

```
app/src/main/java/com/callrecorder/app/
├── CallRecorderApp.kt          # Application + 초기화
├── MainActivity.kt              # 네비게이션 호스트
├── data/
│   ├── api/                     # Retrofit (ApiService, ApiClient)
│   ├── local/                   # Room (RecordingEntity), TokenStore
│   ├── model/                   # DTO
│   └── repository/              # Auth/Store/Call Repository
├── di/AppContainer.kt          # 의존성 컨테이너
├── service/
│   ├── RecordingObserverService.kt  # ContentObserver 포그라운드 서비스
│   └── BootReceiver.kt         # 부팅 시 재시작
├── worker/
│   ├── UploadWorker.kt          # 즉시 업로드
│   └── ScanAndUploadWorker.kt  # 15분 주기 스캔+업로드
├── ui/
│   ├── theme/                   # Material 3 테마
│   └── screens/                 # 로그인, 권한, 가게, 통화목록, 상세, 설정
└── util/RecordingScanner.kt    # 제조사별 녹음 폴더 + MediaStore 스캐너
```

## 실행 전 준비

### 1. 카카오 네이티브 앱 키 발급

[카카오 디벨로퍼스](https://developers.kakao.com)에서 앱을 만들고 **네이티브 앱 키**를 받습니다.

`gradle.properties` 수정:

```properties
KAKAO_NATIVE_APP_KEY=발급받은_키
```

카카오 콘솔의 **플랫폼 → Android** 등록도 필요합니다:
- 패키지명: `com.callrecorder.app`
- 키 해시: 디버그/릴리즈 키스토어에서 추출 (`keytool -exportcert ... | openssl sha1 -binary | openssl base64`)

### 2. Firebase

`call-recorder-prod` 프로젝트에서 `google-services.json`을 받아 `app/` 폴더에 넣습니다.

### 3. API 베이스 URL

이미 설정되어 있습니다. 변경 시 `gradle.properties`의 `API_BASE_URL`을 수정하세요.

## 아키텍처: 어떻게 자동으로 통화를 잡는가

ContentObserver와 WorkManager를 **둘 다** 사용해서 누락이 없도록 했습니다.

### 1차: ContentObserver (실시간)

`RecordingObserverService`가 포그라운드 서비스로 살아 있으면서 `MediaStore.Audio` 변경을 감지합니다. 통화가 끝나 녹음 파일이 저장되는 순간 즉시 트리거됩니다.

- 디바운스 2초 (녹음 버퍼 플러시 대기)
- 최근 1시간 내 파일만 스캔하여 효율 극대화
- 감지 직후 `UploadWorker.enqueueOneShot()` → 네트워크 연결 시 즉시 업로드

### 2차: 주기 워커 (백업)

`ScanAndUploadWorker`가 15분마다 실행됩니다. 옵저버가 OS에 의해 죽었거나, 앱이 강제 종료된 동안의 녹음을 챙깁니다.

- 최근 7일 내 파일을 스캔
- DB에 없으면 등록 후 업로드

### 부팅 시 재시작

`BootReceiver`가 `BOOT_COMPLETED`를 받아 옵저버 서비스와 주기 워커를 다시 등록합니다.

## 통화 녹음 파일 위치 (제조사별)

`RecordingScanner`는 다음 경로를 모두 훑고 MediaStore도 함께 쿼리합니다:

| 제조사 | 경로 |
|---|---|
| 삼성 | `/Recordings/Call/`, `/Sounds/CallRecord/` |
| LG | `/CallRecord/` |
| 샤오미 | `/MIUI/sound_recorder/call_rec/` |
| 화웨이 | `/Sounds/CallRecord/` |
| Pixel | `/Recordings/` |

## 멱등성 / 중복 방지

`RecordingEntity.filePath`에 unique index가 걸려 있어 같은 파일을 두 번 등록하지 않습니다. `Insert(OnConflictStrategy.IGNORE)`로 멱등 보장.

## 권한 흐름

1. 로그인 직후 `PermissionScreen`에서 안내
2. Android 13+: `READ_MEDIA_AUDIO` + `POST_NOTIFICATIONS`
3. Android 12 이하: `READ_EXTERNAL_STORAGE`
4. 배터리 최적화 제외 (선택, 권장)

## 빌드

### 첫 실행 시 (gradle-wrapper.jar 받기)

ZIP에는 용량 문제로 `gradle/wrapper/gradle-wrapper.jar`가 빠져 있습니다. **Android Studio**로 프로젝트를 열면 자동으로 받습니다.

수동으로 받고 싶다면:

```bash
# 프로젝트 루트에서
gradle wrapper --gradle-version 8.5
```

또는 [공식 위치](https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar)에서 받아 `gradle/wrapper/gradle-wrapper.jar`로 저장.

### Android Studio로 열기 (권장)

1. Android Studio (Hedgehog 2023.1.1 이상) 실행
2. **File → Open** → 압축 푼 `CallRecorder` 폴더 선택
3. Gradle Sync 자동 진행 (몇 분 소요)
4. 우측 상단 ▶ 버튼으로 디바이스에 설치

### 커맨드라인

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# 실기기에 설치
./gradlew installDebug
```

## 알려진 이슈 / TODO

- [ ] FCM 토큰을 서버에 등록해서 요약 완료 푸시 받기 (현재 채널만 만들어 둠)
- [ ] 로그인 토큰 만료 시 자동 refresh (현재는 재로그인 필요)
- [ ] 통화 상세 화면에서 오디오 재생
- [ ] 가게별 색상/이모지 커스터마이징
- [ ] 통화 검색 / 필터 (날짜, 상대방)

## 백엔드 API 매핑

| 화면 액션 | API |
|---|---|
| 카카오 로그인 | `POST /auth/kakao` |
| 가게 목록 | `GET /stores` |
| 가게 추가 | `POST /stores` |
| 업로드 URL 발급 | `POST /calls/upload` |
| S3 업로드 | `PUT {presigned_url}` |
| STT/요약 시작 | `POST /calls/{id}/process` |
| 통화 목록 | `GET /calls?store_id=...` |
| 통화 상세 | `GET /calls/{id}` |
| 요약 단독 조회 | `GET /summaries/{id}` |
