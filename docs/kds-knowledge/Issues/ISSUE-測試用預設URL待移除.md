---
type: issue
status: todo
priority: P1
created: 2026-06-03
tags:
  - type/issue
  - status/todo
  - priority/P1
---
# ISSUE: 測試用預設 URL 待移除

## 現象

為測試階段方便，把 **POS IP** 與 **Server URL** 的「空白回退預設值」寫死成本地測試後端 `192.168.0.162:8099`。**此為測試用，不是永久預設**，上線前必須處理。

## 背景（2026-06-03）

- 在 D3_PRO 上測試 Compass 後端（本地 **IIS / port 8099**）。同一台後端同時提供：
  - 本地 POS 端點（`/KDS/OrdersList`、`/KDS/SetOrderStatus`、`/KDS/InventoryList`…）
  - 遠端 Server 端點（`/KDS/SetSellStatus`、`/KDS/OrdersNotify`）
- 為讓機器免手動填欄位即可連線，於 `Constants` 新增 `DEFAULT_POS_IP` / `DEFAULT_SERVER_URL`，[[Systems/Prefs偏好設定]] 在欄位空白時回退到它們。
- 注意：已設過舊值（非空白）的機器，回退不會生效，需手動覆蓋（D3_PRO 的 POS IP 即為此情況）。

## 待辦

- [ ] 上正式環境前，移除/改掉 `Constants.DEFAULT_POS_IP` 與 `Constants.DEFAULT_SERVER_URL` 的 `192.168.0.162:8099`
- [ ] 決定正式預設策略：(a) 回退改空白強制手動填 / (b) 改成正式環境 URL / (c) 各店不同則維持空白
- [ ] 確認 `Constants.BASE_URL`（AppModule Retrofit 佔位用，目前 UAT）是否一併處理

## 相關

- [[Systems/Prefs偏好設定]]
- [[Systems/POS-API端點]]
- [[Systems/設定頁]]
