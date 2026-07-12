# 信用卡管家 (Credit Card Manager)

基于完整设计方案开发的 Android 应用，支持信用卡管理、消费录入、活动追踪、免息期计算等功能。

## 功能特性

- **银行/卡片管理**：支持多家银行、多张信用卡管理
- **免息期计算**：自动计算最优用卡方案
- **消费录入**：实时预览消费对活动和免息期的影响
- **活动追踪**：支持6种活动类型（金额达标、笔数达标、比例返现、连续周期、首笔触发、手动打卡）
- **年费追踪**：自动统计年费刷免进度
- **数据导入导出**：JSON格式备份与恢复
- **GitHub Actions**：自动构建APK

## 技术栈

- Kotlin + AndroidX
- Room 数据库
- Hilt 依赖注入
- Navigation 组件
- Coroutines + Flow
- ViewBinding

## 项目结构

```
CreditCardManager/
├── .github/workflows/     # CI/CD配置
├── app/
│   ├── build.gradle       # 依赖配置
│   └── src/main/
│       ├── java/com/creditcardmanager/
│       │   ├── data/      # 数据层 (Entity, DAO, Repository)
│       │   ├── di/        # Hilt DI模块
│       │   ├── model/     # 数据模型 + 枚举
│       │   ├── ui/        # UI层 (Activity, Fragment)
│       │   ├── utils/     # 工具类
│       │   └── viewmodel/ # ViewModel层
│       └── res/           # 布局、资源、导航
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 快速开始

### 1. 合并所有Part

将6个ZIP包解压到同一目录：

```bash
mkdir CreditCardManager
cd CreditCardManager
unzip ../CreditCardManager_Part1_Core.zip
unzip ../CreditCardManager_Part2_Repository_DI_Utils.zip
unzip ../CreditCardManager_Part3_ViewModel.zip
unzip ../CreditCardManager_Part4_UI.zip
unzip ../CreditCardManager_Part5_Resources.zip
unzip ../CreditCardManager_Part6_CICD.zip
```

### 2. 生成Gradle Wrapper

```bash
# 需要本地安装Gradle
gradle wrapper --gradle-version 8.4
```

或者手动下载 `gradle-wrapper.jar` 放入 `gradle/wrapper/` 目录。

### 3. 本地构建

```bash
./gradlew assembleDebug
# APK输出: app/build/outputs/apk/debug/app-debug.apk
```

### 4. 上传到GitHub自动构建

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/CreditCardManager.git
git push -u origin main
```

Push后GitHub Actions会自动构建Debug和Release APK。

### 5. 配置签名密钥（可选）

生成签名密钥：
```bash
keytool -genkey -v -keystore ccm-release.keystore -alias ccm -keyalg RSA -keysize 2048 -validity 10000
base64 ccm-release.keystore > signing_key_base64.txt
```

在GitHub仓库 Settings -> Secrets and variables -> Actions 中添加：
- `SIGNING_KEY`: base64编码的keystore内容
- `ALIAS`: ccm
- `KEY_STORE_PASSWORD`: 你的密码
- `KEY_PASSWORD`: 你的密码

然后在Actions页面手动触发 **Release Build**，输入版本号即可自动签名并发布。

## 开发阶段

| 阶段 | 功能 |
|------|------|
| 第一阶段 | 银行/卡片/标签CRUD、消费录入、免息期计算、还款提醒、金额/笔数/返现活动 |
| 第二阶段 | 连续周期/首笔/打卡活动、筛选规则、年费追踪、提醒体系 |
| 第三阶段 | JSON导入导出、数据备份、活动归档、批量操作 |

## License

MIT
