# WSL Android SDK 빌드 검증

## 목적

Windows Android Studio 설치본이 없거나 경로가 다른 개발 PC에서도 `shipmate-android`를 WSL에서 검증할 수 있게 한다.

## 기본 원칙

- Android SDK는 저장소에 커밋하지 않는다.
- 검증용 SDK는 워크스페이스의 `.codex-tmp/android-sdk` 아래에 둔다.
- `local.properties`는 로컬 전용 파일이므로 공용 설정으로 커밋하지 않는다.
- `local.properties`의 Windows SDK 경로가 없어도 `ANDROID_HOME`, `ANDROID_SDK_ROOT` 환경변수로 빌드할 수 있다.

## 설치 위치

```text
.codex-tmp/android-sdk/
  cmdline-tools/latest/
  platforms/android-36/
  build-tools/34.0.0/
  build-tools/36.0.0/
  platform-tools/
```

## 초기 설치 절차

```bash
mkdir -p .codex-tmp/android-sdk/cmdline-tools .codex-tmp/android-cmdline-tools
curl -fsS https://dl.google.com/android/repository/commandlinetools-linux_latest.zip \
  -o .codex-tmp/commandlinetools-linux_latest.zip
unzip -q .codex-tmp/commandlinetools-linux_latest.zip -d .codex-tmp/android-cmdline-tools
mv .codex-tmp/android-cmdline-tools/cmdline-tools .codex-tmp/android-sdk/cmdline-tools/latest
```

라이선스와 필수 패키지:

```bash
yes | JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
ANDROID_SDK_ROOT="$PWD/.codex-tmp/android-sdk" \
.codex-tmp/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses

JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
ANDROID_SDK_ROOT="$PWD/.codex-tmp/android-sdk" \
.codex-tmp/android-sdk/cmdline-tools/latest/bin/sdkmanager \
  'platforms;android-36' \
  'build-tools;36.0.0' \
  'build-tools;34.0.0' \
  'platform-tools'
```

## 빌드 검증

```bash
cd shipmate-android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
GRADLE_USER_HOME="$PWD/../.codex-tmp/android-gradle-home" \
ANDROID_HOME="$PWD/../.codex-tmp/android-sdk" \
ANDROID_SDK_ROOT="$PWD/../.codex-tmp/android-sdk" \
./gradlew assembleDebug \
  -Dorg.gradle.java.home=/usr/lib/jvm/java-17-openjdk \
  --no-daemon --console=plain
```

## 오류 처리

| 증상 | 처리 |
|---|---|
| `SDK location not found` | `ANDROID_HOME`, `ANDROID_SDK_ROOT`가 `.codex-tmp/android-sdk`를 가리키는지 확인한다. |
| `sdk.dir property ... Directory does not exist` | 경고만 나오고 환경변수 SDK로 빌드가 진행되면 그대로 둔다. |
| Gradle lock이 read-only home에 생김 | `GRADLE_USER_HOME`을 `.codex-tmp/android-gradle-home`으로 지정한다. |
| 샌드박스에서 `Operation not permitted` | 외부 실행 권한으로 같은 빌드 명령을 재실행한다. |

## 검증 기록

2026-06-24 기준 WSL `.codex-tmp/android-sdk` 구성 후 `assembleDebug`가 통과했다.
