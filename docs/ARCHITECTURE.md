# 客户端架构

## 分层

```text
UI Layer
  ├─ Desktop: Tauri / native shell
  ├─ Android: native app or v2rayNG fork route
  └─ Future iOS/macOS native shells

Core Layer
  ├─ node parser
  ├─ subscription parser
  ├─ QR generator/parser
  ├─ Xray config generator
  ├─ route mode model
  └─ diagnostics model

Runtime Layer
  ├─ Xray-core
  ├─ optional TUN/routing helper
  └─ platform service integration
```

## 核心策略

- 分享链接、订阅、二维码先统一为内部 `NodeProfile`。
- 各平台只负责 UI 和系统权限。
- Xray 配置生成由共享 core 负责，避免每个平台重复实现。
- 平台运行时和管理员凭证分离。

## 两种客户端路线

### 兼容路线

不复制 v2rayN/v2rayNG 代码，只兼容其数据格式和使用流程：

- 生成可导入链接
- 生成订阅
- 生成二维码
- 提供配置助手

这条路线可以与主仓库 MIT 文档协同，也可以在 GPL 客户端仓库中开发。

### Fork 路线

直接 fork 或改造 v2rayN/v2rayNG：

- 优点：复用成熟客户端能力。
- 代价：必须遵守 GPL-3.0，保留许可证和源码公开。
- 建议：如果走 fork，每个平台单独子目录或单独仓库，保留 upstream 记录。

