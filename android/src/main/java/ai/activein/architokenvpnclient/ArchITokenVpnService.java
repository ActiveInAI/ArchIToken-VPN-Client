package ai.activein.architokenvpnclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArchITokenVpnService extends VpnService {
    public static final String ACTION_START = "ai.activein.architokenvpnclient.START_TUNNEL";
    public static final String ACTION_STOP = "ai.activein.architokenvpnclient.STOP_TUNNEL";
    public static final String EXTRA_XRAY_OUTBOUND = "xray_outbound";
    private static final String PREFS = "architoken_vpn_state";
    private static final String KEY_STATUS = "status";
    private static final String KEY_CONFIG = "xray_outbound";
    private static final String CHANNEL_ID = "architoken_vpn";
    private static final int NOTIFICATION_ID = 7101;
    private ParcelFileDescriptor tun;
    private int tunFd = -1;
    private Process runnerProcess;
    private Process xrayProcess;
    private Process tun2socksProcess;

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

        startForeground(NOTIFICATION_ID, notification("正在启动 VPN 运行器..."));

        String outbound = intent == null ? "" : intent.getStringExtra(EXTRA_XRAY_OUTBOUND);
        if (outbound == null || outbound.trim().isEmpty()) {
            outbound = prefs(this).getString(KEY_CONFIG, "");
        }
        if (outbound.trim().isEmpty()) {
            stopWithStatus("缺少 Xray outbound 配置，无法启动。");
            return Service.START_NOT_STICKY;
        }

        saveConfig(this, outbound);
        RuntimeInstaller.installBundledRuntime(this);
        RuntimeState runtime = RuntimeState.detect(this);
        if (!runtime.ready) {
            stopWithStatus(runtime.message);
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
            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (Exception ignored) {
            }
            tun = builder.establish();
            if (tun == null) {
                stopWithStatus("系统未创建 VPN TUN 接口。");
                return Service.START_NOT_STICKY;
            }

            tunFd = tun.detachFd();
            tun = null;
            startRuntime(runtime, tunFd, outbound);
            setStatus("VPN 已启动: " + runtime.mode + " · fd=" + tunFd + " · " + runtime.dir.getAbsolutePath());
            updateNotification("VPN 已启动: " + runtime.mode);
            return Service.START_STICKY;
        } catch (Exception ex) {
            stopTunnel("VPN 启动失败: " + ex.getMessage());
            stopSelf();
            return Service.START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        stopTunnel("VPN 服务已退出。");
        super.onDestroy();
    }

    private void startRuntime(RuntimeState runtime, int fd, String outbound) throws IOException {
        File config = new File(runtime.dir, "xray-config.json");
        writeText(config, fullXrayConfig(outbound));
        File log = new File(runtime.dir, "runtime.log");

        if (runtime.runner != null) {
            Map<String, String> env = new LinkedHashMap<>();
            env.put("ARCHITOKEN_TUN_FD", String.valueOf(fd));
            env.put("ARCHITOKEN_XRAY_CONFIG", config.getAbsolutePath());
            env.put("ARCHITOKEN_SOCKS", "127.0.0.1:10808");
            runnerProcess = startProcess(runtime.runner,
                    args(runtime.argsFile, fd, config),
                    env,
                    log);
            return;
        }

        xrayProcess = startProcess(runtime.xray,
                list("run", "-config", config.getAbsolutePath()),
                new LinkedHashMap<>(),
                log);
        tun2socksProcess = startProcess(runtime.tun2socks,
                args(runtime.argsFile, fd, config),
                new LinkedHashMap<>(),
                log);
    }

    private Process startProcess(File executable, List<String> args, Map<String, String> env, File log) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable.getAbsolutePath());
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(executable.getParentFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        builder.environment().putAll(env);
        return builder.start();
    }

    private List<String> args(File argsFile, int fd, File config) throws IOException {
        if (argsFile != null && argsFile.isFile()) {
            String raw = readText(argsFile)
                    .replace("{tun_fd}", String.valueOf(fd))
                    .replace("{xray_config}", config.getAbsolutePath())
                    .replace("{socks}", "127.0.0.1:10808");
            List<String> result = new ArrayList<>();
            for (String part : raw.split("\\s+")) {
                if (!part.trim().isEmpty()) result.add(part.trim());
            }
            return result;
        }
        return list("-device", "fd://" + fd, "-proxy", "socks5://127.0.0.1:10808");
    }

    private List<String> list(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) result.add(value);
        return result;
    }

    private String fullXrayConfig(String outbound) {
        return "{\n"
                + "  \"log\": {\"loglevel\": \"warning\"},\n"
                + "  \"inbounds\": [{\n"
                + "    \"tag\": \"socks-in\",\n"
                + "    \"listen\": \"127.0.0.1\",\n"
                + "    \"port\": 10808,\n"
                + "    \"protocol\": \"socks\",\n"
                + "    \"settings\": {\"udp\": true}\n"
                + "  }],\n"
                + "  \"outbounds\": [\n"
                + outbound + ",\n"
                + "    {\"tag\": \"direct\", \"protocol\": \"freedom\"},\n"
                + "    {\"tag\": \"block\", \"protocol\": \"blackhole\"}\n"
                + "  ],\n"
                + "  \"routing\": {\"domainStrategy\": \"IPIfNonMatch\", \"rules\": []}\n"
                + "}\n";
    }

    private void stopWithStatus(String message) {
        setStatus(message);
        stopForegroundCompat();
        stopSelf();
    }

    private void stopTunnel(String message) {
        destroy(runnerProcess);
        destroy(tun2socksProcess);
        destroy(xrayProcess);
        runnerProcess = null;
        tun2socksProcess = null;
        xrayProcess = null;

        if (tun != null) {
            try {
                tun.close();
            } catch (IOException ignored) {
            }
            tun = null;
        }
        if (tunFd >= 0) {
            try {
                ParcelFileDescriptor.adoptFd(tunFd).close();
            } catch (IOException ignored) {
            }
            tunFd = -1;
        }
        setStatus(message);
        stopForegroundCompat();
    }

    private void destroy(Process process) {
        if (process == null) return;
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void setStatus(String value) {
        prefs(this).edit().putString(KEY_STATUS, value).apply();
    }

    private void updateNotification(String content) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification(content));
    }

    private Notification notification(String content) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ArchIToken-VPN",
                    NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                0,
                open,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("ArchIToken-VPN")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    private static void writeText(File file, String value) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readText(File file) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        }
        return result.toString();
    }

    private static class RuntimeState {
        final boolean ready;
        final String message;
        final String mode;
        final File dir;
        final File runner;
        final File xray;
        final File tun2socks;
        final File argsFile;

        RuntimeState(boolean ready, String message, String mode, File dir, File runner, File xray, File tun2socks, File argsFile) {
            this.ready = ready;
            this.message = message;
            this.mode = mode;
            this.dir = dir;
            this.runner = runner;
            this.xray = xray;
            this.tun2socks = tun2socks;
            this.argsFile = argsFile;
        }

        static RuntimeState detect(Context context) {
            File dir = RuntimeInstaller.runtimeDir(context);
            File runner = new File(dir, "runner");
            File xray = new File(dir, "xray");
            File tun2socks = new File(dir, "tun2socks");
            File argsFile = new File(dir, "tun2socks.args");
            if (runner.canExecute()) {
                return new RuntimeState(true, "检测到移动端运行器: " + runner.getAbsolutePath(), "runner", dir, runner, null, null, argsFile);
            }
            if (xray.canExecute() && tun2socks.canExecute()) {
                return new RuntimeState(true, "检测到 Xray 和 tun2socks 运行时。", "xray+tun2socks", dir, null, xray, tun2socks, argsFile);
            }
            return new RuntimeState(false,
                    "尚未安装移动端 VPN 内核。可通过私有 CI 注入 android-runtime.zip，"
                            + "或在应用私有目录放置 runner，或同时放置 xray 与 tun2socks。当前不会接管系统流量，避免断网。",
                    "missing", dir, null, null, null, null);
        }
    }
}
