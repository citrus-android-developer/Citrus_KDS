---
type: verification
status: pass
feature: Android remote soldout 改精簡 contract + Status 映射(Sold Out→Not Available)
commit: 1ef99ba(+未commit工作區)
date: 2026-06-08
valid_under:
  - "雲端 Base URL = https://lab2.citrus.tw/CompassKDS/（新版部署，Gname/Size 非必填、Status 值域 Available/Not Available）"
  - "local POS = http://{localIp}/KDS/SetSellStatus 沿用 full body(含 Gname/Size)且接受 Sold Out"
  - "App 版本 1.1.5 / SM-X710(Android, One UI)"
revalidate_when:
  - "雲端 SetSellStatus contract 再變（Status 值域或必填欄位調整）"
  - "設備 Server URL 指向其他部署（如 global.citrus.tw/CompassKDS_UAT 舊版仍要 Gname/Size）"
  - "local POS SetSellStatus 契約變更"
tags:
  - type/verification
  - status/pass
---
# 驗證：Android remote soldout 精簡 contract + Status 映射

## 變更範圍
- 新增 commonData/vo/SetItemSellStatusRemoteRequest.kt（精簡 VO：StoreNo/GKID/GID/Status）
- commonData/ApiService.kt#setSellStatusRemote 改用精簡 VO
- ui/domain/ApiRepositoryImpl.kt#setSellStatusRemote 內部把 full request 轉精簡 + Status 映射(Available→Available, 其餘→Not Available)
- local 路徑、ViewModel、流程(local→remote→失敗回滾)皆不變

## 測試方式
SM-X710 實機安裝 debug APK，OkHttp BODY interceptor 攔截真實封包；於 SetStock 點品項開關觸發。

## 測試項目

### 1. 線上 cloud server contract（lab2，curl 直打）
| 步驟 | 預期 | 結果 |
|------|------|------|
| POST 精簡 body Status=Not Available | ApiStatus=1 | ✅ |
| POST Status=Available 還原 | ApiStatus=1 | ✅ |
| 缺必填 Status | 400/ApiStatus=0 | ✅ 擋下 |
| 錯誤 ApiKey | 401 Invalid API Key | ✅ |

### 2. 實機端到端（修正前，Server URL=global UAT 舊版）
| 步驟 | 預期 | 結果 |
|------|------|------|
| 點 soldout→remote 送精簡 body | — | ❌ 400 Gname/Size required（舊版部署仍必填）→ 回滾 local + 跳錯 |

### 3. 實機端到端（Server URL=lab2，未加 Status 映射）
| 步驟 | 預期 | 結果 |
|------|------|------|
| remote 送 Status=Sold Out | — | ❌ ApiStatus=0「Status 僅允許 Available 或 Not Available」→ 回滾 |

### 4. 實機端到端（修正後：Status 映射 + lab2）
| 步驟 | 送出 body | 結果 |
|------|------|------|
| 切 Sold Out | local {…Sold Out}→200；remote {StoreNo,GKID,GID,Status:Not Available} | ✅ ApiStatus=1，無回滾無錯誤 |
| 切回 Available | remote {…Status:Available} | ✅ ApiStatus=1，品項還原 |

## 結論
remote 精簡 body 被 lab2 接受；Status 映射解決值域限制；local 與流程不受影響。**正式發版前須確認設備 Server URL 指向已部署新 contract 的雲端**（global UAT 舊版會 400）。

## 相關模組
- [[Systems/SetSellStatus-Remote串接]]
- [[Systems/SetStockPage]]
- [[Systems/POS-API端點]]
