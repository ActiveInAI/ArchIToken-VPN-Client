package ai.activein.architokenvpnclient;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Base64;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQUEST_VPN = 7011;
    private EditText linkInput;
    private EditText subscriptionInput;
    private TextView output;
    private Node lastNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ArchIToken-VPN");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Android 客户端：导入 VLESS Reality / 订阅，生成 Xray 出站配置。");
        subtitle.setTextColor(Color.rgb(203, 213, 225));
        subtitle.setTextSize(14);
        root.addView(subtitle);

        linkInput = input("粘贴 vless:// 链接或订阅内容");
        root.addView(linkInput);

        subscriptionInput = input("订阅 URL，可选");
        subscriptionInput.setMinLines(1);
        root.addView(subscriptionInput);

        LinearLayout rowOne = row();
        rowOne.addView(button("解析链接", v -> parseCurrentInput()));
        rowOne.addView(button("拉取订阅", v -> fetchSubscription()));
        root.addView(rowOne);

        LinearLayout rowTwo = row();
        rowTwo.addView(button("生成 Xray JSON", v -> renderXrayJson()));
        rowTwo.addView(button("剪贴板导入", v -> pasteClipboard()));
        rowTwo.addView(button("复制结果", v -> copyOutput()));
        root.addView(rowTwo);

        LinearLayout rowThree = row();
        rowThree.addView(button("保存 VPN 配置", v -> saveTunnelConfig()));
        rowThree.addView(button("启动 VPN", v -> requestStartTunnel()));
        rowThree.addView(button("停止 VPN", v -> stopTunnel()));
        root.addView(rowThree);

        output = new TextView(this);
        output.setTextColor(Color.rgb(226, 232, 240));
        output.setTextSize(14);
        output.setTypeface(android.graphics.Typeface.MONOSPACE);
        output.setText("等待导入 VLESS Reality 链接或订阅。\n" + ArchITokenVpnService.status(this));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(root);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startTunnel();
        } else if (requestCode == REQUEST_VPN) {
            output.setText("用户未授予 Android VPN 权限。");
        }
    }

    private EditText input(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setSingleLine(false);
        edit.setMinLines(3);
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.rgb(148, 163, 184));
        edit.setBackgroundColor(Color.rgb(30, 41, 59));
        return edit;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private Button button(String label, android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(listener);
        button.setAllCaps(false);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return button;
    }

    private void parseCurrentInput() {
        String text = linkInput.getText().toString().trim();
        String link = firstVless(text);
        if (link.isEmpty()) {
            output.setText("未找到 vless:// 链接。");
            return;
        }
        try {
            lastNode = parseNode(link);
            output.setText(lastNode.summary());
        } catch (Exception ex) {
            output.setText("解析失败: " + ex.getMessage());
        }
    }

    private void fetchSubscription() {
        String url = subscriptionInput.getText().toString().trim();
        if (url.isEmpty()) {
            output.setText("请先填写订阅 URL。");
            return;
        }
        output.setText("正在拉取订阅...");
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "ArchIToken-VPN-Android/0.5.0");
                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line).append("\n");
                String decoded = decodeSubscription(body.toString());
                String link = firstVless(decoded);
                runOnUiThread(() -> {
                    if (link.isEmpty()) {
                        output.setText("订阅 HTTP " + code + "，但未找到 vless:// 链接。");
                        return;
                    }
                    linkInput.setText(link);
                    parseCurrentInput();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> output.setText("订阅拉取失败: " + ex.getMessage()));
            }
        }).start();
    }

    private String decodeSubscription(String value) {
        String raw = value.trim();
        if (raw.contains("vless://")) return raw;
        try {
            return new String(Base64.decode(raw, Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private String firstVless(String text) {
        for (String line : text.split("\\s+")) {
            if (line.startsWith("vless://")) return line.trim();
        }
        return "";
    }

    private Node parseNode(String link) throws Exception {
        String body = link.substring("vless://".length());
        String code = "CUSTOM-NOD-A1";
        int fragment = body.indexOf('#');
        if (fragment >= 0 && fragment + 1 < body.length()) {
            code = decode(body.substring(fragment + 1));
            body = body.substring(0, fragment);
        }
        int at = body.indexOf('@');
        int query = body.indexOf('?');
        if (at <= 0) throw new IllegalArgumentException("缺少 UUID 或服务器地址。");
        String uuid = at > 0 ? body.substring(0, at) : "";
        String server = at > 0 ? body.substring(at + 1, query > at ? query : body.length()) : "";
        String params = query >= 0 ? body.substring(query + 1) : "";
        Map<String, String> parsed = params(params);
        return new Node(code, uuid, server, parsed);
    }

    private Map<String, String> params(String value) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String item : value.split("&")) {
            if (item.isEmpty()) continue;
            String[] pair = item.split("=", 2);
            result.put(decode(pair[0]), pair.length > 1 ? decode(pair[1]) : "");
        }
        return result;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
            return value;
        }
    }

    private void renderXrayJson() {
        if (lastNode == null) parseCurrentInput();
        if (lastNode != null) output.setText(lastNode.xrayJson());
    }

    private void saveTunnelConfig() {
        if (lastNode == null) parseCurrentInput();
        if (lastNode == null) return;
        ArchITokenVpnService.saveConfig(this, lastNode.xrayJson());
        output.setText("已保存 VPN 配置。\n" + lastNode.summary() + "\n\n" + ArchITokenVpnService.status(this));
    }

    private void requestStartTunnel() {
        if (lastNode == null) parseCurrentInput();
        if (lastNode == null) return;
        ArchITokenVpnService.saveConfig(this, lastNode.xrayJson());
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQUEST_VPN);
            return;
        }
        startTunnel();
    }

    private void startTunnel() {
        if (lastNode == null) parseCurrentInput();
        if (lastNode == null) return;
        Intent intent = new Intent(this, ArchITokenVpnService.class);
        intent.setAction(ArchITokenVpnService.ACTION_START);
        intent.putExtra(ArchITokenVpnService.EXTRA_XRAY_OUTBOUND, lastNode.xrayJson());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        output.setText("已请求启动 VPN。\n" + ArchITokenVpnService.status(this));
    }

    private void stopTunnel() {
        Intent intent = new Intent(this, ArchITokenVpnService.class);
        intent.setAction(ArchITokenVpnService.ACTION_STOP);
        startService(intent);
        output.setText("已请求停止 VPN。");
    }

    private void pasteClipboard() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null && manager.hasPrimaryClip()) {
            CharSequence text = manager.getPrimaryClip().getItemAt(0).coerceToText(this);
            linkInput.setText(text);
            parseCurrentInput();
        }
    }

    private void copyOutput() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("ArchIToken-VPN", output.getText()));
        }
    }

    private static String mask(String value) {
        if (value.length() <= 10) return "{hidden}";
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
    }

    private static String esc(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class Node {
        final String code;
        final String uuid;
        final String address;
        final String port;
        final Map<String, String> params;

        Node(String code, String uuid, String server, Map<String, String> params) {
            this.code = code;
            this.uuid = uuid;
            this.params = params;
            int colon = server.lastIndexOf(':');
            if (colon > 0 && colon + 1 < server.length()) {
                this.address = server.substring(0, colon);
                this.port = server.substring(colon + 1);
            } else {
                this.address = server;
                this.port = "443";
            }
        }

        String summary() {
            return "节点: " + code + "\n"
                    + "地址: " + address + ":" + port + "\n"
                    + "UUID: " + mask(uuid) + "\n"
                    + "传输: " + params.getOrDefault("type", "tcp") + "\n"
                    + "安全: " + params.getOrDefault("security", "reality") + "\n"
                    + "SNI: " + params.getOrDefault("sni", "") + "\n"
                    + "指纹: " + params.getOrDefault("fp", "chrome") + "\n"
                    + "Flow: " + params.getOrDefault("flow", "xtls-rprx-vision") + "\n"
                    + "PublicKey: " + mask(params.getOrDefault("pbk", "")) + "\n"
                    + "ShortID: " + mask(params.getOrDefault("sid", ""));
        }

        String xrayJson() {
            return "{\n"
                    + "  \"tag\": \"" + esc(code) + "\",\n"
                    + "  \"protocol\": \"vless\",\n"
                    + "  \"settings\": {\n"
                    + "    \"vnext\": [{\n"
                    + "      \"address\": \"" + esc(address) + "\",\n"
                    + "      \"port\": " + port + ",\n"
                    + "      \"users\": [{\"id\": \"" + esc(uuid) + "\", \"encryption\": \"none\", \"flow\": \"" + esc(params.getOrDefault("flow", "xtls-rprx-vision")) + "\"}]\n"
                    + "    }]\n"
                    + "  },\n"
                    + "  \"streamSettings\": {\n"
                    + "    \"network\": \"" + esc(params.getOrDefault("type", "tcp")) + "\",\n"
                    + "    \"security\": \"" + esc(params.getOrDefault("security", "reality")) + "\",\n"
                    + "    \"realitySettings\": {\n"
                    + "      \"serverName\": \"" + esc(params.getOrDefault("sni", "")) + "\",\n"
                    + "      \"fingerprint\": \"" + esc(params.getOrDefault("fp", "chrome")) + "\",\n"
                    + "      \"publicKey\": \"" + esc(params.getOrDefault("pbk", "")) + "\",\n"
                    + "      \"shortId\": \"" + esc(params.getOrDefault("sid", "")) + "\"\n"
                    + "    }\n"
                    + "  }\n"
                    + "}";
        }
    }
}
