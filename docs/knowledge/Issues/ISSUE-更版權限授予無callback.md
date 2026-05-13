---
type: issue
status: todo
priority: P2
created: 2026-05-13
updated: 2026-05-13
tags:
  - type/issue
  - status/todo
  - priority/P2
summary: |-
  FLAG:UX_BUG,UNNECESSARY_PERMISSION
  KEY:requestPermissions沒對應callback,使用者授權後沒反應要再按一次｜額外發現:用getExternalFilesDir根本不需這權限
  DEP:[[MainActivity入口]][[版本更新系統]]
---
# ISSUE: 更版權限請求授予後無 callback

## 現象

`MainActivity.kt:223-243` `intentToUpdate(updateAsk)`：

```kotlin
if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(
        this@MainActivity,
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        888
    )
} else {
    updateAsk.value = true
}
```

`requestPermissions` 觸發系統權限對話框，但 **沒有 override `onRequestPermissionsResult`**。授權後使用者要再點一次版本標籤才會進入下載對話框。

## UX 影響

員工首次點更版會：

1. 點版本標籤
2. 系統權限對話框跳出，按「允許」
3. **什麼都沒發生**（看起來像當機）
4. 困惑下再點一次版本標籤
5. 這次才出現 UpdateDialog

## 修正方向

選項 1：override `onRequestPermissionsResult`：

```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 888 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
        updateAsk.value = true
    }
}
```

但 `updateAsk` 是 Composable scope 內的 `remember` state，Activity 拿不到 — 需要改成 Activity-level state 或 ViewModel 中介。

選項 2：改用 `ActivityResultContracts.RequestPermission` 註冊：

```kotlin
private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) updateAsk.value = true
}
```

選項 3：Android 11+ Scoped Storage 不需要 WRITE_EXTERNAL_STORAGE（`getExternalFilesDir()` 不需）。改用 `targetSdk` 與 manifest 條件 → 移除權限請求。

## 額外發現

[[版本更新系統]] `KtorDownloadUseCase.kt:62-67` 已經用 `getExternalFilesDir(DOWNLOADS)` — **這個路徑不需要 WRITE_EXTERNAL_STORAGE 權限**（App 私有區）。所以權限請求本身可能就是**多餘的**。

## 狀態

- [ ] 待修正（建議走選項 3 — 直接移除權限請求）

## 關聯

- 觸發處：[[MainActivity入口]] intentToUpdate
- 受影響功能：[[版本更新系統]]
