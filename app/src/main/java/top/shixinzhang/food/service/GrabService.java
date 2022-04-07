package top.shixinzhang.food.service;

import android.accessibilityservice.AccessibilityService;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.shixinzhang.food.util.Helper;

/**
 * @description : GrabService
 * @create by : zhangshixin
 */
public class GrabService extends AccessibilityService {
    public final String TAG = getClass().getSimpleName();
    public static final String PACKAGE_MEITUAN = "com.meituan.retail.v.android";

    final List<Integer> supportEventTypes = Arrays.asList(
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_CLICKED
    );

    public interface IGrabHandler {
        void handlerEvent(AccessibilityService service, AccessibilityEvent event);
    }

    Map<String, IGrabHandler> appHandlerMap = new HashMap<>();

    public GrabService() {
        Helper.sService = this;

        appHandlerMap.put(PACKAGE_MEITUAN, new MeituanGrabHandler());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!supportEventTypes.contains(event.getEventType())) {
            return;
        }

        CharSequence eventPackageName = event.getPackageName();
        if (eventPackageName == null) {
            return;
        }

        IGrabHandler grabHandler = appHandlerMap.get(eventPackageName.toString());
        if (grabHandler != null) {
            grabHandler.handlerEvent(this, event);
        }
    }

    @Override
    public void onInterrupt() {

    }

}
