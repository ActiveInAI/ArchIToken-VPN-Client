package ai.activein.architokenvpnclient;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.IOException;

public class ArchITokenVpnService extends VpnService {
    public static final String ACTION_START = "ai.activein.architokenvpnclient.START_TUNNEL";
    public static final String ACTION_STOP = "ai.activein.architokenvpnclient.STOP_TUNNEL";
    public static final String EXTRA_XRAY_OUTBOUND = "xray_outbound";
    private static final String PREFS = "architoken_vpn_state";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CONFIG = "xray_outbound";
    private ParcelFileDescriptor tun;

    public static void saveConfig(Context context, String config) {
        prefs(context).edit()
                .putString(KEY_CONFIG, config)
                .putString(KEY_STATUS, "已保存节点配置，等待启动 VPN。")
                .apply();
    }

    public static String status(Context context) {
        return prefs(context).getString(KEY_STATUS, "VPN 尚未启动。");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopTunnel("VPN 已停止。");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        String config = intent == null ? "" : intent.getStringExtra(EXTRA_XRAY_OUTBOUND);
        if (config == null || config.trim().isEmpty()) {
            config = prefs(this).getString(KEY_CONFIG, "");
        }
        if (config.trim().isEmpty()) {
            setStatus("缺少 Xray outbound 配置，无法启动。");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        saveConfig(this, config);
        RuntimeState runtime = RuntimeState.detect(this);
        if (!runtime.ready) {
            setStatus(runtime.message);
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        try {
            Builder builder = new Builder()
                    .setSession("ArchIToken-VPN")
                    .setMtu(1500)
                    .addAddress("10.66.0.2", 32)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0);
            tun = builder.establish();
            if (tun == null) {
                setStatus("系统未创建 VPN TUN 接口。");
                stopSelf();
                return Service.START_NOT_STICKY;
            }
            setStatus("VPN TUN 已建立，等待移动端内核运行器接管 fd=" + tun.getFd() + "。");
            return Service.START_STICKY;
        } catch (Exception ex) {
            setStatus("VPN 启动失败: " + ex.getMessage());
            stopSelf();
            return Service.START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        stopTunnel("VPN 服务已退出。");
        super.onDestroy();
    }

    private void stopTunnel(String message) {
        if (tun != null) {
            try {
                tun.close();
            } catch (IOException ignored) {
            }
            tun = null;
        }
        setStatus(message);
    }

    private void setStatus(String value) {
        prefs(this).edit().putString(KEY_STATUS, value).apply();
    }

    private static class RuntimeState {
        final boolean ready;
        final String message;

        RuntimeState(boolean ready, String message) {
            this.ready = ready;
            this.message = message;
        }

        static RuntimeState detect(Context context) {
            File files = context.getFilesDir();
            File runner = new File(files, "architoken-mobile/runner");
            File xray = new File(files, "architoken-mobile/xray");
            File tun2socks = new File(files, "architoken-mobile/tun2socks");
            if (runner.canExecute()) {
                return new RuntimeState(true, "检测到移动端运行器: " + runner.getAbsolutePath());
            }
            if (xray.canExecute() && tun2socks.canExecute()) {
                return new RuntimeState(true, "检测到 Xray 和 tun2socks 运行时。");
            }
            return new RuntimeState(false,
                    "尚未安装移动端 VPN 内核。需要在应用私有目录放置 architoken-mobile/runner，"
                            + "或同时放置可执行的 xray 与 tun2socks；当前不会接管系统流量，避免断网。");
        }
    }
}
