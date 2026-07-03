# josmtiff

为 [JOSM](https://josm.openstreetmap.de/) 提供 GeoTIFF 影像导入与图层显示支持的插件。

## 环境要求

- **JDK 11 或更高版本**（推荐 JDK 17+）
- 无需单独安装 Gradle，项目已包含 Gradle Wrapper（`gradlew` / `gradlew.bat`）

## 打包

### 编译并运行检查

```bash
# Linux / macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

该命令会编译源码、执行单元测试（如有），并生成插件 JAR。

### 生成发布用 JAR

```bash
./gradlew dist
```

产物位于 `build/dist/josmtiff.jar`，可直接复制到 JOSM 插件目录使用，或作为 GitHub Release 附件上传。

其他常用打包任务：

| 任务 | 说明 | 输出路径 |
|------|------|----------|
| `jar` | 标准 JAR（含版本号） | `build/libs/josmtiff-<version>.jar` |
| `dist` | 发布用 JAR（固定文件名） | `build/dist/josmtiff.jar` |
| `localDist` | 本地开发更新站点 | `build/localDist/` |

### 本地开发更新站点

如需在不重启 JOSM 的情况下频繁测试插件，可生成本地更新站点：

```bash
./gradlew localDist
```

在 JOSM 中（偏好设置 → 插件 → 专家模式）添加更新站点 URL：

```
file:/<项目绝对路径>/build/localDist/list
```

## 测试

### 自动化测试

```bash
./gradlew test
# 或运行全部检查
./gradlew check
```

当前项目尚未包含单元测试源码（`src/test`），上述命令主要验证编译是否通过。后续可在 `src/test/java` 中添加 JUnit 测试。

### 在 JOSM 中手动测试

推荐方式：启动一个独立的、干净的 JOSM 实例，并自动加载当前编译的插件：

```bash
./gradlew runJosm
```

该任务会：

- 下载并启动与 `minJosmVersion`（当前为 **19555**）匹配的 JOSM
- 在 `build/.josm/` 下使用临时配置目录，不影响日常 JOSM 环境
- 自动加载刚编译的插件

手动验证步骤：

1. 在 JOSM 菜单 **影像 → Import GeoTIFF** 中选择 GeoTIFF 文件
2. 确认影像图层正确显示，且地理坐标与地图对齐
3. 在 **工具 → About josmtiff** 中查看插件信息

调试模式（需先设置 `project.josm.debugPort` 属性）：

```bash
./gradlew debugJosm
```

清理 runJosm 产生的临时目录：

```bash
./gradlew cleanJosm
```

## 发布 Release

本项目使用 [gradle-josm-plugin](https://gitlab.com/JOSM/gradle-josm-plugin) 的 GitHub Release 任务发布。JOSM 官方插件列表会从 GitHub 仓库的 **latest** Release 自动拉取新版本。

GitHub 仓库配置见 `build.gradle.kts` 中的 `josm.github` 块（当前为 `daishu0000/josm-geotiff`）。

### 1. 更新版本号

同步修改以下两处：

**`build.gradle.kts`** — 项目版本：

```kotlin
version = "0.0.2"  // 新版本号
```

**`releases.yml`** — 在文件**顶部**追加新条目（最新一条会被发布任务使用）：

```yaml
releases:
  - label: 0.0.2
    minJosmVersion: 19555
    description: |
      本次更新的说明
  - label: 0.0.1
    minJosmVersion: 19555
    description: |
      TIFF image support for JOSM
```

`label` 应与 `build.gradle.kts` 中的 `version` 一致。若 `minJosmVersion` 有变化，需同步更新 `build.gradle.kts` 中 `josm.manifest.minJosmVersion` 和 `josmCompileVersion`。

### 2. 构建并提交

```bash
./gradlew clean build
git add build.gradle.kts releases.yml
git commit -m "Release 0.0.2"
git push
```

### 3. 创建 Git Tag

标签名需与 `releases.yml` 中最新条目的 `label` 对应（可按团队习惯加 `v` 前缀，但需与发布命令保持一致）：

```bash
git tag 0.0.2
git push origin 0.0.2
```

### 4. 配置 GitHub 访问令牌

发布任务需要 GitHub API 权限。设置环境变量：

```bash
# Linux / macOS
export GITHUB_ACCESS_TOKEN=<your_github_personal_access_token>

# Windows PowerShell
$env:GITHUB_ACCESS_TOKEN = "<your_github_personal_access_token>"
```

Personal Access Token 需具备目标仓库的 **Contents** 写权限（用于创建 Release 并上传附件）。

### 5. 创建 GitHub Release 并上传 JAR

```bash
# 根据 releases.yml 最新条目创建 GitHub Release
./gradlew createGithubRelease

# 将 build/dist/josmtiff.jar 上传为 Release 附件
./gradlew publishToGithubRelease
```

也可显式指定版本标签：

```bash
./gradlew createGithubRelease --release-label=0.0.2
./gradlew publishToGithubRelease --release-label=0.0.2
```

发布完成后，GitHub 上的 latest Release 即为 JOSM 插件管理器可检测到的新版本。

## 项目结构

```
josm_tiff/
├── build.gradle.kts      # 构建与插件配置
├── releases.yml          # 发布版本清单
├── gradlew / gradlew.bat # Gradle Wrapper
└── src/main/java/        # 插件源码
    └── org/openstreetmap/josm/plugins/josmtiff/
```

## 参考链接

- [gradle-josm-plugin 文档](https://gitlab.com/JOSM/gradle-josm-plugin)
- [JOSM 插件开发 Wiki](https://wiki.openstreetmap.org/wiki/JOSM/Plugins/DevelopersGuide)
