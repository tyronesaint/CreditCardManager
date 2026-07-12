# 信用卡管家 (Credit Card Manager)

一款专为信用卡用户设计的Android应用，帮助管理多张信用卡、跟踪消费活动、计算免息期、管理还款提醒等。

## 功能特性

- **卡片管理**: 添加、编辑、删除信用卡，支持账单日、还款日设置
- **消费录入**: 记录每笔消费，自动计算免息期
- **活动跟踪**: 支持多种活动类型（金额达标、笔数达标、比例返现、连续消费等）
- **还款提醒**: 全局设置提醒天数和提醒时间
- **数据导出/导入**: 支持JSON格式备份和恢复

## 全局设置

在"设置"页面可以配置：
- **还款提醒天数**: 首页显示多少天内到期的还款（默认7天）
- **提醒推送次数**: 到期前3天、1天、当天（可配置）
- **提醒时间**: 统一设置提醒推送时间（默认10:00）

## 技术栈

- Kotlin
- Jetpack Navigation Component
- Room Database
- Hilt Dependency Injection
- Material Design 3

## 签名说明

项目内置固定debug.keystore，确保每次构建的APK签名一致，无需重复安装。

## 构建

```bash
./gradlew assembleDebug
```

或使用GitHub Actions自动构建。
