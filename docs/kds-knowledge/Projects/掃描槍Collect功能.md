---
type: project
status: done
priority: P1
created: 2026-05-13
updated: 2026-06-04
tags:
  - type/project
  - status/done
  - priority/P1
summary: |-
  FLOW:SUNMI廣播(ACTION_DATA_CODE_RECEIVED,extra data=完整訂單號)→震動+ScanOrderNo→查main清單→該單為PREPARED(O)才setStatus=F,否則跳不在待取清單
  KEY:複用既有/KDS/SetOrderStatus,全頁面通用,SUNMI廣播輸入(非HID),2026-06-04修正:驗證清單從served(F)改main(O)
  FIX:2026-06-04掃待取單收不到→根因handler查served但後端served=已取(F)→改查main(O);另跳回主頁=掃描器同時開鍵盤輸出(裝置設定關掉)
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



## 2026-06-04 實作現況 + 重大修正

### 實際實作（與上方設計差異）
- **輸入接收改用 SUNMI 廣播**（非 onPreviewKeyEvent HID）：`SunmiScanReceiver` 收 `com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED`、extra `data`=訂單號 → `MainActivity` onStart/onStop 註冊 → 震動 + dispatch `ScanOrderNo`。全頁面通用。
- QR 內容 = **完整訂單號**（如 `P012026060400005`），與後端 OrderNo 完全比對。

### Bug：掃了沒收到（狀態不變）— 已修
- **症狀**：掃待取的單，狀態沒變（還可能跳回主頁，見下）。
- **根因**：handler 原本查 `getOrders("served")` 比對。但後端 `served` 自 4/9（commit 673ec39「Served呈現Ｆ」）起回的是**已取(F)**，不是待取(O)。→ 掃**待取(O)的單**永遠不在 F 清單 → 找不到 → 不 Collect。
- **修法**：改查 `getOrders("main")`（含 J/W/O），且僅在該單 `status == PREPARED(O)`（ignoreCase）時才 `setOrderStatus(COLLECTED)`；否則跳「不在待取餐清單」。
- **驗證（adb 模擬 SUNMI 廣播）**：O 單掃→收到廣播→查 main→比對 O→SetOrderStatus 200→後端轉 F ✅；已取(F)單掃→查 main→無 SetOrderStatus、跳提示 ✅。

### 另一症狀「跳回主頁」= 掃描器設定問題（非程式）
logcat 顯示掃描當下除廣播外還有 `SunmiCustomKey interceptKeyBeforeQueueing` 鍵盤事件 → 掃描器同時開了「鍵盤輸出」。修法：SUNMI 掃碼工具 App → 應用設置 → 輸出方式設置 → **只開廣播、關鍵盤模擬輸出**。

### 跨群組注意
`SetOrderStatus` 是 `WHERE OrderNO=… AND PrintGroup=@KDS_ID` → 掃描只收掉「本 KDS 群組」的品項；同單在其他群組(如 DTF)的品項仍為 O、續留 OrderReady 牆。
