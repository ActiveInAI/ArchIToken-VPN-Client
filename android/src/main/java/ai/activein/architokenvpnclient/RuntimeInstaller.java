package ai.activein.architokenvpnclient;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class RuntimeInstaller {
    private RuntimeInstaller() {
    }

    static void installBundledRuntime(Context context) {
        File target = runtimeDir(context);
        if (new File(target, ".installed").isFile()) return;
        AssetManager assets = context.getAssets();
        try {
            String source = selectAssetRuntime(assets);
            if (source == null) return;
            deleteDir(target);
            target.mkdirs();
            copyAssetDir(assets, source, target);
            markExecutable(new File(target, "runner"));
            markExecutable(new File(target, "xray"));
            markExecutable(new File(target, "tun2socks"));
            if (hasRuntimeFiles(target)) {
                new File(target, ".installed").createNewFile();
            } else {
                deleteDir(target);
            }
        } catch (IOException ignored) {
        }
    }

    static File runtimeDir(Context context) {
        return new File(context.getFilesDir(), "architoken-mobile/current");
    }

    private static String selectAssetRuntime(AssetManager assets) throws IOException {
        for (String abi : Build.SUPPORTED_ABIS) {
            String candidate = "architoken-mobile/" + abi;
            if (assetDirExists(assets, candidate) && hasRuntimeAssets(assets, candidate)) return candidate;
        }
        return assetDirExists(assets, "architoken-mobile") && hasRuntimeAssets(assets, "architoken-mobile") ? "architoken-mobile" : null;
    }

    private static boolean assetDirExists(AssetManager assets, String path) throws IOException {
        String[] children = assets.list(path);
        return children != null && children.length > 0;
    }

    private static boolean hasRuntimeAssets(AssetManager assets, String path) {
        return assetFileExists(assets, path + "/runner")
                || (assetFileExists(assets, path + "/xray") && assetFileExists(assets, path + "/tun2socks"));
    }

    private static boolean assetFileExists(AssetManager assets, String path) {
        try (InputStream ignored = assets.open(path)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean hasRuntimeFiles(File target) {
        File runner = new File(target, "runner");
        File xray = new File(target, "xray");
        File tun2socks = new File(target, "tun2socks");
        return runner.isFile() || (xray.isFile() && tun2socks.isFile());
    }

    private static void copyAssetDir(AssetManager assets, String source, File target) throws IOException {
        String[] children = assets.list(source);
        if (children == null || children.length == 0) {
            copyAssetFile(assets, source, target);
            return;
        }
        target.mkdirs();
        for (String child : children) {
            copyAssetDir(assets, source + "/" + child, new File(target, child));
        }
    }

    private static void copyAssetFile(AssetManager assets, String source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream input = assets.open(source); FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        }
    }

    private static void markExecutable(File file) {
        if (file.isFile()) file.setExecutable(true, true);
    }

    private static void deleteDir(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteDir(child);
            }
        }
        file.delete();
    }
}
