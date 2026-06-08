---
type: system
status: done
created: 2026-06-08
updated: 2026-06-08
related: "[[SetStockPage]], [[POS-API端點]]"
tags:
  - type/system
  - status/done
summary: |-
  FLOW:呼叫端組精簡body→POST KDS/SetSellStatus(帶ApiKey header)→先看HTTP status再看ApiStatus
  KEY:body只4欄StoreNo/GKID/GID/Status,Gname/Size後端自Goods補,Status只收Available/NotAvailable,本地SoldOut須映射NotAvailable,HTTP200也可能ApiStatus0,IIS需Content-Length
  DEP:[[SetStockPage]][[POS-API端點]]
  DECISION:[2026-06-08]remote改精簡contract+Status映射(valid)
  TEST:SM-X710實機端到端通過(2026-06-08) | VERIFY:[[2026-06-08_soldout_remote精簡contract]]
verified_by:
  - "[[Verification/2026-06-08_soldout_remote精簡contract]]"
decisions:
  - content: "Android remote soldout 改送精簡 contract（只 StoreNo/GKID/GID/Status）並把本地 Status 'Sold Out' 映射為 'Not Available'；local 與雲端 server 皆不改"
    context: "雲端 CompassKDS（lab2 部署）已改精簡 contract，Gname/Size 由後端自 Goods 主檔補，且 Status 值域只收 Available/Not Available。Android 原 remote 沿用 local 的 full body（含 Gname/Size）且 soldout 送 'Sold Out'。需求僅要對齊 remote，不動 local 與 server。"
    alternatives_considered:
      - "改 local+remote 共用的 request 一起精簡：會破壞 local POS（local 仍需 Gname/Size、且接受 Sold Out），排除"
      - "Android 不改、改雲端 server 接受舊欄位/舊 Status 值：超出需求且雲端已部署新 contract，不該回頭遷就"
      - "只在 remote 路徑新增精簡 VO + Status 映射（選用）：local 零影響、改動面最小"
    why_chosen: "需求明確只改 remote；新增獨立 remote VO 讓 local 完全不受影響，Status 映射解決雲端值域限制，改動最小、回滾風險最低"
    trade_offs: "local 與 remote 變成兩份 request 結構（多一 VO + 一次映射）；Status 語意兩端不一致（local=Sold Out / remote=Not Available），靠映射維持"
    decided: 2026-06-08
    valid: true
---
# CompassKDS Remote `SetSellStatus`（售況/Soldout）串接指南

> 給其他專案（POS / KDS / 後台）串接「雲端售況更新」用的可攜文件。
> 內容皆以 SM-X710 實機封包 + 線上 swagger 驗證（2026-06-08）。

---

## 1. 這支 API 在做什麼

把「某品項在某門市的售況」寫到雲端 CompassKDS。
售況只有兩種值：**可售 / 不可售**。商品名稱、尺寸等欄位**由後端自 Goods 主檔補齊**，呼叫端不需傳。

---

## 2. 端點

| 項目 | 值 |
|---|---|
| Method | `POST` |
| Path | `KDS/SetSellStatus` |
| Base URL（lab2 測試環境） | `https://lab2.citrus.tw/CompassKDS/` |
| 完整 URL | `https://lab2.citrus.tw/CompassKDS/KDS/SetSellStatus` |
| Content-Type | `application/json` |

> ⚠️ 後端在 IIS 上，**POST 一定要帶 `Content-Length`**（連空 body 也要）。
> 用 curl 時別送無 body 的 POST，否則回 `411 Length Required`。

---

## 3. 認證

| Header | 必填 | 說明 |
|---|---|---|
| `ApiKey` | ✅ | 環境專屬金鑰，請向後端索取 |

- 金鑰錯誤 → `HTTP 401` `{"Message":"Invalid API Key"}`
- lab2 測試金鑰（僅測試用，勿用於正式）：
  `5EC8433D25C759DD6BB965090F6835C77BB569CE86F3713B2D364E642F693280`

---

## 4. Request Body

只有 4 個欄位：

