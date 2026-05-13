---
type: project
status: doing
priority: P1
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/project
  - status/doing
  - priority/P1
summary: |-
  FLOW:HID掃描槍輸入→根層級onPreviewKeyEvent+時序啟發式→ScanOrderNo事件→servedList比對→sets tatus=F or 顯示錯誤
  KEY:複用既有/KDS/SetOrderStatus,全頁面通用,50ms間隔判斷掃描vs人類打字,新事件抽象為未來Intent掃描槍鋪路
  DEP:[[訂單狀態流轉]][[POS-API端點]][[KDS訂單管理]][[ISSUE-Collect動畫缺失]]
---
# 掃描槍 Collect 功能（評估中）

員工用條碼掃描槍掃訂單號 → App 自動觸發該筆訂單的 Collect 動作，免去找卡片點擊的步驟。

## 需求摘要

- **觸發來源**：條碼掃描槍掃訂單號條碼/QR
- **動作**：等同點擊 ServedPage 的 Collected 按鈕（status="F"）
- **適用範圍**：全頁面通用（KDS / Served / Recall / Setting 都能掃）
- **找不到單**：顯示提示「該訂單不在待取餐清單」

## 推薦實作方向

### 輸入接收：根層級 `onPreviewKeyEvent` + 時序啟發式

在 `MainActivity` setContent 的根 Box 加 `Modifier.onPreviewKeyEvent`：

- 累積 key 字元到 buffer
- 兩 key 間隔 < 50ms → 視為掃描槍輸入（HID 掃描槍字元間隔 5–20ms）
- 收到 `Key.Enter` 且 buffer 由「快速輸入」累積 → dispatch 新事件 `Event.ScanOrderNo(buffer)`
- 間隔 > 50ms → 視為人類打字，丟棄 buffer（不干擾 Setting 頁打字）

**好處**：未來支援 OEM Intent 掃描槍只要加 BroadcastReceiver dispatch 同一事件，業務邏輯不變。

### 業務邏輯：新事件 + ViewModel handler

`CentralContract.kt`：
```kotlin
data class ScanOrderNo(val orderNo: String) : Event()
```

`CentralViewModel.handleEvent`：
```kotlin
is Event.ScanOrderNo -> {
    val found = currentState.servedList?.any { it.orderNo == event.orderNo } == true
    if (found) {
        setOrderStatus(event.orderNo, status = COLLECTED)  // 沿用既有
    } else {
        setState { copy(errMsg = UiText.StringResource(R.string.order_not_in_served)) }
    }
}
```

## 變更檔案清單

| 檔案 | 改動 |
|------|------|
| `MainActivity.kt` | 根 Box 加 `onPreviewKeyEvent` + 時序判斷 + dispatch ScanOrderNo |
| `CentralContract.kt` | 新增 `Event.ScanOrderNo(orderNo)` |
| `CentralViewModel.kt` | 新增 ScanOrderNo handler |
| `strings.xml` | 新增 `R.string.order_not_in_served` |

## 不需要改

- API 層：完全複用 `/KDS/SetOrderStatus`（[[POS-API端點]]）
- UI 元件：servedList 卡片無需改動，狀態變化驅動

## 相關決策

- 不依賴 servedList 的 OK 覆蓋層動畫（[[ISSUE-Collect動畫缺失]] 仍存在，但掃描槍場景員工通常不看卡片）→ 改加音效/震動回饋
- 維持原本 ServedPage 點擊 Collect 行為不變（兼容雙重操作方式）

## 待決事項

- [ ] **掃描槍類型確認**：HID 鍵盤模式 vs OEM Intent（用戶選「還不確定」，先以 HID 為主）
- [ ] **訂單號長度上限**：buffer 超過 X 字直接 reset，避免奇怪輸入污染
- [ ] **音效/震動回饋**：成功 / 失敗時是否加？掃描操作的標配
- [ ] **「已 collected 過的單」掃到**：照目前邏輯會顯示「不在待取餐清單」，OK 嗎？

## 狀態

- [x] 需求收斂
- [x] 實作方向評估
- [ ] 待決事項確認後開工
- [ ] 實作 + 測試
- [ ] 驗證紀錄（→ Verification/）

## 關聯

- 核心 API：[[訂單狀態流轉]] / [[POS-API端點]]
- 影響的 UI：[[KDS訂單管理]]
- 連帶影響的 Issue：[[ISSUE-Collect動畫缺失]] / [[ISSUE-Collect死碼]]
