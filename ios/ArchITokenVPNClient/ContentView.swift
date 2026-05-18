import SwiftUI
import Foundation
import UIKit
import NetworkExtension

struct ContentView: View {
    @State private var link: String = ""
    @State private var subscriptionURL: String = ""
    @State private var output: String = "等待导入 VLESS Reality 链接或订阅。"
    @State private var tunnelStatus: String = "系统 VPN 尚未配置。"
    @State private var lastNode: VPNNode?
    private let tunnelManager = TunnelManager()

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 12) {
                Text("导入节点")
                    .font(.headline)

                TextEditor(text: $link)
                    .frame(minHeight: 110)
                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.secondary.opacity(0.35)))

                TextField("订阅 URL，可选", text: $subscriptionURL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                HStack {
                    Button("解析链接", action: parseCurrentInput)
                    Button("拉取订阅", action: fetchSubscription)
                    Button("剪贴板导入", action: pasteClipboard)
                }
                .buttonStyle(.bordered)

                HStack {
                    Button("生成 Xray JSON", action: renderXrayJSON)
                    Button("复制结果", action: copyOutput)
                }
                .buttonStyle(.borderedProminent)

                HStack {
                    Button("保存 VPN 配置", action: saveTunnelConfig)
                    Button("安装 VPN 配置", action: installTunnelProfile)
                }
                .buttonStyle(.bordered)

                HStack {
                    Button("启动 VPN", action: startTunnel)
                    Button("停止 VPN", action: stopTunnel)
                }
                .buttonStyle(.borderedProminent)

                Text(tunnelStatus)
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                ScrollView {
                    Text(output)
                        .font(.system(.body, design: .monospaced))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .textSelection(.enabled)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .padding()
            .navigationTitle("ArchIToken-VPN")
        }
    }

    private func parseCurrentInput() {
        guard let value = firstVless(in: link) else {
            output = "未找到 vless:// 链接。"
            return
        }
        do {
            let node = try VPNNode.parse(value)
            lastNode = node
            output = node.summary
        } catch {
            output = "解析失败: \(error.localizedDescription)"
        }
    }

    private func fetchSubscription() {
        guard let url = URL(string: subscriptionURL.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            output = "请先填写有效订阅 URL。"
            return
        }
        output = "正在拉取订阅..."
        var request = URLRequest(url: url)
        request.setValue("ArchIToken-VPN-iOS/0.5.0", forHTTPHeaderField: "User-Agent")
        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error {
                    output = "订阅拉取失败: \(error.localizedDescription)"
                    return
                }
                let status = (response as? HTTPURLResponse)?.statusCode ?? 0
                let text = String(data: data ?? Data(), encoding: .utf8) ?? ""
                let decoded = decodeSubscription(text)
                guard let value = firstVless(in: decoded) else {
                    output = "订阅 HTTP \(status)，但未找到 vless:// 链接。"
                    return
                }
                link = value
                parseCurrentInput()
            }
        }.resume()
    }

    private func renderXrayJSON() {
        if lastNode == nil {
            parseCurrentInput()
        }
        if let lastNode {
            output = lastNode.xrayJSON
        }
    }

    private func pasteClipboard() {
        if let text = UIPasteboard.general.string {
            link = text
            parseCurrentInput()
        }
    }

    private func copyOutput() {
        UIPasteboard.general.string = output
    }

    private func ensureNode() -> VPNNode? {
        if lastNode == nil {
            parseCurrentInput()
        }
        return lastNode
    }

    private func saveTunnelConfig() {
        guard let node = ensureNode() else { return }
        tunnelManager.saveLocalConfig(node)
        tunnelStatus = "已保存 VPN 配置。"
        output = node.summary
    }

    private func installTunnelProfile() {
        guard let node = ensureNode() else { return }
        tunnelStatus = "正在安装 iOS VPN 配置..."
        tunnelManager.installProfile(node) { result in
            switch result {
            case .success(let message):
                tunnelStatus = message
            case .failure(let error):
                tunnelStatus = "安装失败: \(error.localizedDescription)"
            }
        }
    }

    private func startTunnel() {
        tunnelStatus = "正在启动 iOS VPN..."
        tunnelManager.startTunnel { result in
            switch result {
            case .success(let message):
                tunnelStatus = message
            case .failure(let error):
                tunnelStatus = "启动失败: \(error.localizedDescription)"
            }
        }
    }

    private func stopTunnel() {
        tunnelManager.stopTunnel()
        tunnelStatus = "已请求停止 iOS VPN。"
    }
}

