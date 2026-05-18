# Clashproxyselect - Android TV/Phone/Tablet Clash节点选择应用

## 📺 应用简介

Clash/Mihomo 等代理后端的 Android 客户端，支持在电视、手机、平板上方便地选择代理节点。

**应用名称：**
- 英文：`CPS`
- 中文：`节点选择`

## ✨ 主要特性

- 🖥️ **多设备支持** - 完美适配 Android TV、手机、平板
- 🔄 **多后端管理** - 支持同时管理多个 Clash/Mihomo 后端
- 📱 **Material Design 3** - 现代化的用户界面
- ⚡ **实时更新** - 实时显示代理状态和延迟
- 🎯 **遥控器友好** - 专为 Android TV 优化，支持遥控器操作

## 📋 系统要求

### Android TV 支持

| 设备类型 | 最低系统版本 | 推荐系统版本 |
|---------|------------|------------|
| **Android TV** | **Android 8.0 (API 26)** | Android 10.0+ |
| **安卓电视盒子** | Android 8.0+ | Android 9.0+ |
| **手机/平板** | Android 8.0+ | Android 10.0+ |

**重要提示：**
- ✅ **必须支持 Android 8.0 (API 26) 或更高版本**
- ❌ 不支持 Android 7.x 或更低版本
- 📺 推荐使用官方 Android TV 系统或基于 Android TV 的定制系统

### 设备兼容性

**支持的设备类型：**
- ✅ 小米电视盒子
- ✅ NVIDIA Shield TV
- ✅ Google Chromecast with Google TV
- ✅ 华为智慧屏
- ✅ 其他基于 Android TV 的设备
- ✅ 安卓手机和平板

## 🚀 安装方式

### 方式一：直接安装 APK

1. 下载最新版本的 APK 文件：
   - 访问：https://github.com/hggcgd/Clashproxyselect/releases
   - 下载 `app-release.apk` 文件

2. 在 Android 设备上启用"未知来源"安装：
   - 设置 → 安全 → 允许从未知来源安装

3. 安装 APK 文件

### 方式二：通过 U 盘安装（推荐电视使用）

1. 将 APK 文件复制到 U 盘
2. 将 U 盘插入 Android TV
3. 使用文件管理器找到并安装 APK

## 🔧 配置说明

### 首次设置

1. 启动应用后，点击"添加后端"
2. 输入 Clash/Mihomo 后端地址：
   ```
   http://your-server:port
   ```
3. 如果需要，输入密码
4. 保存后即可选择节点

### 多后端管理

- 支持添加多个后端配置
- 可在不同后端之间快速切换
- 自动保存后端配置信息

## 🛠️ 技术信息

- **应用包名**: `com.clashproxyselect.app`
- **最低 API 级别**: 26 (Android 8.0)
- **目标 API 级别**: 35 (Android 15)
- **编译 SDK**: 35
- **构建工具**: Gradle with Kotlin DSL
- **UI 框架**: Jetpack Compose with Material Design 3

## 📱 功能特点

- 🎨 **Material Design 3** - 现代化界面设计
- 📺 **TV 优化** - 专为遥控器操作优化
- 🌐 **网络通信** - 支持自签名证书
- 💾 **本地存储** - 配置信息安全存储
- 🔄 **自动刷新** - 节点状态实时更新

## 📄 开源说明

本项目所有代码由 AI 100% 生成，旨在解决软路由在电视上选择节点不便的问题。

**特点：**
- ✅ 无广告
- ✅ 开源免费
- ✅ 可自行修改功能

## 🔗 相关链接

- **下载地址**: https://github.com/hggcgd/Clashproxyselect/releases
- **问题反馈**: https://github.com/hggcgd/Clashproxyselect/issues

---

**注意**: 本应用需要配合 Clash/Mihomo 后端使用，请确保你已有相应的后端服务。