```json
{
  "StoreNo": "S00000",
  "GKID": "110",
  "GID": "1003",
  "Status": "Not Available"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|---|---|---|---|
| `StoreNo` | string | ✅ | 門市代號 |
| `GKID` | string | ✅ | 商品大類編號 |
| `GID` | string | ✅ | 商品編號 |
| `Status` | string | ✅ | 售況，**只允許 `Available` / `Not Available`** |

### Status 值對應（重要）

> 線上 lab2 部署實測：`Status` 僅接受 `Available` / `Not Available`。
> 送其他值（例如 `Sold Out`）會回 `ApiStatus:"0"` + `Status 僅允許 Available 或 Not Available`。
> （swagger 文字敘述雖寫 `Sold Out / Available / Not Available / LOCK`，但部署版驗證只認兩個值，以實測為準。）

若你的系統內部用「Sold Out / 售完」表示不可售，**送雲端前要轉成 `Not Available`**：

| 系統內部售況 | 送雲端 `Status` |
|---|---|
| 可售 / Available | `Available` |
| 售完 / Sold Out / 不可售 | `Not Available` |

- **不要**傳 `Gname`、`Size`：後端會自 Goods 主檔補齊。傳了通常被忽略，但舊版部署（見 §7）會因 `[Required]` 而擋下。

---

## 5. Response

**注意：邏輯成功與否看 `ApiStatus`，HTTP 可能是 200 但 `ApiStatus:"0"`。**

成功：
```json
{ "ApiStatus": "1", "Error": { "Code": null, "Message": null } }
```

邏輯失敗（HTTP 仍 200）：
```json
{ "ApiStatus": "0", "Error": { "Code": "400", "Message": "Status 僅允許 Available 或 Not Available" } }
```

| 欄位 | 說明 |
|---|---|
| `ApiStatus` | `"1"` 成功 / `"0"` 失敗 |
| `Error.Code` | 失敗代碼（如 `400`、`999`） |
| `Error.Message` | 失敗訊息 |

### 呼叫端判斷準則
1. 先看 HTTP status（`401` 金鑰錯、`400` 格式/欄位錯、`411` 缺 Content-Length）。
2. HTTP 200 再看 body 的 `ApiStatus`，**`"1"` 才算成功**。

---

## 6. curl 範例（可直接驗證）

```bash
KEY="<你的 ApiKey>"
URL="https://lab2.citrus.tw/CompassKDS/KDS/SetSellStatus"

# 設為不可售
curl -s -X POST "$URL" \
  -H "ApiKey: $KEY" -H "Content-Type: application/json" \
  -d '{"StoreNo":"S00000","GKID":"110","GID":"1003","Status":"Not Available"}'
# => {"ApiStatus":"1","Error":{"Code":null,"Message":null}}

# 還原為可售
curl -s -X POST "$URL" \
  -H "ApiKey: $KEY" -H "Content-Type: application/json" \
  -d '{"StoreNo":"S00000","GKID":"110","GID":"1003","Status":"Available"}'
```

---

## 7. 部署版本差異（踩雷紀錄）

實測同一支 API 在不同部署的 contract 不同，串接前務必確認你連的是哪一台：

| 部署 | Gname/Size | Status 值域 | 狀態 |
|---|---|---|---|
| `lab2.citrus.tw/CompassKDS`（新版） | 不需要 | `Available` / `Not Available` | ✅ 本文件基準 |
| `global.citrus.tw/CompassKDS_UAT`（舊版） | **`[Required]`** | — | ⚠️ 送精簡 body 會 `400 Gname/Size required` |

> 若你的 client 連到舊版 UAT，精簡 body 會被擋。請確認目標 server 已部署新 contract，或暫時補傳 Gname/Size。

---

## 8. Android / Retrofit 串接範例

本專案（CitrusKDS）的實作可直接複製到其他 Android 專案。

**Request VO**（Moshi）：
```kotlin
data class SetItemSellStatusRemoteRequest(
    @Json(name = "StoreNo") val storeNo: String,
    @Json(name = "GKID")    val gKID: String,
    @Json(name = "GID")     val gID: String,
    @Json(name = "Status")  val status: String   // 只放 "Available" / "Not Available"
)
```

**Retrofit service**：
```kotlin
@POST
suspend fun setSellStatusRemote(
    @Url url: String,                 // serverBaseUrl + "KDS/SetSellStatus"
    @Header("ApiKey") apiKey: String, // 若沒有共用 interceptor，記得帶
    @Body body: SetItemSellStatusRemoteRequest
): ApiResult<Int>
```

**呼叫端做 Status 映射**：
```kotlin
val remote = SetItemSellStatusRemoteRequest(
    storeNo = storeNo,
    gKID = gkid,
    gID = gid,
    // 內部 "Sold Out" → 雲端 "Not Available"
    status = if (localStatus == "Available") "Available" else "Not Available"
)
```

> CitrusKDS 內參考實作：
> - VO：`commonData/vo/SetItemSellStatusRemoteRequest.kt`
> - Service：`commonData/ApiService.kt#setSellStatusRemote`
> - 映射：`ui/domain/ApiRepositoryImpl.kt#setSellStatusRemote`

---

## 9. 串接檢核清單

- [ ] 目標 Base URL 確認是新版部署（§7）
- [ ] `ApiKey` header 已帶且正確
- [ ] body 只送 `StoreNo/GKID/GID/Status` 四欄
- [ ] `Status` 已映射為 `Available` / `Not Available`
- [ ] POST 帶 `Content-Length`
- [ ] 成功判斷同時檢查 HTTP status 與 `ApiStatus == "1"`
