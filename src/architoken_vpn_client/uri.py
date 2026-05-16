from __future__ import annotations

import re
from urllib.parse import parse_qs, quote, unquote, urlencode, urlparse

from .models import NodeProfile

NODE_CODE_PATTERN = re.compile(r"^[A-Z]{3}-[A-Z0-9]{3}-A[0-9]+$")


class VlessUriError(ValueError):
    pass


def _one(query: dict[str, list[str]], key: str, default: str = "") -> str:
    values = query.get(key)
    if not values:
        return default
    return values[0]


def parse_vless_uri(uri: str, fallback_code: str = "CUSTOM-NOD-A1") -> NodeProfile:
    raw = uri.strip()
    if not raw.startswith("vless://"):
        raise VlessUriError("VLESS link must start with vless://")

    parsed = urlparse(raw)
    if parsed.scheme != "vless":
        raise VlessUriError("Only vless:// links are supported")
    if not parsed.username:
        raise VlessUriError("VLESS link is missing UUID")
    if not parsed.hostname:
        raise VlessUriError("VLESS link is missing server address")

    query = parse_qs(parsed.query, keep_blank_values=True)
    code = unquote(parsed.fragment or "").strip().upper() or fallback_code
    if not NODE_CODE_PATTERN.match(code):
        code = fallback_code

    return NodeProfile(
        code=code,
        label=unquote(parsed.fragment or code),
        protocol="vless",
        address=parsed.hostname,
        port=int(parsed.port or 443),
        uuid=unquote(parsed.username),
        transport=_one(query, "type", "tcp") or "tcp",
        security=_one(query, "security", "reality") or "reality",
        sni=_one(query, "sni", ""),
        fingerprint=_one(query, "fp", "chrome") or "chrome",
        public_key=_one(query, "pbk", ""),
        short_id=_one(query, "sid", ""),
        spider_x=_one(query, "spx", "/") or "/",
        flow=_one(query, "flow", "xtls-rprx-vision") or "xtls-rprx-vision",
        encryption=_one(query, "encryption", "none") or "none",
    )


def export_vless_uri(node: NodeProfile) -> str:
    query = {
        "type": node.transport,
        "encryption": node.encryption,
        "security": node.security,
        "fp": node.fingerprint,
    }
    if node.sni:
        query["sni"] = node.sni
    if node.public_key:
        query["pbk"] = node.public_key
    if node.short_id:
        query["sid"] = node.short_id
    if node.spider_x:
        query["spx"] = node.spider_x
    if node.flow:
        query["flow"] = node.flow
    return (
        f"vless://{quote(node.uuid)}@{node.address}:{node.port}?"
        f"{urlencode(query)}#{quote(node.code)}"
    )

