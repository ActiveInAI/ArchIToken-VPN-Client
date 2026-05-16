import base64
import json
import unittest

from architoken_vpn_client.models import NodeProfile
from architoken_vpn_client.subscription import parse_subscription
from architoken_vpn_client.uri import export_vless_uri, parse_vless_uri
from architoken_vpn_client.xray import xray_outbound


LINK = (
    "vless://123e4567-e89b-12d3-a456-426614174000@203.0.113.10:443"
    "?type=tcp&encryption=none&security=reality&fp=chrome&sni=www.microsoft.com"
    "&pbk=PUBLIC_KEY&sid=abcd&spx=%2F&flow=xtls-rprx-vision#USA-LAX-A1"
)


class UriTests(unittest.TestCase):
    def test_parse_vless_uri(self):
        node = parse_vless_uri(LINK)
        self.assertEqual(node.code, "USA-LAX-A1")
        self.assertEqual(node.address, "203.0.113.10")
        self.assertEqual(node.public_key, "PUBLIC_KEY")
        self.assertEqual(node.short_id, "abcd")

    def test_export_roundtrip(self):
        node = parse_vless_uri(LINK)
        exported = export_vless_uri(node)
        self.assertEqual(parse_vless_uri(exported).code, "USA-LAX-A1")

    def test_subscription_base64(self):
        encoded = base64.b64encode((LINK + "\n").encode()).decode()
        nodes = parse_subscription(encoded)
        self.assertEqual(len(nodes), 1)
        self.assertEqual(nodes[0].code, "USA-LAX-A1")

    def test_xray_outbound(self):
        node = parse_vless_uri(LINK)
        outbound = xray_outbound(node)
        self.assertEqual(outbound["protocol"], "vless")
        self.assertEqual(outbound["tag"], "proxy-usa-lax-a1")

    def test_from_dict(self):
        data = json.loads(json.dumps(parse_vless_uri(LINK).to_dict()))
        node = NodeProfile.from_dict(data)
        self.assertEqual(node.code, "USA-LAX-A1")


if __name__ == "__main__":
    unittest.main()

