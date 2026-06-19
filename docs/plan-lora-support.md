# LoRA 支持实现计划

## 目标
在 SD WebUI 模式下，用户可以在 app 内选择 LoRA 并设置权重，生成时自动在 prompt 中追加 `<lora:name:weight>` 语法。

## 实现方案

### 1. 新增数据模型
在 `SdApiModels.kt` 中已有 `SdLoraInfo`（name, alias, path），无需修改。

### 2. 新增 UI 组件 `LoraSelector.kt`
- 可折叠的 Card，标题显示 "LoRA (已选 N 个)"
- 列表展示可用 LoRA，每个 LoRA 一行：勾选框 + 名称 + 权重滑块（0.0~2.0，默认 1.0）
- 仅在 SD 后端时显示

### 3. 修改 ViewModel 状态
**TextToImageState / ImageEditState 新增：**
- `availableLoras: List<SdLoraInfo>` — 服务器返回的 LoRA 列表
- `selectedLoras: Map<String, Float>` — 已选 LoRA 及其权重
- `isSdBackend: Boolean` — 已有，用于控制显示

**ViewModel 新增方法：**
- `loadSdLoras()` — 从 SD API 加载 LoRA 列表
- `toggleLora(name: String)` — 切换 LoRA 选中状态
- `updateLoraWeight(name: String, weight: Float)` — 更新权重

### 4. 修改生成逻辑
在 `TextToImageViewModel.generate()` 和 `ImageEditViewModel.generate()` 中，生成前将已选 LoRA 拼接到 prompt 末尾：
```
原prompt + " <lora:name1:0.8> <lora:name2:1.0>"
```

### 5. 修改 Screen
在 TextToImageScreen 和 ImageEditScreen 中，SD 模式下在 ModelSelector 下方插入 LoraSelector 组件。

### 6. 刷新时机
在 `refreshModels()` 中同时刷新 LoRA 列表。

## 修改文件清单
1. `ui/components/LoraSelector.kt` — 新增
2. `ui/texttoimage/TextToImageViewModel.kt` — 状态 + 方法
3. `ui/texttoimage/TextToImageScreen.kt` — 插入组件
4. `ui/imageedit/ImageEditViewModel.kt` — 状态 + 方法
5. `ui/imageedit/ImageEditScreen.kt` — 插入组件
6. `ui/localization/AppStrings.kt` — LoRA 相关文案
