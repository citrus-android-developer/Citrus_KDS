---
type: issue
status: todo
priority: P1
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/issue
  - status/todo
  - priority/P1
summary: |-
  FLAG:SECURITY,INTERNAL_TRUST_ONLY
  KEY:sha3_256()寫死常數所有請求同KEY,APK下載HTTP無簽章,HttpLogging BODY level在release會印出敏感資料,首次驗證演算法在client
  DEP:[[依賴注入]][[版本更新系統]][[首次安裝驗證]][[POS-API端點]]
---
# ISSUE: 硬編 ApiKey + HTTP 更版 + Logging on release

## 現象

`AppModule.kt:42-49` OkHttp 攔截器為所有請求加 `ApiKey` 標頭，值為：

```kotlin
"CitrusCompassKDS".sha3_256()
```

但 `sha3_256()` 實作（`Constants.kt:178-181`）是**寫死回傳常數**：

```kotlin
fun String.sha3_256(): String {
    return "5EC8433D25C759DD6BB965090F6835C77BB569CE86F3713B2D364E642F693280"
}
```

不論輸入是什麼字串都回傳同一個值。所有請求帶同一個 ApiKey。

## 連帶風險

1. **APK 下載走 HTTP**（`KtorDownloadUseCase.kt:36`）`http://hq.citrus.tw/apk/...`
   - 中間人可替換 APK
   - 無簽章驗證
2. **HttpLoggingInterceptor 用 BODY level**（[[依賴注入]]）
   - release build 也會印出所有請求/回應 body
   - 含可能的訂單資料、店家資訊
3. **驗證演算法寫死 client**（[[首次安裝驗證]]）
   - 反編譯可繞過

## 影響

整個 App 的安全性建立在「**內網信任**」前提。一旦有人接入內網（或側錄 APK）：
- ApiKey 直接可見
- 可冒充 KDS 對 POS 下指令
- 可監聽所有訂單資料
- 可替換更版 APK

## 修正方向

| 項目 | 建議 |
|------|------|
| ApiKey | 改成動態 token（如 KDS_ID + nonce + timestamp 簽章），或徹底改用 mTLS |
| APK 下載 | HTTPS + APK 簽章驗證 |
| HttpLogging | release build 改 NONE 或 BASIC level |
| 首次驗證 | 改成 server-side 一次性碼，或徹底移除（既然 KDS_ID 已綁裝置） |

## 但有可能不修

若 POS 與 KDS 永遠在同一台路由器、廚房內網不對外，這些都不是**業務優先**問題。屬於「技術債」清單。

## 狀態

- [ ] 待安全評估優先級

## 關聯

- 影響範圍：[[依賴注入]] / [[版本更新系統]] / [[首次安裝驗證]]
- 連帶 secrets：[[POS-API端點]]
