---
type: issue
status: done
priority: P1
created: 2026-06-04
updated: 2026-06-04
related: "[[設定頁]], [[KdsScreen容器]], [[訂單卡片元件]], [[Prefs偏好設定]]"
summary: |-
  FLAG:DECISION,ROOTCAUSE | 語系切換有時沒套用/錯亂
  KEY:舊作法用已棄用 updateConfiguration 竄改 resources(不通知 Compose)+SettingPage alpha hack 局部硬刷;靜態字串(stringResource)與品項名稱(prefs.language 直讀)兩套來源更新時機不同→錯亂
  DECISION:[2026-06-04]改用 MainActivity CompositionLocalProvider 提供 LocalConfiguration/LocalContext,languageState 為單一來源,整棵樹一致重組(valid)
---
# ISSUE: 語系切換套用不一致（有時沒套用 / 錯亂）

## 現象
設定頁切換語系後，有時畫面沒更新；有時靜態字串(標題/按鈕)還是舊語言、但品項名稱已變新語言（錯亂）。

## 根因
1. 舊作法在 [[KdsScreen容器]] 用 `context.resources.updateConfiguration(...)`（已棄用）就地竄改 resources，**不會通知 Compose 重組** → 已組好的 `stringResource` 不更新。
2. [[設定頁]] 用 `invalidScreen` alpha(1f↔0.99f) 強制重繪當 band-aid，但只有設定頁有，其他頁沒有。
3. 兩套語言來源不同步：靜態字串走 stringResource(locale)，品項名稱直讀 `prefs.language`（English→eName else cName）→ 更新時機/觸發不同 → 錯亂。

## 修正（2026-06-04）
- [[MainActivity入口]] setContent 最外層用 `CompositionLocalProvider` 提供更新後的 `LocalConfiguration` + `LocalContext`(`createConfigurationContext`)，以 `currentState.languageState` 為單一來源 → 語系一改整棵樹一致重組。
- 移除 KdsScreen 的 updateConfiguration、SettingPage 的 alpha hack。
- 採用理由：不閃爍、不跳頁、單一來源；相對於 attachBaseContext+recreate() 不需整個 Activity 重建。

## 狀態
- [x] 定位根因
- [x] 實作修正（CompositionLocalProvider）
- [ ] 實機驗證：兩台切 English↔华文，標題/按鈕/品項名稱同時切換無殘留
