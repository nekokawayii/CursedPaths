# AGENTS.md · 咒命歧路 / Cursed Paths

给在本仓库工作的 AI 代理 / 协作者的固定约定。**每次开工先读这一页，按这里的流程做。**

---

## 0. 硬性要求（最高优先级）

### 0.1 每次版本更新都必须写入/修改桌面更新日志

**文件夹：`C:\Users\Administrator\Desktop\咒命歧路-发布记录\`**（在**桌面**，不在仓库里）

| 内容 | 路径 |
|---|---|
| 更新日志 | `C:\Users\Administrator\Desktop\咒命歧路-发布记录\咒命歧路-更新日志.md` |
| 各版 Release 请求体 | `C:\Users\Administrator\Desktop\咒命歧路-发布记录\rel-rXXX.json` |
| 预览图 / 截图 | `C:\Users\Administrator\Desktop\咒命歧路-发布记录\图片\` |

- **每次版本号变动都要写**：新增当版条目，并**保留全部历史，不得删减或重写旧条目**。
- 文件头部表格必须同步更新：`当前版本`、`最后更新`、`对应提交`、`上一次对外发布`。
- Release 说明直接复用该文件里「## rXXX（本次发布）」那一节，保证一处维护、两处一致。

**判定标准：版本号 +1 而没有更新这份日志 = 本次交付未完成。**

#### 要求：必须写得"详细"，不能一行带过

风格参考（用户指定）：`C:\Users\Administrator\Desktop\sukuna-Shrine\试玩说明-rXXX-*.md`
—— 那批文档每篇 3–9 KB，逐技能、逐系统写清"玩家会看到什么/怎么操作/数值是多少/没做什么"。

当版条目至少包含这几节：

| 小节 | 写什么 |
|---|---|
| **安装 / 前置** | 装哪个 jar、需要什么前置、和旧版能不能共存、要不要开新世界 |
| **本次改动（分主题）** | 每个功能一段：改了哪个文件/类、玩家侧表现是什么、关键数值（原名/原值/新值） |
| **操作方式** | 新按键、新指令、新界面的具体路径（按键 → 界面 → 选格 → 释放） |
| **数值表** | 技能伤害/冷却/半径/层数等，用表格列 |
| **请验清单** | 编号列出玩家在游戏里怎么验、看什么现象（可勾选） |
| **已知问题 / 未做** | 明确写清这版没做的、已知会出现的现象（光影下失效、性能、交互冲突等） |

禁止只写"修复若干问题""优化体验"这种空话；每一条都要能让人照着验收。

### 0.2 发布前必须留档

在发布/大规模清理之前，先把当前状态留一份：

```powershell
git archive --format=zip -o "C:\Users\Administrator\Desktop\留档\sukuna-jogo-rXXX-源码.zip" HEAD
# 再把 Desktop\打包 里的 -ask / -auto-share 两个 jar 拷过去并改名带 r 号
```

### 0.3 不要提交垃圾

仓库只保留：`src/`、构建配置（`build.gradle`/`settings.gradle`/`gradle.properties`/`gradlew(.bat)`）、`.gitignore`、`LICENSE`、`README.md`、`使用说明.md`、`DISCLAIMER.md`。

**不要进版本控制**：`build/`、`.gradle/`、`libs/`、`gradle/wrapper/`、`tools/`、`scripts/`、`run-client/`、`world/`、`config/`、`logs/`、任何 `*.log`、`hs_err_pid*`、生成报告 json、预览图。

> 移出版本控制 ≠ 删除：`libs/`、`gradle/wrapper/`、`build.gradle` 家族是**构建必需**，必须留在本地。动完删除/忽略类操作后，**立刻跑一次构建**确认没把必需文件删掉。

---

## 1. 项目速览

| 项 | 值 |
|---|---|
| 名称 | 咒命歧路 / Cursed Paths（非官方《咒术回战》二次创作） |
| 平台 | Minecraft **Forge 1.20.1** |
| modid | `jujutsu_kaisen_cursed_paths` |
| 包名 | `cn.blockforge.generated.nnnnr1201forge2`（历史遗留旧名，不要改） |
| 分支 | `jogo` 与 `main` 逐版保持一致；`neoforge-1.21.1` 已冻结在 r375 |
| 前置 | **Photon 1.1.17 必需**；可选 PlayerAnimator / Touhou Little Maid / JEI / Jade |
| 协议 | LICENSE v2.0（署名+禁商用+相同方式共享）+ DISCLAIMER.md（独立商用免责） |

## 2. 构建

```powershell
.\gradlew build --console=plain --offline "-Dnet.minecraftforge.gradle.check.certs=false"
```

- 约 16–25 秒；产物自动同步到 `E:\hmcl\.minecraft\versions\1.20.1test\mods` 与 `Desktop\打包`
- `build.gradle` 的 `version` **每版 +1**（当前形如 `1.8.1-original-textures-rXXX`）
- 本机**没有系统 gradle**，`gradle/` wrapper 必须存在（备份在 `Desktop\留档\gradle-wrapper-backup`）
- `libs/` 里 5 个 jar 是编译前置（Photon / TLM / JEI / Jade / PlayerAnimator），缺了编译必失败

## 3. 发布流程

1. 改 `build.gradle` 版本号 → 构建
2. **更新桌面更新日志**（见 0.1）
3. `git tag -a v1.8.1-original-textures-rXXX -m "咒命歧路 rXXX"` → 推 tag
4. 建 GitHub Release（不依赖 gh CLI）：
   - token：`git credential fill`（Windows 凭据管理器里的 `gho_...`）
   - **请求体先用 Python 写成 UTF-8 JSON 文件，再用 `[IO.File]::ReadAllBytes` 作为 `-Body`**（直接 `ConvertTo-Json` 传中文会 400 `Problems parsing JSON`）
   - 上传资产：`$rel.upload_url -replace '\{.*\}$',''` 后拼 `?name=<文件名>`，`ContentType=application/java-archive`
5. 推 `jogo` / `main`：

```powershell
git push origin jogo
git checkout main; git merge --ff-only jogo; git push origin main; git checkout jogo
```

> `git push` 在 PowerShell 里的进度输出走 stderr，`[exit code: 1]` 红字**不代表失败**，看 `xxx..yyy branch -> branch` 那行。

## 4. 代码约定（踩过的坑）

- **索敌/结算**一律走 `CombatSettings.canHit/hurt`；**破方块**一律走 `SkillDestruction`（自动继承 `sukunaDesblock` 开关与领域/结界保护）。赤血操术是**独立通道**（伤害 `CombatSafety.hurt`、施法 `BloodArts.input`、特效 `Channel.send` 直发），加全局系统时要单独给它补。
- **两套伤害/施法/特效通道**：改全局效果前先确认目标角色走的是哪条。
- 新角色 = `character/<id>/` 四件套 + `CharacterRegistry.register` + `GeneratedMod` 里 `registerEffect` + `Client.receive` 号段分派。
- 自绘着色器：`RegisterShadersEvent` 用 `modBus.addListener(类::shaders)`（方法引用）；手工注册的类**不要**再加 `@Mod.EventBusSubscriber`（会重复注册）。
- **清理/移出版本控制后必须跑一次构建**：未跟踪的本地必需文件（`build.gradle`、`libs/`）被误删过两次。
- 字符串判重用**完整调用形态**或逐行比对，别用裸关键词（会被注释误命中）。

## 5. 交付时必须说清

- 逐条对应：用户提的每一点改了什么、在哪个文件
- 常量表：涉及数值的改动列「原名 / 原值 / 新值」
- **请验清单**：玩家在游戏里怎么验、看什么现象
- **不得声称已实机验证**——只能声明「编译/打包通过」；`runClient` 因 Photon mixin 在开发环境崩溃，无法自检

## 6. 更新日志维护速查

```text
桌面\咒命歧路-发布记录\
├── 咒命歧路-更新日志.md      ← 唯一定稿的更新日志
│   ├── 头部表格（当前版本 / 最后更新 / 对应提交 / 上次发布）
│   ├── ## rXXX（本次发布）· 日期
│   │   ├── 破坏方块 / 招式
│   │   ├── 界面与数值
│   │   ├── 协议与文档
│   │   └── 工程
│   ├── ## 历史版本（rA → rB）   ← 逐版一行，永不删除
│   └── ## 更早版本（已发布 Release）
├── rel-rXXX.json            ← 每版 Release 请求体（可留档）
└── 图片\                     ← 预览图 / 截图 / 方案图
```
