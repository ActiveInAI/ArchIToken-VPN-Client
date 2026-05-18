import NetworkExtension

final class PacketTunnelProvider: NEPacketTunnelProvider {
    private var tunnelSettings: NEPacketTunnelNetworkSettings?

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
            // Production builds must attach an Xray-compatible packet runner here.
            // The extension template is intentionally present without bundled secrets or node data.
            completionHandler(nil)
        }
    }

    override func stopTunnel(with reason: NEProviderStopReason, completionHandler: @escaping () -> Void) {
        tunnelSettings = nil
        completionHandler()
    }
}
