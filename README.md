# ArchIToken-VPN Client

ArchIToken-VPN Client 是 ArchIToken-VPN 的独立全平台客户端项目。它面向 Windows、Linux、macOS、Android 和后续 iOS，目标是提供统一的节点导入、订阅、二维码、出口检测、TUN/路由模式和用户级状态面板。

本仓库采用 **GPL-3.0-or-later**。这样后续可以合法参考、fork 或改造 GPL-3.0 的 v2rayN / v2rayNG 代码路线；如果直接使用它们的代码，衍生部分必须继续按 GPL-3.0 公开源码和保留声明。

## 目标

- 兼容 Xray VLESS / TCP / Reality / Vision。
- 兼容 v2rayN / v2rayNG 的 VLESS 分享链接、订阅、二维码和节点导入习惯。
- 桌面端提供 TUN、国内直连、全局代理、出口检测、WebRTC 检测、节点延迟测试。
- 移动端优先支持二维码/订阅导入、节点选择、出口检测和系统 VPN 模式。
- 客户端只保存用户侧配置，不把生产管理员凭证写入仓库。

## 与 ArchIToken-VPN 主仓库的关系

- `ActiveInAI/ArchIToken-VPN`：运维、服务端、Linux 托盘、部署文档、节点模板、诊断脚本。
- `ActiveInAI/ArchIToken-VPN-Client`：全平台客户端，GPL 协议，后续可独立发布二进制。

主仓库可以继续 MIT；客户端仓库走 GPL，避免把 GPL 客户端代码混进 MIT 仓库。

## 技术路线

### Phase 1: 协议兼容层

- 解析 VLESS Reality 分享链接。
- 生成/读取订阅列表。
- 生成二维码。
- 统一节点数据模型。
- 输出 Xray JSON 配置片段。

### Phase 2: 桌面客户端

建议优先评估：

- Rust core + Tauri UI，用于 Windows / Linux / macOS。
- Xray-core 作为外部二进制或用户自带运行时。
- 桌面 TUN 由平台权限和 Xray/sing-box 能力组合实现。

### Phase 3: Android 客户端

两条路线：

- 兼容导入 v2rayNG，不复制代码，先做配置助手。
- fork v2rayNG 并改造为 ArchIToken-VPN Android，按 GPL-3.0 发布完整源码。

### Phase 4: Windows 深度集成

两条路线：

- 兼容 v2rayN，先做配置助手和订阅生成器。
- fork v2rayN 并改造为 ArchIToken-VPN Windows，按 GPL-3.0 发布完整源码。

## 当前 MVP

当前 `0.4.0` 版本已经包含一个可运行的跨平台 MVP：

- VLESS Reality 链接解析
- base64/plain 订阅解析
- Xray outbound JSON 生成
- Tk 桌面 UI
- Windows / Linux / macOS x64 和 ARM64 PyInstaller 打包工作流
- Android 客户端：VLESS Reality 导入、订阅拉取、剪贴板导入、Xray outbound JSON 导出、系统 `VpnService` 权限申请、VPN 启动/停止入口、运行时缺失保护、debug/release unsigned APK 和 AAB
- iOS 客户端：VLESS Reality 导入、订阅拉取、剪贴板导入、Xray outbound JSON 导出、NetworkExtension 配置管理、PacketTunnelProvider 模板、simulator 构建和 Xcode 工程包
- tag 发布时自动生成 Release 安装包/可执行文件

移动端 VPN 隧道说明见 [docs/MOBILE_TUNNEL.md](docs/MOBILE_TUNNEL.md)，签名边界见 [docs/SIGNING.md](docs/SIGNING.md)。

## 下载

安装包会发布在：

<https://github.com/ActiveInAI/ArchIToken-VPN-Client/releases>

自动构建目标：

- `ArchIToken-VPN-Client-windows-x86_64.exe`
- `ArchIToken-VPN-Client-windows-arm64.exe`
- `ArchIToken-VPN-Client-linux-x86_64`
- `ArchIToken-VPN-Client-linux-arm64`
- `ArchIToken-VPN-Client-macos-x86_64`
- `ArchIToken-VPN-Client-macos-arm64`
- `ArchIToken-VPN-Client-android-universal-debug.apk`
- `ArchIToken-VPN-Client-android-universal-release-unsigned.apk`
- `ArchIToken-VPN-Client-android-universal-release-unsigned.aab`
- `ArchIToken-VPN-Client-ios-simulator.zip`
- `ArchIToken-VPN-Client-ios-source.zip`

Android release 产物当前为 unsigned，上传商店或企业分发前需要接入 keystore 签名。iOS 真机安装包和 TestFlight 发布需要 Apple Developer 证书、签名身份和 provisioning profile；公开 CI 只能生成未签名 simulator 构建和 Xcode 工程包。

## 本地运行

```bash
python -m pip install -e .
architoken-vpn-client gui
```

CLI 示例：

```bash
architoken-vpn-client parse 'vless://...#USA-LAX-A1'
architoken-vpn-client xray-outbound 'vless://...#USA-LAX-A1'
architoken-vpn-client subscription ./subscription.txt
```

## 本地打包

```bash
./scripts/build-local.sh
```

产物会写入 `dist/`。

## 仓库结构

```text
.
├── docs/                 # 设计、部署、平台路线和许可说明
├── examples/             # 脱敏节点、订阅和链接示例
├── packages/             # 后续各平台客户端包
├── src/                  # Python MVP 客户端
├── shared/               # 共享协议模型和配置 schema
├── scripts/              # 安全扫描和开发辅助脚本
├── tests/                # 单元测试
├── LICENSE
└── THIRD_PARTY_NOTICES.md
```

## 安全要求

公开仓库禁止提交：

- 真实 VLESS 链接
- UUID
- Reality public/private key
- ShortID
- 订阅 token
- 3x-ui 面板路径
- 生产 VPS IP
- 用户邮箱和成员身份信息

发布前运行：

```bash
./scripts/scan-secrets.sh
```

## 上游参考

- Xray-core v26.3.27: <https://github.com/XTLS/Xray-core/releases/tag/v26.3.27>
- v2rayN 7.21.3: <https://github.com/2dust/v2rayN/releases/tag/7.21.3>
- v2rayNG releases: <https://github.com/2dust/v2rayNG/releases>
