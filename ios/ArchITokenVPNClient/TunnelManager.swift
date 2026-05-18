import Foundation
import NetworkExtension

final class TunnelManager {
    private let description = "ArchIToken-VPN"
    private let providerBundleIdentifier = "ai.activein.ArchITokenVPNClient.PacketTunnel"

    func saveLocalConfig(_ node: VPNNode) {
        UserDefaults.standard.set(node.xrayJSON, forKey: "architoken.xray.outbound")
        UserDefaults.standard.set(node.code, forKey: "architoken.node.code")
        UserDefaults.standard.set(node.address, forKey: "architoken.node.address")
    }

    func installProfile(_ node: VPNNode, completion: @escaping (Result<String, Error>) -> Void) {
        saveLocalConfig(node)
        NETunnelProviderManager.loadAllFromPreferences { managers, error in
            if let error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }

            let manager = managers?.first(where: { $0.localizedDescription == self.description }) ?? NETunnelProviderManager()
            let proto = NETunnelProviderProtocol()
            proto.providerBundleIdentifier = self.providerBundleIdentifier
            proto.serverAddress = "\(node.address):\(node.port)"
            proto.providerConfiguration = [
                "nodeCode": node.code,
                "xrayOutbound": node.xrayJSON
            ]
            manager.localizedDescription = self.description
            manager.protocolConfiguration = proto
            manager.isEnabled = true
            manager.saveToPreferences { saveError in
                DispatchQueue.main.async {
                    if let saveError {
                        completion(.failure(saveError))
                    } else {
                        completion(.success("VPN 配置已写入系统。真机连接需要 Apple Network Extension entitlement 和 PacketTunnel 扩展签名。"))
                    }
                }
            }
        }
    }

    func startTunnel(completion: @escaping (Result<String, Error>) -> Void) {
        NETunnelProviderManager.loadAllFromPreferences { managers, error in
            if let error {
                DispatchQueue.main.async { completion(.failure(error)) }
                return
            }
            guard let manager = managers?.first(where: { $0.localizedDescription == self.description }) else {
                DispatchQueue.main.async {
                    completion(.failure(TunnelError.profileMissing))
                }
                return
            }
            do {
                try manager.connection.startVPNTunnel()
                DispatchQueue.main.async { completion(.success("已请求启动系统 VPN。")) }
            } catch {
                DispatchQueue.main.async { completion(.failure(error)) }
            }
        }
    }

    func stopTunnel() {
        NETunnelProviderManager.loadAllFromPreferences { managers, _ in
            managers?.first(where: { $0.localizedDescription == self.description })?.connection.stopVPNTunnel()
        }
    }
}

enum TunnelError: LocalizedError {
    case profileMissing

    var errorDescription: String? {
        switch self {
        case .profileMissing:
            return "未找到 ArchIToken-VPN 系统配置，请先安装 VPN 配置。"
        }
    }
}
