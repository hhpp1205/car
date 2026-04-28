# 빌드 · 배포 가이드

이 문서는 **개발자(님)** 가 신규 버전을 빌드해서 **지인(사용자) PC** 에 전달하는 절차를 정리한다. 한 번 셋업해두면 매 릴리스는 `./gradlew jpackage` 한 줄.

---

## 1. 개발자 PC 셋업 (최초 1회)

| 항목 | 다운로드 | 비고 |
|---|---|---|
| **Liberica Full JDK 17** | https://bell-sw.com/pages/downloads/?version=java-17-lts → **Full JDK** | JavaFX·jpackage 가 이미 포함된 배포판. 일반 OpenJDK 는 JavaFX 모듈이 빠져있어 빌드 실패. |
| **WiX Toolset 3.14** | https://github.com/wixtoolset/wix3/releases → `wix314.exe` | jpackage 가 .exe 인스톨러를 만들 때 내부적으로 호출. 설치하면 PATH 자동 등록. |

설치 확인:

```bash
java -version    # 17.x ("Liberica" 또는 "BellSoft" 표기 확인)
jpackage --version
where candle     # WiX 의 candle.exe 가 PATH 에 잡히면 OK
```

---

## 2. 버전 변경 + 릴리스 노트

릴리스마다 두 가지를 갱신한다.

1. `build.gradle` 의 `version = '0.x.0'` 수정
2. `CHANGELOG.md` 상단에 변경사항 한 단락 추가 (사용자에게 "뭐가 바뀌었는지" 안내용)

DB 스키마를 변경한 릴리스는 반드시 [`MIGRATIONS.md`](MIGRATIONS.md) 의 절차를 먼저 따른다.

---

## 3. 빌드

```bash
./gradlew check       # 1) 테스트 그린 확인
./gradlew jpackage    # 2) .exe 인스톨러 생성
```

**산출물**: `build/jpackage/전북공업사-0.1.0.exe`  (~80 MB, JDK · JavaFX 포함)

처음 실행 시 5~10분 걸릴 수 있다 (jpackage 가 jlink 로 커스텀 런타임을 만드는 단계). 이후는 캐시.

### 빌드 옵션 미세조정 (선택)

`build.gradle` 의 `jpackage` 태스크 안 `cmd` 리스트에서:

| 옵션 | 효과 |
|---|---|
| `--win-per-user-install` | 현재 사용자에게만 설치 (관리자 권한 불필요). 제거하면 Program Files 에 시스템 전역 설치 — 관리자 권한 요구. |
| `--win-shortcut` | 바탕화면 바로가기 생성 |
| `--win-menu` + `--win-menu-group` | 시작메뉴 등록 |
| `--win-dir-chooser` | 설치 시 폴더 선택 다이얼로그 표시 |
| `--icon src/main/resources/icon.ico` | 아이콘 (`.ico` 256x256). 파일이 있으면 자동 적용. |

---

## 4. 지인에게 전달

`전북공업사-0.x.0.exe` 1개만 전달하면 끝. 전달 수단:

- USB 메모리 (오프라인 환경 권장)
- 메일 첨부 — 80 MB 라 회사 메일 첨부 한도 확인 필요
- Google Drive · OneDrive 같은 클라우드 공유 링크

지인 측 절차:

1. `.exe` 더블클릭
2. 설치 마법사 진행 (다음 → 다음 → 설치)
3. 시작메뉴 또는 바탕화면 바로가기로 실행

JDK · 환경변수 등 별도 설치 불필요. **첫 실행 시** `%APPDATA%\전북공업사\repair.db` 가 자동 생성된다.

---

## 5. 사용자 데이터 위치

| 항목 | 경로 |
|---|---|
| DB 본체 | `%APPDATA%\전북공업사\repair.db` |
| 자동 백업 | `%APPDATA%\전북공업사\backup\repair-YYYYMMDD.db` (앱 시작 시 1일 1회) |
| 로그 | (현재) 프로젝트 폴더 — 차후 같은 경로로 이전 검토 |

`%APPDATA%` 는 사용자별 폴더이므로 **앱을 재설치/업데이트해도 데이터가 보존된다.** 백업 파일은 최근 10개까지만 유지된다 (`BackupUtil.KEEP`).

다른 위치에 저장하고 싶으면 환경변수로 오버라이드:

```bash
set JEONBUK_REPAIR_HOME=D:\전북공업사데이터
```

(주로 개발자 본인이 테스트할 때 — 지인 PC 에선 건드릴 필요 없음.)

---

## 6. 업데이트 배포 흐름

```
[개발자 PC]                                [지인 PC]
1. 코드 수정 + 마이그레이션 SQL 추가
2. ./gradlew check
3. ./gradlew jpackage
4. .exe + CHANGELOG 발췌 전달   ───────►   기존 앱 위에 .exe 더블클릭
                                            ├ %APPDATA%\전북공업사\ 데이터 보존
                                            ├ 첫 실행 시 자동 백업 생성
                                            └ 마이그레이션 자동 적용
```

업데이트 시 지인이 따로 할 일 없음 — `.exe` 만 더블클릭.

만약 마이그레이션이 잘못 짜였다면 `%APPDATA%\전북공업사\backup\` 의 어제자 백업을 `repair.db` 로 이름 바꿔 복구하면 된다.

---

## 7. 트러블슈팅

| 증상 | 원인 / 해결 |
|---|---|
| `jpackage: command not found` | JDK 가 jpackage 미포함. Liberica Full JDK 17 로 재설치. |
| `WiX Tools not found` | WiX Toolset 3.14 미설치 또는 PATH 미등록. `wix314.exe` 재설치 후 새 터미널. |
| 빌드는 성공했는데 .exe 가 실행되자마자 닫힘 | 보통 JavaFX 모듈 로딩 실패. `Launcher` 클래스가 main 인지 확인 (`build.gradle` 의 `--main-class com.jeonbuk.repair.Launcher`). |
| 지인 PC 에서 한글이 깨짐 | `--java-options '-Dfile.encoding=UTF-8'` 적용됐는지 확인. |
| 업데이트 후 데이터가 비어있음 | DB 경로가 프로젝트 폴더로 잘못 잡혔을 수 있음. `%APPDATA%\전북공업사\repair.db` 존재 확인. 없으면 오래된 빌드 — 최신 빌드로 재설치. |
