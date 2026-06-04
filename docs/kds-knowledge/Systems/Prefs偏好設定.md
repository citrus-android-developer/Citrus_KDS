---
type: system
status: done
created: 2026-05-13
updated: 2026-06-03
tags:
  - type/system
  - status/done
summary: |-
  KEY:SharedPreferences包裝,全域單例prefs,~30欄位｜連線(localIp/serverUrl/kdsId/rsno)/模式(mode/printMode/prepareMode/autoAccept)/印表機/計算｜localIp+serverUrl空白回退DEFAULT常數
  FLAG:Key命名不一致(部分KEY_*常數,部分硬編字串),無migration機制,測試用預設URL待移除
  DEP:[[設定頁]][[系統模式切換]][[POS-API端點]][[訂單狀態流轉]][[自動接單功能]][[ISSUE-測試用預設URL待移除]]
---
# Prefs 偏好設定

`util/Prefs.kt` — SharedPreferences 包裝類，提供型別安全的 getter/setter 存取。透過 [[MVVM架構]] 的全域單例 `prefs` 在整個 App 內共用。

## 取用方式

```kotlin
import com.citrus.citruskds.di.prefs   // 全域單例

prefs.localIp = "192.168.1.100"
val mode = prefs.mode
```

實例由 `MyApplication` 在 onCreate 時建立，掛到 companion 物件。

## 完整欄位

### 連線 / 識別

| 欄位 | 型別 | 預設 | 用途 |
|------|------|------|------|
| `localIp` | String | "" | POS 本地服務 IP（拼接所有本地 API） |
| `serverIp` | String | "" | （未使用 / 保留） |
| `kdsId` | String | "" | KDS 裝置識別碼（隨 SetOrderStatus 等請求送出） |
| `rsno` | String | "" | 店家代號（OrdersNotify 帶） |
| `storeName` / `storeNo` / `storeAddress` | String | "" | 店家資訊（列印用） |

### 模式 / UI

| 欄位 | 型別 | 預設 | 用途 |
|------|------|------|------|
| `mode` | Int | 0 | 0=KDS / 1=OrderReady（[[系統模式切換]]） |
| `printMode` | Int | 0 | 列印模式（Setting `PrintRadio` 控制） |
| `isPrepareEnable` | Boolean | false | 準備模式開關（`PrepareRadio` 控制） |
| `language` | String | "English" | UI 語系 |
| `itemDisplayLan` | String | "English" | 訂單品項顯示語系 |
| `defaultPage` | String | "Main" | App 啟動時 KDS 模式下的預設 Tab |
| `bgColor` | String | "" | 背景色 |

### 印表機

| 欄位 | 型別 | 用途 |
|------|------|------|
| `printer` | String | （舊欄位） |
| `printerName` | String | `PrinterDetecter` 找到的印表機名稱 |
| `printerTarget` | String | 連線目標（USB/Network address） |
| `printerIs80mm` | Boolean | 紙張寬度（80mm vs 58mm） |
| `portName` | String | （Serial port 用） |
| `charSet` | String "UTF-8" | 列印編碼 |
| `isLargeLineSpacing` | Boolean | 大行距開關 |
| `header` / `footer` | String | 收據頁首/頁尾文字 |

### 計算 / 顯示

| 欄位 | 型別 | 預設 | 用途 |
|------|------|------|------|
| `decimalPlace` | Int | 0 | 小數位數 |
| `taxFunction` | Int | 0 | 稅率功能模式 |
| `tax` | Int | 0 | 稅率值 |
| `methodOfOperation` | Int | 0 | 進位方式：0=四捨五入 / 1=無條件進位 / 2=無條件捨去 / 3=特殊規則（見 `Constants.getValByMathWay`） |
| `idleTime` | Int | 120 | 閒置秒數 |

### App 狀態

| 欄位 | 型別 | 預設 | 用途 |
|------|------|------|------|
| `isNavigate` | Boolean | false | 導航旗標（用法待查） |
| `firstInstall` | Boolean | true | 首次安裝標記 |
| `orderStr` | String | "" | 訂單字串快取 |

## 設計觀察

- 純單純 SharedPreferences wrapper，沒有 reactive flow / coroutine 整合
- 大量設定欄位透過 `Constants.KEY_*` 常數定位，少數直接用字串 key（如 `"mode"`、`"localIp"`）— **不一致**
- 沒有資料 migration 機制（新版加欄位靠預設值，OK；改型別會炸）

## 關聯

- 寫入處：[[設定頁]]（大部分欄位）、`MainActivity`（少數）
- 讀取處：[[POS-API端點]]（拼接 URL）、[[訂單狀態流轉]]（kdsId）、[[系統模式切換]]（mode）


## 2026-06-03 更新（新增欄位）

| 欄位 | 型別 | 預設 | 用途 |
|------|------|------|------|
| `serverUrl` | String | `DEFAULT_SERVER_URL` | 遠端 Compass base url（設定頁可改）；getter 空白時回退到 `Constants.DEFAULT_SERVER_URL` |
| `serverBaseUrl` | String(唯讀) | — | `serverUrl` 正規化：自動補結尾 `/`，供 [[POS-API端點]] 拼接 |
| `isAutoAcceptEnable` | Boolean | false | 自動接單開關（用 `KEY_AUTO_ACCEPT`），見 [[自動接單功能]] |

變更：
- `localIp` getter 改為**空白時回退** `Constants.DEFAULT_POS_IP`（原預設 ""）
- 測試階段 `DEFAULT_POS_IP` / `DEFAULT_SERVER_URL` = `192.168.0.162:8099`（待移除 → [[ISSUE-測試用預設URL待移除]]）
- ⚠️ 已存非空白舊值的機器，回退不生效，需手動覆蓋
