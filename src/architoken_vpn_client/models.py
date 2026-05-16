from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any


@dataclass(slots=True)
class NodeProfile:
    code: str
    protocol: str
    address: str
    port: int
    uuid: str
    transport: str = "tcp"
    security: str = "reality"
    label: str = ""
    sni: str = ""
    fingerprint: str = "chrome"
    public_key: str = ""
    short_id: str = ""
    spider_x: str = "/"
    flow: str = "xtls-rprx-vision"
    encryption: str = "none"
    subscription: str = ""
    enabled: bool = True

    def to_dict(self) -> dict[str, Any]:
        data = asdict(self)
        data["publicKey"] = data.pop("public_key")
        data["shortId"] = data.pop("short_id")
        data["spiderX"] = data.pop("spider_x")
        return data

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "NodeProfile":
        item = dict(data)
        if "publicKey" in item:
            item["public_key"] = item.pop("publicKey")
        if "shortId" in item:
            item["short_id"] = item.pop("shortId")
        if "spiderX" in item:
            item["spider_x"] = item.pop("spiderX")
        item["port"] = int(item["port"])
        return cls(**item)

    @property
    def display_name(self) -> str:
        return self.label or self.code

