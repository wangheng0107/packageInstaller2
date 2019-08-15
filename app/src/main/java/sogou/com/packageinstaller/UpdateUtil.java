package sogou.com.packageinstaller;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 应用升级目前是唯一需要下载文件的需求，使用系统的DownloadManager会有比较多的坑
 * 所以这里只用直接下载，下载完成之后更新的方式。
 */

public class UpdateUtil {
    private static final String TAG = UpdateUtil.class.getSimpleName();

    public static final String ACTION_INSTALL_COMPLETE = "cm.android.intent.action.INSTALL_COMPLETE";

    private static class UpdateUtilHolder {
        private static final UpdateUtil INSTANCE = new UpdateUtil();
    }

    private UpdateUtil() {
    }

    public static final UpdateUtil getInstance() {
        return UpdateUtilHolder.INSTANCE;
    }

    public static File getCacheDir(Activity context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            File externalCacheDir = context.getExternalCacheDir();
            return externalCacheDir == null ? context.getCacheDir() : externalCacheDir;
        } else {
            return context.getCacheDir();
        }
    }

    public void download(final Activity activity, String url, String name) {
        String savePath;
        File saveDir = getCacheDir(activity);
        if (saveDir != null) {
            savePath = saveDir.getPath() + "/loan.apk";
        } else {
            return;
        }
        Toast.makeText(activity, "开始下载", Toast.LENGTH_SHORT).show();
        FileDownloader.getImpl().create(url)
                .setPath(savePath)
                .setListener(new FileDownloadListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        Log.e(TAG, "下载进度：" + soFarBytes * 100 / totalBytes + "%");
                    }

                    @Override
                    protected void blockComplete(BaseDownloadTask task) {
                    }

                    @Override
                    protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        /*开始安装*/
                        Log.e(TAG, "开始安装");
                        try {
                            installPackage(activity, task.getTargetFilePath());
                        } catch (Throwable t) {
                            t.fillInStackTrace();
                        }
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                        Log.e(TAG, "paused");
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        Log.e(TAG, "error");
                        e.printStackTrace();
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        Log.e(TAG, "warn");
                    }
                }).start();
    }

    private final String PACKAGE_INSTALLED_ACTION =
            "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED";

    private void install(Activity activity, String string) {
        PackageInstaller.Session session = null;
        try {
            PackageInstaller packageInstaller = activity.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);
            addApkToInstallSession(activity, string, session);
            // Create an install status receiver.
//            Context context = activity;
            Intent intent = new Intent(activity, InstallApkSessionApi.class);
            intent.setAction(PACKAGE_INSTALLED_ACTION);
            PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
            IntentSender statusReceiver = pendingIntent.getIntentSender();
            // Commit the session (this will start the installation workflow).
            session.commit(statusReceiver);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't install package", e);
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            throw e;
        }
    }

    private void addApkToInstallSession(Activity activity, String assetName, PackageInstaller.Session session)
            throws IOException {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        try (OutputStream packageInSession = session.openWrite("package", 0, -1);
             InputStream is = activity.getAssets().open(assetName)) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
            }
        }
    }


//
//
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 通常安装方式
     * @param activity
     * @param apkPath
     */
    private void installCommon(Activity activity,String apkPath) {
        //7.0以上通过FileProvider
        if (Build.VERSION.SDK_INT >= 24) {
            Uri uri = FileProvider.getUriForFile(activity, "sogou.com.packageinstaller.fileProvider", new File(apkPath));
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
            activity.startActivity(intent);
        }
    }


    // 适配androidQ的安装方法。
    public void install29(Context mContext, String apkFilePath) {
        File apkFile = new File(apkFilePath);
        PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sessionParams
                = new PackageInstaller.SessionParams(PackageInstaller
                .SessionParams.MODE_FULL_INSTALL);
        sessionParams.setSize(apkFile.length());
        Set<String> set = new HashSet<String>();
        set.add("android.permission.READ_CALL_LOG");
        set.add("android.permission.READ_SMS");
        sessionParams.setWhitelistedRestrictedPermissions(set);
        int sessionId = createSession(packageInstaller, sessionParams);
        if (sessionId != -1) {
            boolean copySuccess = copyInstallFile(packageInstaller, sessionId, apkFilePath);
            if (copySuccess) {
                execInstallCommand(mContext, packageInstaller, sessionId);
            }
        }
    }

    private int createSession(PackageInstaller packageInstaller,
                              PackageInstaller.SessionParams sessionParams) {
        int sessionId = -1;
        try {
            sessionId = packageInstaller.createSession(sessionParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sessionId;
    }

    private boolean copyInstallFile(PackageInstaller packageInstaller,
                                    int sessionId, String apkFilePath) {
        InputStream in = null;
        OutputStream out = null;
        PackageInstaller.Session session = null;
        boolean success = false;
        try {
            File apkFile = new File(apkFilePath);
            session = packageInstaller.openSession(sessionId);
            out = session.openWrite("base.apk", 0, apkFile.length());
            in = new FileInputStream(apkFile);
            int total = 0, c;
            byte[] buffer = new byte[65536];
            while ((c = in.read(buffer)) != -1) {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            Log.i(TAG, "streamed " + total + " bytes");
            success = true;
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
        return success;
    }

    private void execInstallCommand(Context mContext, PackageInstaller packageInstaller, int sessionId) {
        PackageInstaller.Session session = null;
        try {
            session = packageInstaller.openSession(sessionId);
            Intent intent = new Intent(mContext, InstallResultReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
                    1, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void installPackage(Context mContext, String filepathApk) {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                PackageInstaller pi = mContext.getPackageManager().getPackageInstaller();
                //给定模式，创建新的参数，创建新安装会话，返回唯一 Id
                int sessionId = pi.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
                //打开现有会话，主动执行工作
                PackageInstaller.Session session = pi.openSession(sessionId);
                long sizeBytes = 0;
                final File file = new File(filepathApk);
                if (file.isFile()) {
                    sizeBytes = file.length();
                }
                InputStream in = null;
                OutputStream out = null;
                in = new FileInputStream(filepathApk);
                //打开一个流，将一个APK文件写入会话
                //指定有效长度系统将预先分配底层磁盘空间以优化磁盘上的放置
                out = session.openWrite("app_store_session", 0, sizeBytes);
                int total = 0;
                byte[] buffer = new byte[65536];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    total += len;
                    out.write(buffer, 0, len);
                }
                //根据需要调用，用来确保字节已保留到磁盘
                session.fsync(out);
                in.close();
                out.close();
                Log.v(TAG, "InstallApkViaPackageInstaller - Success: streamed apk " + total + " bytes");
                PendingIntent broadCastTest = PendingIntent.getBroadcast(
                        mContext,
                        sessionId,
                        new Intent(ACTION_INSTALL_COMPLETE),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                //提交之前必须关闭所有流
                session.commit(broadCastTest.getIntentSender());
                session.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.v(TAG, "error");
            }
        }

    }

}
