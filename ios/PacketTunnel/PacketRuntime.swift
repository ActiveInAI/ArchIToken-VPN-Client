import Foundation
import NetworkExtension

@objc(ArchITokenPacketCoreRunner)
protocol ArchITokenPacketCoreRunner {
    func start(with provider: NEPacketTunnelProvider, outboundJSON: String) throws
    func stop()
}

final class PacketRuntime {
    private var runner: ArchITokenPacketCoreRunner?
    private var started = false

    func start(provider: NEPacketTunnelProvider, xrayOutbound: String, completionHandler: @escaping (Error?) -> Void) {
        guard !xrayOutbound.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            completionHandler(PacketRuntimeError.missingConfig)
            return
        }

        guard let runner = loadPrivateRunner() else {
            started = false
            completionHandler(PacketRuntimeError.missingPrivateCore)
            return
        }

        do {
            try runner.start(with: provider, outboundJSON: xrayOutbound)
            self.runner = runner
            started = true
            completionHandler(nil)
        } catch {
            completionHandler(error)
        }
    }

    func stop() {
        runner?.stop()
        runner = nil
        started = false
    }

    private func loadPrivateRunner() -> ArchITokenPacketCoreRunner? {
        for name in ["ArchITokenPacketCore.PacketRunner", "PacketTunnel.PacketRunner", "PacketRunner"] {
            if let type = NSClassFromString(name) as? NSObject.Type,
               let candidate = type.init() as? ArchITokenPacketCoreRunner {
                return candidate
            }
        }
        return nil
    }
}

enum PacketRuntimeError: LocalizedError {
    case missingConfig
    case missingPrivateCore

    var errorDescription: String? {
        switch self {
        case .missingConfig:
            return "PacketTunnel 缺少 Xray outbound 配置。"
        case .missingPrivateCore:
            return "未链接私有 ArchITokenPacketCore.xcframework，PacketTunnel 不会接管流量。"
        }
    }
}
