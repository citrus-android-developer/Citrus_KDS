---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:onCreate→setContent→NavHost(kds/orderReady/setting)→根據prefs.mode決定startDestination｜onResume→Kiosk全螢幕
  KEY:單一Activity+全Compose,IMMERSIVE_STICKY隱藏狀態列導航列,單一CentralViewModel共用,觀察printOrder觸發列印,intentToUpdate權限授予後不會自動繼續
  FLAG:KIOSK_MODE,SINGLE_ACTIVITY
  DEP:[[KdsScreen容器]][[OrderReady模式]][[設定頁]][[列印系統]][[版本更新系統]][[依賴注入]][[系統模式切換]]
---
# MainActivity 入口

App 唯一的 Activity，宿主所有 Compose 內容。`MainActivity.kt`。

## 生命週期 Kiosk 化

`setSystemUiVisibilityMode()`（line 109-119）設一組 flag：
- `SYSTEM_UI_FLAG_HIDE_NAVIGATION` + `SYSTEM_UI_FLAG_FULLSCREEN` + `IMMERSIVE_STICKY`
- `onResume` 每次重設

實質讓 App **隱藏狀態列與導航列**，做成全螢幕廚房終端 — 員工無法輕易退出。

## Navigation 結構

`NavHost`（line 152-193）三條路由：

```
startDestination = if (prefs.mode == 0) "kds" else "orderReady"

├─ "kds"        → KdsScreen
├─ "orderReady" → OrderReadyScreen
└─ "setting"    → SettingPage（單獨進入版本）
```

> Setting 在 [[KdsScreen容器]] 也是 Tab 4，這個獨立路由是給 OrderReady 模式進入用的。

## Hilt 注入

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var printUtil: PrintUtil
}
```

`PrintUtil` 注入後在 `LaunchedEffect(homeViewModel.currentState.printOrder)` 觀察狀態：

```kotlin
LaunchedEffect(homeViewModel.currentState.printOrder) {
    homeViewModel.currentState.printOrder?.let {
        printUtil.setOrderPrint(it)
    }
}
```

ViewModel 派發 ReprintOrder → 設 `printOrder` state → 這個 effect 觸發實際列印（[[列印系統]]）。

## 三個一次性 Dialog

`MainActivity.kt:195-215`：

| Dialog | 觸發條件 |
|--------|----------|
| `DownloadApkProgressDialog` | `downloadStatus` 非 null（更版中） |
| `UpdateDialog` | `updateAsk.value == true`（要求輸入版本號） |
| `isVerifyCancel`（→ Activity.finish） | 首次安裝驗證取消 |

## intentToUpdate 流程

點 KDS 底部版本欄或 OrderReady 設定 → `intentToUpdate(updateAsk)`：

1. 檢查 `WRITE_EXTERNAL_STORAGE` 權限
2. 沒權限 → `requestPermissions(..., 888)`，使用者授權後沒有後續處理（**疑似 bug**：權限授予後不會自動繼續，需再點一次）
3. 有權限 → `updateAsk.value = true` 開啟 [[版本更新系統]] 對話框

## CentralViewModel 全域 instance

```kotlin
val homeViewModel = hiltViewModel<CentralViewModel>()
```

整個 Compose 樹共用同一個 `CentralViewModel`，所以 KDS/Served/Recall/Setting 等不同頁面看到的是同一份 State。這是 [[MVVM架構]] 的關鍵假設。

## 關聯

- 啟動子畫面：[[KdsScreen容器]] / [[OrderReady模式]] / [[設定頁]]
- 注入依賴：[[依賴注入]] / [[列印系統]]
- 上層 App：[[依賴注入]]（MyApplication）
- 模式分流：[[系統模式切換]]
- 版本更新：[[版本更新系統]]
