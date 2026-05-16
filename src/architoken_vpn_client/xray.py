from __future__ import annotations

import json

from .models import NodeProfile


def outbound_tag(node: NodeProfile) -> str:
    return f"proxy-{node.code.lower()}"


def xray_outbound(node: NodeProfile) -> dict:
    user = {
        "id": node.uuid,
        "encryption": node.encryption,
    }
    if node.flow:
        user["flow"] = node.flow

    return {
        "tag": outbound_tag(node),
        "protocol": "vless",
        "settings": {
            "vnext": [
                {
                    "address": node.address,
                    "port": node.port,
                    "users": [user],
                }
            ]
        },
        "streamSettings": {
            "network": node.transport,
            "security": node.security,
            "realitySettings": {
                "serverName": node.sni,
                "fingerprint": node.fingerprint,
                "publicKey": node.public_key,
                "shortId": node.short_id,
                "spiderX": node.spider_x,
            },
        },
    }


def xray_outbound_json(node: NodeProfile) -> str:
    return json.dumps(xray_outbound(node), ensure_ascii=False, indent=2)

