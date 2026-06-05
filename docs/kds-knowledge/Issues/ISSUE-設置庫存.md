---
type: issue
status: doing
priority: P1
created: 2026-06-05
updated: 2026-06-05
related: "[[SetStockPage]], [[POS-API端點]], [[Prefs偏好設定]], [[設定頁]]"
summary: |-
  FLAG:ROOTCAUSE,DECISION | 設置庫存點擊全錯(400/伺服器忙碌中)
  KEY:三層問題-(1)前端送空Gname被[Required]擋400(2)後端寫不存在的ProductBoutique/Store→伺服器忙碌中,改寫SoldOutItem(3)serverUrl誤設本地應指雲端後台UAT
  KEY:本地POS寫SoldOutItem(KDS顯示)、雲端後台寫ProductBoutique(由serverUrl remote同步)
---
# ISSUE: 設置庫存點擊報錯（400 / 伺服器忙碌中）

## 三層問題與修法

### ① 400：前端送空 Gname
- 346/960 品項無英文名(GName2 空)，前端 `gname = eName ?: cName` 不接空字串 → 送空 Gname → 後端 `[Required]`(預設不接受空字串)擋 → 400。
- 修(前端 CentralViewModel)：`eName?.takeIf{notBlank} ?: cName`；並 `storeNo.trim()`（RSNO 有前導空格 ' S00000'）。

### ② 伺服器忙碌中：後端寫不存在的表
- 後端 SetSellStatus 寫 `ProductBoutique`/`Store` —— 本地 POS DB(order/product)**無此表** → 例外 → 200 但 ApiStatus=0「伺服器忙碌中」。
- 讀取端 GetInventoryList 用的是 `SoldOutItem`(本地有)。
- 修(後端，本地 POS 版)：SetSellStatus 改寫 `SoldOutItem`（DAL 原本被註解的版本）：Available→DELETE、其餘→upsert SOType。

### ③ serverUrl 設定錯誤（後台同步打回本地）
- 後台同步靠前端 setSellStatusRemote → `serverUrl + KDS/SetSellStatus`。
- 但裝置 `serverUrl` = 預設 `http://192.168.0.162:8099/`(本地 POS)，**非雲端後台**。
- 雲端後台實為 `https://global.citrus.tw/CompassKDS_UAT/`(已驗證為 Compass_KDS、SetSellStatus 回 ApiStatus:1、有 ProductBoutique)。
- `ProductBoutique` 是後台的表，須由雲端後台寫 → serverUrl 應設成 UAT。
- 修：設定頁 Server URL 改 `https://global.citrus.tw/CompassKDS_UAT/`（正式環境用 /CompassKDS/）。

## 角色分工（結論）
- 本地 POS(localIp)：SetSellStatus 寫 `SoldOutItem` → KDS 即時顯示售完。
- 雲端後台(serverUrl)：SetSellStatus 寫 `ProductBoutique` → 後台庫存（前端 remote 同步）。
- ⚠️ 同一份 code 兩處部署，本地版用 SoldOutItem、雲端版維持 ProductBoutique，勿互相覆蓋。

## 狀態
- [x] 前端 400 修正（已裝）
- [x] 後端 SoldOutItem 版（已 push 77e60fc，**待部署本地 POS**）
- [ ] serverUrl 改 UAT（設定頁手動，D3 已改測試；正式需確認 UAT/正式）
- [ ] 本地 POS 部署後端後端到端驗證
