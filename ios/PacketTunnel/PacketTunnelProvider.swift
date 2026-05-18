import NetworkExtension

final class PacketTunnelProvider: NEPacketTunnelProvider {
    private var tunnelSettings: NEPacketTunnelNetworkSettings?
    private let runtime = PacketRuntime()

    override func startTunnel(options: [String : NSObject]?, completionHandler: @escaping (Error?) -> Void) {
        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "10.66.0.1")
        settings.ipv4Settings = NEIPv4Settings(addresses: ["10.66.0.2"], subnetMasks: ["255.255.255.255"])
        settings.ipv4Settings?.includedRoutes = [NEIPv4Route.default()]
        settings.dnsSettings = NEDNSSettings(servers: ["1.1.1.1", "8.8.8.8"])
        tunnelSettings = settings

        setTunnelNetworkSettings(settings) { error in
            if let error {
                completionHandler(error)
                return
            }
            let config = (self.protocolConfiguration as? NETunnelProviderProtocol)?
                .providerConfiguration?["xrayOutbound"] as? String ?? ""
            self.runtime.start(provider: self, xrayOutbound: config, completionHandler: completionHandler)
        }
    }

    override func stopTunnel(with reason: NEProviderStopReason, completionHandler: @escaping () -> Void) {
        runtime.stop()
        tunnelSettings = nil
        completionHandler()
    }
}
