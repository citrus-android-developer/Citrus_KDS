---
type: system
status: done
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/system
  - status/done
summary: |-
  FLOW:Screen→setEvent→handleEvent→setState→Compose重組
  KEY:單一CentralViewModel(>900行)集中所有狀態,BaseViewModel封裝事件管道,State用mutable var+copy
  DEP:[[POS-API端點]][[KDS訂單管理]][[OrderReady模式]][[設定頁]]
---
# MVVM 架構

整個 App 用單一 ViewModel 集中管理狀態：`CentralViewModel`（>900 行）。Compose 畫面只透過 `Event` / `State` 與之溝通。

## Contract Pattern

`CentralContract.kt`：
- `sealed class Event : UiEvent` — UI 觸發的事件（按鈕、輸入、模式切換等）
- `data class State : UiState` — 整個 App 的單一狀態容器（含三個訂單 list、orderReadyList、印表機資訊、錯誤訊息等）
- `sealed class Effect : UiEffect` — 一次性副作用（如 `DownloadApkSuccess`）

State 採可變欄位（`var`），透過 `setState { copy(...) }` 更新。

## BaseViewModel

`util/BaseViewModel.kt`：通用 ViewModel，提供：
- `currentState` / `setState`
- `setEvent(event)` + `event.collect { handleEvent(it) }`（line 66）
- `setEffect`（一次性 effect）

`CentralViewModel` 繼承並實作：
- `createInitialState()` — 用 prefs 還原 InputStateWrapper
- `handleEvent(event)` — 大 when 分支處理所有 Event（line 243~471）

## DI（Hilt）

`@Singleton class ApiRepositoryImpl @Inject constructor(...)`
ViewModel 用 `@HiltViewModel` 注入 `ApiRepository` + `PrinterDetecter`。

## 偏好設定（Prefs）

`util/Prefs.kt`：SharedPreferences 包裝，全域單例 `prefs`（從 `MyApplication` 取）。
存：localIp / kdsId / rsno / mode / printerName / language 等使用者設定。

## Compose 整合

- 各 Screen 接收 `viewModel: CentralViewModel`
- 透過 `viewModel.currentState` 讀取狀態（Compose 觀察 mutableState）
- 透過 `viewModel.setEvent(Event.XXX)` 派發事件
- `LaunchedEffect(Unit)` 處理啟動時的一次性動作（如啟動輪詢）
- `LaunchedEffect(state.XXX)` 處理狀態變化反應（如 `modeState` 變化時 navigate）

## 副作用 / Job 管理

- `viewModelScope.launch { ... }` 啟動的 Job 存於變數（如 `orderInfoJob`, `orderReadyJob`）
- 切換模式時 `.cancel()` 對應 Job

## 已知設計疑慮

- **單一巨型 ViewModel**：>900 行，所有事件、所有 list、所有 API 邏輯都集中
- **State mutable 欄位**：用 `var` + `setState { copy() }` 雖能工作，但失去 data class 不可變的優勢，容易誤改

## 關聯

- 事件源頭：[[KDS訂單管理]] / [[OrderReady模式]] / [[設定頁]]
- 副作用：API → [[POS-API端點]]
