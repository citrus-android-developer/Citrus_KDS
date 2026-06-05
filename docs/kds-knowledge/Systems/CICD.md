---
type: system
status: done
created: 2026-06-05
updated: 2026-06-05
tags:
  - type/system
  - status/done
summary: |-
  FLOW:打tag v* →GitHub Actions→assembleRelease(repo內建keystore簽章,免secret)→改名citruskds_v<版本>.apk+產version.json→curl上傳hq.citrus.tw/fileUploader(subPath=kds)
  KEY:tag格式v<版本>,subPath=kds,下載https://hq.citrus.tw/apk/kds/citruskds_v<版本>.apk,僅需UPLOAD_API_KEY,無flavor無Tinker
  FLAG:KEYSTORE_IN_REPO(簽章密碼明文,CI免secret但有外洩風險)
  DEP:[[版本更新系統]]
  VERIFY:[[Verification/2026-06-05_CICD打tag發版v115]]
verified_by: "[[Verification/2026-06-05_CICD打tag發版v115]]"
---
# CI/CD 發布系統

打 tag → GitHub Actions 自動建置簽章 APK → 上傳到 hq.citrus.tw 指定資料夾。基於 `android-cicd-upgrade-hotfix` skill 的組織共用模式精簡而成（本專案無 flavor、無 Tinker 熱修）。

## 觸發與流程

`.github/workflows/release.yml`，觸發：`push tag v*`（如 `v1.1.5`）。

```
打 tag v<版本>
  │ 正規驗證 ^v[0-9]+\.[0-9]+(\.[0-9]+)?$（不符直接失敗）
  │ 解析 VERSION=去v；versionCode 從 build.gradle.kts grep；tag≠versionName 只警告
  ▼
JDK 17 → ./gradlew assembleRelease   ← repo 內建 keystore(app/kds_key.jks)直接簽章，免 secret
  ▼
改名 citruskds_v<版本>.apk + 算 MD5/size → 產 version.json
  ▼
curl POST hq.citrus.tw/fileUploader/api/upload  (X-API-KEY=UPLOAD_API_KEY, subPath=kds, file=@...)
  ├─ 上傳 APK
  └─ 上傳 version.json
  ▼
actions/upload-artifact 備份一份
```

## 關鍵約定

| 項目 | 值 |
|------|-----|
| Tag 格式 | `v<版本>`（純版本號，單一環境，無 prod/dev） |
| 上傳資料夾 subPath | `kds` |
| APK 檔名 | `citruskds_v<版本>.apk` |
| 下載網址 | `https://hq.citrus.tw/apk/kds/citruskds_v<版本>.apk` |
| version.json | `https://hq.citrus.tw/apk/kds/version.json`（latestVersion/Code、md5、fileSize、downloadUrl、changelog） |
| 簽章 | keystore 與密碼**明文在 repo**（見下方風險）；CI 免 keystore secret |
| 必要 GitHub Secret | `UPLOAD_API_KEY`（hq.citrus.tw 上傳金鑰，與 compass/taroko 共用） |
| 上傳 API | `https://hq.citrus.tw/fileUploader/api/upload`（multipart, X-API-KEY, subPath, file）；伺服器存 `E:\\iis\\apk\\kds\\` |

## 發版步驟

1. 改 `app/build.gradle.kts` 的 versionCode/versionName
2. commit
3. `git tag v1.1.x && git push origin v1.1.x`
4. 看 `gh run watch` 或 Actions 分頁

## 與 App 內版更的關係

App 內手動版更（[[版本更新系統]]）下載網址已對齊本 CI 產物（`kds/citruskds_v<版本>.apk`）。version.json 目前 **App 尚未消費**（自動檢查更新待做）。

## 風險／待辦

- ⚠️ keystore(`kds_key.jks`)+ 簽章密碼明文在 repo → CI 方便但任何能讀 repo 者可取得正式金鑰。private repo 內部工具暫可接受；若開放/外包需改用 secret 注入。見 [[ISSUE-測試用預設URL待移除]] 同類安全議題。
- Node.js 20 actions 警告（checkout/setup-java/upload-artifact@v4）：2026-06-16 起 GitHub 強制 Node 24，屆時升 action 版本即可（不升也會自動切）。

## 相關
- [[版本更新系統]]（App 端下載/安裝）
- `.github/workflows/release.yml`
