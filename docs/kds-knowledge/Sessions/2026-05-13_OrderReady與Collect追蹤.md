---
created: 2026-05-13
tags:
  - type/session
---
# 2026-05-13 Session 交接

## 本次完成

1. **梳理 OrderReady 模式 API**：確認切到 OrderReady 後僅輪詢 `POST /controller/OrdersList`（3 秒一次）。模式切換本身不打 API。
2. **追蹤 Collect 按鈕完整流程**：
   - ServedPage → `CollectedOrder(orderNo, status="F")`
   - → `setOrderStatus()` → `POST /KDS/SetOrderStatus`
   - 成功後本地處理有 UX 不一致 / 死碼（建立兩個 Issue）
3. **OrderReadyInfo 反推**：POS 端 `/controller/OrdersList` 預期回傳 `[{OrderName, OrderNo[]}]`
4. **新功能評估**：掃描槍→Collect。確認推薦用根層級 `onPreviewKeyEvent` + 時序啟發式
5. **建立知識圖譜**：本 vault 初始化，含 1 Project + 7 Systems + 2 Issues + 1 評估中 Project + 1 MOC

## 待用戶確認

[[掃描槍Collect功能]] 中的待決事項：
- 掃描槍類型（HID / Intent）— 用戶答「還不確定」
- 訂單號長度上限
- 音效/震動回饋
- 已 collected 過的單再掃到的處理

## 下一步建議

1. 等用戶回答待決事項 → 開始實作 [[掃描槍Collect功能]]
2. 實作時可順手討論是否合併修 [[ISSUE-Collect動畫缺失]] / [[ISSUE-Collect死碼]]
3. 完成後寫 Verification

## 圖譜入口

- [[MOC/系統地圖]]
- [[Citrus_KDS]]
