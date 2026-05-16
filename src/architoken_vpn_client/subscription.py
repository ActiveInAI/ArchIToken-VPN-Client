from __future__ import annotations

import base64

from .models import NodeProfile
from .uri import parse_vless_uri


def decode_subscription_text(text: str) -> str:
    stripped = "".join(text.strip().split())
    if "://" in text:
        return text
    padding = "=" * (-len(stripped) % 4)
    try:
        return base64.urlsafe_b64decode(stripped + padding).decode("utf-8")
    except Exception:
        return text


def parse_subscription(text: str) -> list[NodeProfile]:
    decoded = decode_subscription_text(text)
    nodes: list[NodeProfile] = []
    for line in decoded.splitlines():
        item = line.strip()
        if not item or not item.startswith("vless://"):
            continue
        nodes.append(parse_vless_uri(item))
    return nodes

