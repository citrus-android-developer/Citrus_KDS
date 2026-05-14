---
type: project
status: doing
created: 2026-05-13
updated: 2026-05-13
tags:
  - status/doing
  - type/project
summary: |-
  FLOW:App啟動→prefs.mode判斷→KDS或OrderReady
  KEY:雙模式,單一CentralViewModel,prefs.localIp指向POS本地服務
  DEP:[[KDS訂單管理]][[OrderReady模式]][[訂單狀態流轉]][[輪詢架構]][[POS-API端點]][[MVVM架構]]
  TEST:無自動化測試
---
# Citrus_KDS

Android 廚房顯示系統（Kitchen Display System），給餐廳廚房或櫃台使用，用來顯示訂單、管理製作狀態、以及顯示取餐叫號牆。

## 雙模式設計

App 啟動時依 `prefs.mode` 決定預設畫面（[[系統模式切換]]）：
- `mode=0` → [[KDS訂單管理]]（廚房製作畫面，三 tab：Main / Served / Recall）
- `mode=1` → [[OrderReady模式]]（取餐叫號顯示牆）

## 核心系統

| 系統 | 用途 |
|------|------|
| [[KDS訂單管理]] | 廚房主畫面，訂單卡片 + 製作狀態切換 |
| [[OrderReady模式]] | 客戶端取餐顯示牆 |
| [[訂單狀態流轉]] | J/W/O/F 狀態機 + `/KDS/SetOrderStatus` API |
| [[輪詢架構]] | fetchOrdersJob，3 秒輪詢，動作後 1 秒補位 |
| [[POS-API端點]] | 全部後端 API 端點清單 |
| [[MVVM架構]] | BaseViewModel + CentralContract 單一狀態容器 |
| [[設定頁]] | IP / KDS_ID / 印表機 / 模式 / 語系設定 |

## 技術棧

- **語言**：Kotlin
- **UI**：Jetpack Compose
- **架構**：MVVM + Contract（Event/State/Effect）
- **DI**：Hilt（`@Singleton ApiRepositoryImpl`）
- **HTTP**：Retrofit + Moshi
- **Logging**：Timber
- **動畫**：Lottie

## 關鍵專案路徑

| 區塊 | 路徑 |
|------|------|
| Entry | `MainActivity.kt` |
| ViewModel | `ui/presentation/CentralViewModel.kt`（>900 行，唯一 ViewModel） |
| Contract | `ui/presentation/CentralContract.kt`（Event/State） |
| Repository | `ui/domain/ApiRepository*.kt` |
| ApiService | `commonData/ApiService.kt`（Retrofit interface） |
| 常數 | `util/Constants.kt`（含 endpoint paths、status codes） |

## 後端依賴

- **POS 本地服務**（`prefs.localIp`）：訂單列表、狀態變更、庫存
- **遠端 Compass KDS**（`Constants.BASE_URL = global.citrus.tw/CompassKDS_UAT/`）：售罄狀態同步、推播通知

## 進行中

- [ ] [[掃描槍Collect功能]] — 評估中

## 已知問題

- [[ISSUE-Collect動畫缺失]] — Served 頁 Collect 沒有 OK 動畫覆蓋層
- [[ISSUE-Collect死碼]] — Collect 成功處理路徑中對 mainList/recallList 的更新邏輯實質為死碼
