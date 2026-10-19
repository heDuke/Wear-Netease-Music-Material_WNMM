# NetMusic for WearOS

Wear OS 网易云客户端。Kotlin 原生实现 [api-enhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced) 的 weapi/eapi，手表直连网易云，不跑 Node。

- ApplicationId: `com.refresh.netmusic`
- UI: Wear Compose Material 3 1.8 + Horologist media/audio
- 网络: Ktor CIO
- 播放: Media3（允许手表扬声器）
- 会话: Tink AES-256-GCM（Keystore）加密 Cookie / uid

## 工具链（当前最新）

| 组件 | 版本 |
|---|---|
| Gradle | 9.8.1 |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.21 |
| Wear Compose | 1.8.0-alpha01 |
| Compose BOM | 2026.09.00 |
| Navigation 3 | 1.3.0-alpha02 |
| Horologist | 0.8.4-alpha |
| Tink | 1.24.0 |
| Lifecycle | 2.11.0 |
| Ktor | 3.6.0 |
| Koin | 4.2.2 |
| Media3 | 1.11.1 |
| Coil | 3.6.3 |
| Coroutines | 1.11.0 |
| Serialization | 1.11.0 |
| DataStore | 1.2.1 |
| Wear Tiles | 1.6.2 |

## 编译

```bash
git clone --recurse-submodules <repo>
cd wear
./gradlew :ncm:test :app:assembleDebug :app:assembleRelease
```

APK 输出：

- `wear/app/build/outputs/apk/debug/`
- `wear/app/build/outputs/apk/release/`

## 登录

手表显示 Material You 造型二维码，用手机网易云扫描确认。

## CI

Push / PR 会跑测试并上传 Debug + Release APK。打 `v*` tag 会发 GitHub Release。

Release 签名可选 Secrets：

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

未配置时仍会产出 debug 签名的 release 包。

仅供个人学习使用。
