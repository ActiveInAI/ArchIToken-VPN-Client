from __future__ import annotations

import json
import tkinter as tk
from tkinter import messagebox, ttk

from .uri import parse_vless_uri
from .xray import xray_outbound_json


class ClientApp(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("ArchIToken-VPN Client")
        self.geometry("920x680")
        self.minsize(820, 560)
        self.configure(bg="#0f172a")
        self.link_text = tk.Text(self, height=5, wrap="word")
        self.output_text = tk.Text(self, height=20, wrap="none")
        self.fields: dict[str, tk.StringVar] = {}
        self.build()

    def build(self) -> None:
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("TFrame", background="#0f172a")
        style.configure("TLabel", background="#0f172a", foreground="#e2e8f0")
        style.configure("TButton", padding=7)
        style.configure("Header.TLabel", font=("Arial", 18, "bold"), foreground="#f8fafc")

        root = ttk.Frame(self, padding=18)
        root.pack(fill="both", expand=True)
        ttk.Label(root, text="ArchIToken-VPN Client", style="Header.TLabel").pack(anchor="w")
        ttk.Label(root, text="导入 VLESS Reality 链接，生成节点信息和 Xray outbound 配置。").pack(anchor="w", pady=(4, 14))

        ttk.Label(root, text="VLESS Reality 链接").pack(anchor="w")
        self.link_text.pack(fill="x", pady=(4, 10))

        buttons = ttk.Frame(root)
        buttons.pack(fill="x", pady=(0, 10))
        ttk.Button(buttons, text="解析链接", command=self.parse_link).pack(side="left")
        ttk.Button(buttons, text="生成 Xray outbound", command=self.generate_xray).pack(side="left", padx=8)
        ttk.Button(buttons, text="清空", command=self.clear).pack(side="left")

        grid = ttk.Frame(root)
        grid.pack(fill="x", pady=(0, 12))
        names = [
            ("code", "节点代码"),
            ("label", "名称"),
            ("address", "地址"),
            ("port", "端口"),
            ("security", "安全层"),
            ("sni", "SNI"),
            ("fingerprint", "指纹"),
            ("flow", "Flow"),
        ]
        for index, (key, label) in enumerate(names):
            ttk.Label(grid, text=label).grid(row=index // 2, column=(index % 2) * 2, sticky="w", padx=(0, 8), pady=3)
            value = tk.StringVar()
            self.fields[key] = value
            entry = ttk.Entry(grid, textvariable=value, state="readonly", width=42)
            entry.grid(row=index // 2, column=(index % 2) * 2 + 1, sticky="ew", pady=3)
        grid.columnconfigure(1, weight=1)
        grid.columnconfigure(3, weight=1)

        ttk.Label(root, text="输出").pack(anchor="w")
        self.output_text.pack(fill="both", expand=True, pady=(4, 0))

    def read_link(self) -> str:
        return self.link_text.get("1.0", "end").strip()

    def set_output(self, text: str) -> None:
        self.output_text.delete("1.0", "end")
        self.output_text.insert("1.0", text)

    def parse_node(self):
        link = self.read_link()
        if not link:
            raise ValueError("请先粘贴 VLESS Reality 链接")
        return parse_vless_uri(link)

    def parse_link(self) -> None:
        try:
            node = self.parse_node()
            data = node.to_dict()
            for key, value in self.fields.items():
                value.set(str(data.get(key, "")))
            self.set_output(json.dumps(data, ensure_ascii=False, indent=2))
        except Exception as exc:
            messagebox.showerror("解析失败", str(exc))

    def generate_xray(self) -> None:
        try:
            node = self.parse_node()
            self.set_output(xray_outbound_json(node))
        except Exception as exc:
            messagebox.showerror("生成失败", str(exc))

    def clear(self) -> None:
        self.link_text.delete("1.0", "end")
        self.output_text.delete("1.0", "end")
        for value in self.fields.values():
            value.set("")


def run() -> None:
    ClientApp().mainloop()

