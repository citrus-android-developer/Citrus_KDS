---
type: system
status: done
created: 2026-05-13
updated: 2026-06-05
tags:
  - type/system
  - status/done
summary: |-
  FLOW:LoadStockList→getStockInfo→點品項→setSellStatus兩階段(local→remote)→remote失敗回滾local
  KEY:不輪詢只在進入時抓一次,本地樂觀更新+遠端失敗回滾,OnSetInventory死碼(內容已註解)
  DEP:[[POS-API端點]][[KDS訂單管理]][[Prefs偏好設定]]
---
# SetStockPage

庫存 / 售罄管理頁。讓 KDS 操作員把賣完的品項標為 Sold out，避免 POS 繼續接單。

## 資料來源

- `state.stockTypeList: List<String>?` — 分類下拉選單選項
- `state.stockInfoPresentList: List<StockInfo>?` — 篩選後的品項清單
- `state.stockInfoList: List<StockInfo>?` — 原始全量品項清單
- 由 `repository.getStockInfo()` 一次拉回（`POS_GET_STOCK_INFO = /KDS/InventoryList`）

## 進入時機

- KDS 模式下選 Tab index `3`（`currentPage="setStock"`）
- `SetStockPage.LaunchedEffect(Unit)` → `event(LoadStockList)` → `fetchStockInfo()` 拉一次（不輪詢）

## 畫面結構

`SetStockPage.kt`：
- 頂列：時鐘 + 分類下拉（ExposedDropdownMenu）+ 品項搜尋框
- 內容：`LazyVerticalGrid` 5 欄，每格 `StockItem`
- `StockItem` 顯示品項名 + 售罄狀態

## 操作：切換售罄狀態

按下品項 → `event(OnStockItemClicked(stockInfo))`（`SetStockPage.kt:225-226`）

```
Event.OnStockItemClicked
        │
        ▼
handleEvent (CentralViewModel.kt:409-428):
  if (gID/gKID blank) → errMsg "item_abnormal" + return    ← 邊界保護
  setSellStatus(SetItemSellStatusRequest{
    gID, gKID, status=反向("Available"↔"Sold Out"), storeNo=prefs.rsno, gname, size
  })
        │
        ▼
setSellStatus (CentralViewModel.kt:525-581) — 兩階段:
        │
        ├─ 階段 1: POST http://{localIp}/KDS/SetSellStatus  (local POS)
        │  └─ Success: 翻轉 stockInfoPresentList 對應項目 sellStatus
        │              呼叫 setSellStatusRemote (若 isUpdateServer=true)
        │
        └─ 階段 2: POST {BASE_URL}KDS/SetSellStatus  (遠端 Compass)
           ├─ Success: 啥都不做
           └─ Error: ★ 回滾 local ★
                    把 request status 翻回原值
                    再呼叫 setSellStatus(isUpdateServer=false)
                    顯示錯誤訊息
```

## 關鍵設計：兩階段 + 回滾

**先打本地 POS，成功後再打遠端 Compass。遠端失敗時回滾本地狀態**（`CentralViewModel.kt:584-621`）。

理由：保證本地 POS 與雲端的售罄狀態最終一致 — 避免員工以為設成功了，但雲端收到的訂單仍包含該品項。

備註：commit `9f85e67`（9/16）描述「setSellStatus 變更模式為 update Server 失敗，還原 Local、並顯示提示訊息」說明這是後來加上的容錯。

## 設計觀察

- **無中間 loading 狀態**：點下品項到階段 1 成功間 UI 已即時翻轉，看起來樂觀更新；遠端失敗才回滾
- **OnSetInventory 是死碼**：`handleEvent` 裡內容已註解（`CentralViewModel.kt:431-433`），庫存數量設定功能未啟用

## 關聯

- 上游：[[KDS訂單管理]]
- 對應 API：[[POS-API端點]]（`/KDS/SetSellStatus` 本地 + `KDS/SetSellStatus` 遠端）
- 不走 [[訂單狀態流轉]] / [[輪詢架構]]（這頁不輪詢、不改訂單狀態）



## 2026-06-05 庫存卡重設計 + 損耗/報廢 + 英文搜尋修正 + i18n

### 卡片重新設計（StockItem，全 inline 無 dialog）
- 三區：品項名 / 狀態列 / 損耗列。
- **狀態列**：左「● 上架中(綠) / ● 已售完(紅)」文字 + 右「設為售完(紅) / 設為上架(綠)」切換按鈕（按鈕顯示『要切到的目標』，色＝目標色，animateColorAsState 平滑）。狀態文字看現況、按鈕只管動作。
- **損耗列**：數量輸入 + 「損耗」按鈕並排，直接送出（無 dialog）；送出成功 Toast。只送損耗(S)，無報廢。

### 損耗/報廢功能（送本地）
- 事件 `OnSetWastage(stockInfo, qty, status)` → `setWastage` → `POST localIp + /KDS/SetWastage`。
- CreateUser = `prefs.kdsId`；後端寫 [Wastage] 表（Gname/sizedesc/GPrice 由 Goods 主檔填，GPrice=Price×Qty）。Status：W=報廢/S=損耗（UI 目前只送 S）。
- 詳見 [[POS-API端點]]、[[資料模型]]。

### 英文模式庫存搜不到（修正）
- 根因：分類用「該語言分類名」過濾；(1)分類下拉 stockTypeList 只在 stockInfoList 改變時重建，**切語言不重建** →仍是舊語言分類名；(2)選了舊語言分類 → 另一語言用 `gKEName==中文名` 過濾 → 空。
- 修正(OnLanguageChanged)：切語言時**重建 stockTypeList 為新語言** + 重設已選分類為全部 + 清搜尋；過濾健壯化：未選分類/空搜尋一律顯示全部（避免 gKEName 為 null 被 `null.contains('')` 藏掉）。

### i18n
品項名/加料/調味在畫面改用 itemDisplayLan（見 [[多語顯示]]）；庫存卡文字（上架中/已售完/設為.../數量/損耗）全用字串資源。
