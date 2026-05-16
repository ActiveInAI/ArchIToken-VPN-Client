from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from . import __version__
from .subscription import parse_subscription
from .uri import export_vless_uri, parse_vless_uri
from .xray import xray_outbound_json


def read_text(value: str) -> str:
    path = Path(value)
    if path.exists():
        return path.read_text(encoding="utf-8")
    return value


def cmd_parse(args: argparse.Namespace) -> int:
    node = parse_vless_uri(read_text(args.link), fallback_code=args.code)
    print(json.dumps(node.to_dict(), ensure_ascii=False, indent=2))
    return 0


def cmd_xray(args: argparse.Namespace) -> int:
    node = parse_vless_uri(read_text(args.link), fallback_code=args.code)
    print(xray_outbound_json(node))
    return 0


def cmd_subscription(args: argparse.Namespace) -> int:
    nodes = parse_subscription(read_text(args.input))
    print(json.dumps([node.to_dict() for node in nodes], ensure_ascii=False, indent=2))
    return 0


def cmd_export(args: argparse.Namespace) -> int:
    data = json.loads(Path(args.input).read_text(encoding="utf-8"))
    from .models import NodeProfile

    node = NodeProfile.from_dict(data)
    print(export_vless_uri(node))
    return 0


def cmd_gui(_: argparse.Namespace) -> int:
    from .gui import run

    run()
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="architoken-vpn-client")
    parser.add_argument("--version", action="version", version=f"%(prog)s {__version__}")
    sub = parser.add_subparsers(required=True)

    parse_cmd = sub.add_parser("parse", help="Parse a VLESS Reality link")
    parse_cmd.add_argument("link", help="VLESS link or path to a text file")
    parse_cmd.add_argument("--code", default="CUSTOM-NOD-A1", help="Fallback node code")
    parse_cmd.set_defaults(func=cmd_parse)

    xray_cmd = sub.add_parser("xray-outbound", help="Convert a VLESS link to an Xray outbound JSON")
    xray_cmd.add_argument("link", help="VLESS link or path to a text file")
    xray_cmd.add_argument("--code", default="CUSTOM-NOD-A1", help="Fallback node code")
    xray_cmd.set_defaults(func=cmd_xray)

    sub_cmd = sub.add_parser("subscription", help="Parse a plain or base64 subscription")
    sub_cmd.add_argument("input", help="Subscription text or path")
    sub_cmd.set_defaults(func=cmd_subscription)

    export_cmd = sub.add_parser("export-link", help="Export a NodeProfile JSON file as VLESS URI")
    export_cmd.add_argument("input", help="NodeProfile JSON path")
    export_cmd.set_defaults(func=cmd_export)

    gui_cmd = sub.add_parser("gui", help="Open desktop UI")
    gui_cmd.set_defaults(func=cmd_gui)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        return args.func(args)
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

