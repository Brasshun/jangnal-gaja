# 카카오맵 API 연동 가이드

앱에서 지도를 보려면 카카오 개발자 사이트에서 API 키를 발급받아야 합니다.

## 🔑 1. 카카오 개발자 계정 및 앱 생성

1. [카카오 개발자 사이트](https://developers.kakao.com/) 접속 및 로그인
2. **내 애플리케이션** → **애플리케이션 추가하기**
3. 앱 이름 입력 (예: "장날가자"), 사업자명 선택 (개인) 후 저장
4. 생성된 앱 선택

## 📱 2. 플랫폼 설정 (중요!)

### Android 플랫폼 등록
1. 좌측 메뉴 **플랫폼** → **Android 플랫폼 등록** 클릭
2. **패키지명**: `com.jangnal.gaja` 입력
3. **키 해시** 등록 (필수!)

### 키 해시 확인 방법

#### 방법 1: 명령어로 확인 (권장)
Windows PowerShell에서 실행:
```powershell
keytool -exportcert -alias androiddebugkey -keystore $env:USERPROFILE\.android\debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
```

macOS/Linux에서 실행:
```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
```

#### 방법 2: 앱 로그에서 확인 (가장 정확)
1. 앱을 실행하고 지도 탭으로 이동
2. Android Studio의 **Logcat**에서 `KakaoMapScreen` 또는 `KakaoMap` 필터링
3. 에러 로그에 표시되는 키 해시 복사
4. 카카오 개발자 사이트에 등록

## 🔐 3. API 키 발급 및 설정

1. 카카오 개발자 사이트에서 생성한 앱 선택
2. **요약 정보** 탭에서 **앱 키** 확인
3. **네이티브 앱 키** 복사

### AndroidManifest.xml에 키 입력

`app/src/main/AndroidManifest.xml` 파일을 열고 아래 부분을 찾아 키를 붙여넣으세요:

```xml
<meta-data
    android:name="com.kakao.sdk.AppKey"
    android:value="여기에_네이티브_앱_키를_붙여넣으세요" />
```

**현재 설정된 키**: `e88d8e377ad7672c2ecdc80df4123e1a`
→ 이 키가 본인의 카카오 개발자 계정에서 발급받은 키인지 확인하세요!

## ✅ 4. 확인 사항 체크리스트

- [ ] 카카오 개발자 사이트에서 앱 생성 완료
- [ ] Android 플랫폼 등록 완료
- [ ] 패키지명 `com.jangnal.gaja` 등록 확인
- [ ] 키 해시 등록 완료
- [ ] AndroidManifest.xml에 네이티브 앱 키 입력 완료
- [ ] 인터넷 권한 설정 확인 (이미 설정됨)
- [ ] `usesCleartextTraffic="true"` 설정 확인 (이미 설정됨)

## 🚀 5. 실행 및 테스트

1. Android Studio에서 **Sync Project with Gradle Files** 실행
2. 앱 빌드 및 실행
3. 지도 탭으로 이동
4. 지도가 정상적으로 표시되는지 확인

### 문제 해결

#### 지도가 안 보이고 에러 메시지가 표시되는 경우

**"카카오 맵 인증 실패" 에러**
- AndroidManifest.xml의 API 키 확인
- 카카오 개발자 사이트에서 플랫폼 설정 확인
- 키 해시가 올바르게 등록되었는지 확인

**"네트워크 연결을 확인해주세요" 에러**
- 인터넷 연결 확인
- 에뮬레이터/기기의 네트워크 설정 확인

**로딩만 계속되는 경우**
- Logcat에서 `KakaoMapScreen` 태그로 필터링하여 에러 로그 확인
- 키 해시 등록 여부 재확인

## 📝 참고 사항

- **개발용 키 해시**와 **배포용 키 해시**는 다릅니다
- 배포 시에는 릴리즈 키스토어의 키 해시를 추가로 등록해야 합니다
- 최대 500개의 마커가 지도에 표시됩니다 (성능 최적화)

## 🔗 추가 자료

- [카카오맵 SDK 공식 문서](https://apis.map.kakao.com/android/)
- [카카오 개발자 사이트](https://developers.kakao.com/)
