# DS Smart Link Android

반도체 후공정 비전 검사 장비 실시간 모니터링 모바일 앱 (Android).  
MQTT를 통해 장비 이벤트를 실시간 수신하고, Web-Backend REST API로 상세 데이터를 조회합니다.

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java |
| 플랫폼 | Android (minSdk 기준) |
| MQTT | Eclipse Paho Android |
| HTTP | Retrofit2 |
| 빌드 | Gradle |

## 디렉토리 구조

```
Smart_Link_android/app/src/main/java/com/smartfactory/visioninspection/
├── activities/         # LoginActivity, MainActivity
├── adapters/           # RecyclerView 어댑터 (장비, 알람, 검사 결과 등)
├── bottomsheets/       # LOT 상세 Bottom Sheet
├── fragments/          # Dashboard, Equipment, Feed, Report, Settings
├── models/             # 데이터 모델 (LOT, 알람, 장비 상태 등)
├── network/            # Retrofit API 클라이언트 (인증)
└── utils/              # 세션 관리, Mock 데이터, 알람 설정
```

## 주요 화면

| 탭 | 설명 |
|---|---|
| Dashboard | 전체 라인 장비 상태 타일 (실시간 N:1 모니터링) |
| Equipment | 장비 목록 및 상태 조회 |
| Feed | 실시간 이벤트 피드 (알람, 검사 결과) |
| Report | LOT 보고서 조회 |
| Settings | 알람 설정, 계정 관리 |

## MQTT 구독

- `ds/#` — 전체 장비 이벤트 실시간 수신
- 특히 `ds/+/status`, `ds/+/alarm`, `ds/+/oracle` 이벤트 처리

## 빌드 방법

```bash
# Android Studio에서 프로젝트 열기
# 또는 CLI 빌드:

cd Smart_Link_android
./gradlew assembleDebug    # 디버그 APK
./gradlew assembleRelease  # 릴리즈 APK
```

## 인증

Web-Backend(`localhost:8443`)에서 JWT 토큰 발급 후 API 요청에 사용.  
`network/AuthApi.java` 참조.
