# 平台策略

## Windows

优先兼容 v2rayN 的用户习惯：

- VLESS Reality 链接导入
- 订阅导入
- QR 导入
- Xray-core runtime
- TUN / system proxy 模式

如果 fork v2rayN：

- 保留 GPL-3.0
- 保留 upstream notice
- 保留源码发布
- 明确 ArchIToken 定制差异

## Android

优先兼容 v2rayNG 的用户习惯：

- QR 扫码
- 订阅导入
- 节点测速
- VPNService 模式
- 分应用代理和路由规则

如果 fork v2rayNG：

- 保留 GPL-3.0
- 保留 upstream notice
- 保留源码发布
- 明确 ArchIToken 定制差异

## Linux

与 ArchIToken-VPN 主仓库协同：

- 托盘和诊断先由主仓库维护
- 客户端仓库后续可做统一桌面壳
- Xray 入口保持 10808/10809 兼容

## macOS

第一阶段做配置助手：

- 链接/订阅/二维码生成
- 出口检测文档
- 客户端兼容性清单

第二阶段评估原生 Network Extension 或桌面客户端。

## iOS

先保持文档和订阅兼容。iOS 上架、Network Extension、证书和审核要求需要单独评估。

