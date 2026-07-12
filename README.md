# 信用卡管家 (Credit Card Manager)

Android 信用卡管理应用，支持消费录入、活动追踪、免息期计算、提醒管理等功能。

## 功能特性

- **银行/卡片管理**：多家银行、多张信用卡，支持固定日期/固定间隔两种还款模式
- **消费录入**：实时预览免息期、命中活动、预计返现
- **免息期计算**：自动计算最优用卡方案，首页展示 Top 3
- **活动追踪**：8种类型（金额达标、笔数达标、比例返现、连续达标、首刷奖、每日签到、连续消费N天、每周固定领取）
- **活动管理**：分类查看（全部/银行/卡片/通用/归档）、排序（按银行/卡片/类型/进度）、点击详情、长按/滑动编辑归档删除
- **提醒系统**：活动领取提醒 + 自定义周期性提醒（每周/每月），支持完成标记自动重置
- **年费追踪**：自动统计年费刷免进度
- **数据导入导出**：JSON 格式备份与恢复
- **GitHub Actions**：自动构建 APK

## 技术栈

Kotlin + AndroidX + Room + Hilt + Navigation + Coroutines/Flow + ViewBinding + Material Design 3

## 快速开始

```bash
./gradlew assembleDebug
```

GitHub Actions 自动构建 Release APK，需配置签名密钥（SIGNING_KEY, ALIAS, KEY_STORE_PASSWORD, KEY_PASSWORD）。

## License

MIT