private func firstVless(in text: String) -> String? {
    text.split(whereSeparator: { $0.isWhitespace }).map(String.init).first { $0.hasPrefix("vless://") }
}

private func decodeSubscription(_ value: String) -> String {
    if value.contains("vless://") {
        return value
    }
    let compact = value.filter { !$0.isWhitespace }
    if let data = Data(base64Encoded: compact), let decoded = String(data: data, encoding: .utf8) {
        return decoded
    }
    return value
}

private enum VPNParseError: LocalizedError {
    case scheme
    case address

    var errorDescription: String? {
        switch self {
        case .scheme: return "链接必须以 vless:// 开头。"
        case .address: return "缺少 UUID 或服务器地址。"
        }
    }
}

struct VPNNode {
    let code: String
    let uuid: String
    let address: String
    let port: String
    let params: [String: String]

    static func parse(_ link: String) throws -> VPNNode {
        guard link.hasPrefix("vless://") else { throw VPNParseError.scheme }
        var body = String(link.dropFirst("vless://".count))
        var code = "CUSTOM-NOD-A1"
        if let fragment = body.firstIndex(of: "#") {
            let rawCode = String(body[body.index(after: fragment)...])
            code = rawCode.removingPercentEncoding ?? rawCode
            body = String(body[..<fragment])
        }
        guard let at = body.firstIndex(of: "@") else { throw VPNParseError.address }
        let uuid = String(body[..<at])
        let serverAndQuery = String(body[body.index(after: at)...])
        let parts = serverAndQuery.split(separator: "?", maxSplits: 1, omittingEmptySubsequences: false).map(String.init)
        let server = parts.first ?? ""
        let params = parts.count > 1 ? parseParams(parts[1]) : [:]
        let split = server.lastIndex(of: ":")
        let address = split.map { String(server[..<$0]) } ?? server
        let port = split.map { String(server[server.index(after: $0)...]) } ?? "443"
        return VPNNode(code: code, uuid: uuid, address: address, port: port, params: params)
    }

    var summary: String {
        """
        节点: \(code)
        地址: \(address):\(port)
        UUID: \(mask(uuid))
        传输: \(params["type"] ?? "tcp")
        安全: \(params["security"] ?? "reality")
        SNI: \(params["sni"] ?? "")
        指纹: \(params["fp"] ?? "chrome")
        Flow: \(params["flow"] ?? "xtls-rprx-vision")
        PublicKey: \(mask(params["pbk"] ?? ""))
        ShortID: \(mask(params["sid"] ?? ""))
        """
    }

    var xrayJSON: String {
        """
        {
          "tag": "\(escape(code))",
          "protocol": "vless",
          "settings": {
            "vnext": [{
              "address": "\(escape(address))",
              "port": \(port),
              "users": [{"id": "\(escape(uuid))", "encryption": "none", "flow": "\(escape(params["flow"] ?? "xtls-rprx-vision"))"}]
            }]
          },
          "streamSettings": {
            "network": "\(escape(params["type"] ?? "tcp"))",
            "security": "\(escape(params["security"] ?? "reality"))",
            "realitySettings": {
              "serverName": "\(escape(params["sni"] ?? ""))",
              "fingerprint": "\(escape(params["fp"] ?? "chrome"))",
              "publicKey": "\(escape(params["pbk"] ?? ""))",
              "shortId": "\(escape(params["sid"] ?? ""))"
            }
          }
        }
        """
    }
}

private func parseParams(_ value: String) -> [String: String] {
    var result: [String: String] = [:]
    for item in value.split(separator: "&", omittingEmptySubsequences: false) {
        if item.isEmpty { continue }
        let pair = item.split(separator: "=", maxSplits: 1, omittingEmptySubsequences: false).map(String.init)
        let key = pair[0].removingPercentEncoding ?? pair[0]
        let val = pair.count > 1 ? (pair[1].removingPercentEncoding ?? pair[1]) : ""
        result[key] = val
    }
    return result
}

private func mask(_ value: String) -> String {
    guard value.count > 10 else { return "{hidden}" }
    return "\(value.prefix(6))...\(value.suffix(4))"
}

private func escape(_ value: String) -> String {
    value.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
}
