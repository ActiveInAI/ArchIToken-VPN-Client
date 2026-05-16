import SwiftUI

struct ContentView: View {
    @State private var link: String = ""
    @State private var output: String = "Waiting for VLESS Reality link."

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 14) {
                Text("ArchIToken-VPN Client")
                    .font(.title)
                    .bold()

                Text("iOS MVP: VLESS Reality link checker and profile preview.")
                    .foregroundColor(.secondary)

                TextEditor(text: $link)
                    .frame(minHeight: 120)
                    .border(Color.secondary.opacity(0.3))

                Button("Parse") {
                    output = parse(link: link)
                }
                .buttonStyle(.borderedProminent)

                ScrollView {
                    Text(output)
                        .font(.system(.body, design: .monospaced))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                Spacer()
            }
            .padding()
            .navigationTitle("ArchIToken-VPN")
        }
    }

    private func parse(link: String) -> String {
        let value = link.trimmingCharacters(in: .whitespacesAndNewlines)
        guard value.hasPrefix("vless://") else {
            return "Invalid input: link must start with vless://"
        }
        let body = String(value.dropFirst("vless://".count))
        let parts = body.split(separator: "#", maxSplits: 1).map(String.init)
        let code = parts.count > 1 ? parts[1] : "CUSTOM-NOD-A1"
        let left = parts[0]
        let addressPart = left.split(separator: "@", maxSplits: 1).map(String.init)
        let uuid = addressPart.first ?? ""
        let serverAndQuery = addressPart.count > 1 ? addressPart[1] : ""
        let queryParts = serverAndQuery.split(separator: "?", maxSplits: 1).map(String.init)
        let server = queryParts.first ?? ""
        let params = queryParts.count > 1 ? queryParts[1].replacingOccurrences(of: "&", with: "\n") : ""
        return "Node: \(code)\nServer: \(server)\nUUID: \(mask(uuid))\nParams: \(params)"
    }

    private func mask(_ value: String) -> String {
        guard value.count > 10 else { return "{hidden}" }
        return "\(value.prefix(6))...\(value.suffix(4))"
    }
}

