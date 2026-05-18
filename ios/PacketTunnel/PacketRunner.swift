import Foundation
import NetworkExtension

final class PacketRunner: NSObject, ArchITokenPacketCoreRunner {
    func start(with provider: NEPacketTunnelProvider, outboundJSON: String) throws {
        throw PacketRuntimeError.missingPrivateCore
    }

    func stop() {
    }
}
