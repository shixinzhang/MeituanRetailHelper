package top.shixinzhang.food.util;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.List;

import top.shixinzhang.food.ScrollingActivity;
import top.shixinzhang.food.service.GrabService;

public class Helper {
    public static AccessibilityService sService;
    public static boolean isDebugMode;

    public static AccessibilityNodeInfo getRootInActiveWindow() {
        if (sService == null) {
            return null;
        }
        return sService.getRootInActiveWindow();
    }

    /**
     * @param node
     * @return
     */
    public static AccessibilityNodeInfo getClickableNode(AccessibilityNodeInfo node) {
        if (node.isClickable()) {
            return node;
        } else {
            AccessibilityNodeInfo parentNode = node;
            for (int i = 0; i < 5; i++) {
                if (null != parentNode) {
                    parentNode = parentNode.getParent();
                    if (null != parentNode && parentNode.isClickable()) {
                        return parentNode;
                    }
                }
            }
        }

        return null;
    }

    public static boolean clickNode(@NonNull AccessibilityNodeInfo root, final String viewId) {
        AccessibilityNodeInfo nodeInfo = findClickableNode(root, viewId);
        if (null != nodeInfo)
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return false;
    }

    public static AccessibilityNodeInfo findClickableNode(@NonNull AccessibilityNodeInfo root, final String viewId) {
        AccessibilityNodeInfo node = findNode(root, viewId);
        if (null != node)
            return getClickableNode(node);
        return null;
    }

    public static AccessibilityNodeInfo findNode(@NonNull AccessibilityNodeInfo root, final String viewId) {
        if (root == null){
            return null;
        }
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId(viewId);

        if (null == list || list.isEmpty())
            return null;
        return list.get(0);
    }

    public static AccessibilityNodeInfo getNodeByText(AccessibilityNodeInfo rootNode, String text) {
        List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText(text);
        if (null == list || list.size() == 0) return null;

        return list.get(list.size() - 1);
    }

    public static AccessibilityNodeInfo getClickableNode(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (null == rootNode) return null;

        if (TextUtils.isEmpty(text)) return null;

        List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText(text);
        if (null == list || list.size() == 0) return null;

        AccessibilityNodeInfo node = list.get(list.size() - 1);

        if (node.isClickable()) {
            return node;
        } else {
            AccessibilityNodeInfo parentNode = node;
            for (int i = 0; i < 5; i++) {
                if (null != parentNode) {
                    parentNode = parentNode.getParent();
                    if (null != parentNode && parentNode.isClickable()) {
                        return parentNode;
                    }
                }
            }
        }

        return null;
    }

    public static boolean actionText(String buttonText) {
        AccessibilityNodeInfo node = getClickableNode(buttonText);
        Log.d("actionText", buttonText + " getClickableNode ? " + (node != null));
        if (null != node) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        return false;
    }

    public static List<AccessibilityNodeInfo> tryGetNodes(AccessibilityNodeInfo rootNode, String text, int interval, int maxCount) {
        if (rootNode == null) {
            return null;
        }

        for (int i = 0; i < maxCount; i++) {

            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
            if (nodes != null && nodes.size() > 0) {
                return nodes;
            }

            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean clickNode(AccessibilityNodeInfo node) {

        AccessibilityNodeInfo clickableNode = getClickableNode(node);
        if (null != clickableNode) {
            clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        return false;

    }

    public static boolean isAccessibilitySettingsOn(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName() + "/" + GrabService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

            if (accessibilityEnabled != 1) {
                return false;
            }

            String settingValue = Settings.Secure.getString(context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return false;

    }

    public AccessibilityNodeInfo recycle(AccessibilityNodeInfo node) {
        if (null == node)
            return node;

        if (node.getChildCount() == 0) {
            if (node.getText() != null) {
                if (null != node.getText()) {
//                    Log.i(TAG, "node text:" + node.getText().toString());
                }

            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    recycle(node.getChild(i));
                }
            }
        }
        return node;
    }

    public static boolean startApplication(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        Intent launchIntentForPackage = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntentForPackage != null) {
            launchIntentForPackage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntentForPackage);
            return true;
        }
        return false;
    }

    /**
     * 获取前台应用package name
     *
     * @param context
     * @return
     */
    public static String getForegroundApp(Context context) {
        ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> lr = am.getRunningAppProcesses();
        if (lr == null) {
            return null;
        }

        for (ActivityManager.RunningAppProcessInfo ra : lr) {
            if (ra.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                    || ra.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return ra.processName;
            }
        }

        return null;
    }

    public static boolean hasText(@NonNull AccessibilityNodeInfo root, final String text) {
        if (root == null){
            return false;
        }
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText(text);
        return null != list && !list.isEmpty() && list.get(0).isVisibleToUser();
    }


    /**
     * @param context
     * @param className
     * @return
     */
    public static boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className)) {
            return false;
        }

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public static String getTopActivity(Context context) {
        if (context == null) {
            return null;
        }
        String className = null;
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = mActivityManager.getRunningTasks(1);
        if (!list.isEmpty() && list.get(0) != null && list.get(0).topActivity != null) {
            className = list.get(0).topActivity.getClassName();
        }
        return className;
    }


    public static boolean checkAppInstalled(Context context, String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    /**
     * 跳转到微信
     */
    public static void openWechat(Context context){
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            ComponentName cmp = new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(cmp);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // TODO: handle exception
        }
    }
}