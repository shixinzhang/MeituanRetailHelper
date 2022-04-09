package top.shixinzhang.food.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

import top.shixinzhang.food.util.Helper;

/**
 * @description : MeituanGrabImpl
 * @create by : zhangshixin
 */
public class MeituanGrabHandler implements GrabService.IGrabHandler {
    public final String TAG = getClass().getSimpleName();

    public static final String PAGE_SPLASH = "com.meituan.retail.c.android.splash.SplashActivity";
    public static final String PAGE_HOME = "com.meituan.retail.c.android.newhome.newmain.NewMainActivity";
    public static final String PAGE_DIALOG = "com.meituan.retail.c.android.mrn.mrn.MallMrnModal";
    public static final String PAGE_PLACE_ORDER = "com.meituan.retail.c.android.mrn.mrn.MallMrnActivity";
    public static final String PAGE_PAYING = "com.meituan.android.cashier.activity.MTCashierActivity";

    public static final String ID_TAB_CHART = "com.meituan.retail.v.android:id/img_shopping_cart";
    public static final String ID_TAB_MINE = "com.meituan.retail.v.android:id/rl_main_mine";

    public static final String TEXT_SKIP_AD = "跳过";
    public static final String TEXT_CONFIRM_ADDRESS = "请确认地址";
    public static final String TEXT_CHART = "购物车";
    public static final String TEXT_ORDER_BTN = "结算";
    public static final String TEXT_ORDER_OPEN_BEGIN = "当前不在可下";

    public static final String TEXT_PLACE_ORDER = "提交订单";
    public static final String TEXT_BACK_HOME = "返回购物车";
    public static final String TEXT_DELIVERY_HOME = "送货上门";
    public static final String TEXT_DELIVERY_TIME = "送达时间";
    public static final String TEXT_DELIVERY_TIME_SELECT = "请选择送达";
    public static final String TEXT_DELIVERY_TIME_SELECT_DIALOG = "选择送达时间";
    public static final String TEXT_FINE = "我知道了";
    public static final String TEXT_ENSURE_ADDRESS = "确认并支付";
    public static final String TEXT_GIVE_UP = "放弃机会";

    public static final String TEXT_PAY_NOW = "立即支付";
    public static final String TEXT_PAY = "极速支付";

    public static final int COUNT_CHECK_TEXT = 4;
    public static final int CLICK_PAY_INTERVAL = 100;

    private boolean isGrabStarted = true;
    private GrabState lastState = GrabState.IDLE;
    private long lastClickPayTime;

    public enum GrabState {
        IDLE,
        SPLASH_PAGE,
        HOME_PAGE,
        CHART_PAGE,
        PAYMENT_PAGE,
        SELECT_DELIVERY_TIME_PAGE,
    }

    @Override
    public void handlerEvent(AccessibilityService service, AccessibilityEvent event) {

        String eventClassName = event.getClassName().toString();
        Log.e(TAG, "onAccessibilityEvent: " + event.getEventType() + eventClassName);
        Log.d(TAG, "onAccessibilityEvent: " + event);

        AccessibilityNodeInfo rootNode = service.getRootInActiveWindow();

        switch (eventClassName) {
            case PAGE_SPLASH: //冷启广告
                skipAD(rootNode, service);
                break;
            case PAGE_DIALOG:   //弹窗，场景：确认地址
                String msg = Helper.hasText(rootNode, TEXT_CONFIRM_ADDRESS) ? TEXT_CONFIRM_ADDRESS : "请手动关闭一下弹窗！";
                toastWithVoice(service, msg);
                break;
            case PAGE_PLACE_ORDER:
                //提交订单
                selectTimeAndPlaceOrder(event.getSource(), service);
                break;
            case PAGE_HOME:
                //切换到购物车页面，点击结算
                checkChartAndOrder(rootNode, service);
                break;
            case PAGE_PAYING:
                //正在支付
                // TODO: 4/8/22 提示用户支付
                toastWithVoice(service, "快来支付！");
                break;
            default:
                logd("error state, lastState: " + lastState );

                if (lastState == GrabState.HOME_PAGE) {
                    checkChartAndOrder(rootNode, service);
                } else if (lastState == GrabState.SELECT_DELIVERY_TIME_PAGE) {
                    selectDeliveryTime(rootNode, service);
                } else if (lastState == GrabState.PAYMENT_PAGE) {
                    selectTimeAndPlaceOrder(rootNode, service);
                } else {
                    sleep(200);
                    checkChartAndOrder(rootNode, service);
                }
                break;
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void skipAD(AccessibilityNodeInfo rootNode, AccessibilityService service) {
        Log.d(TAG, "page s1 ");
        updateState(GrabState.SPLASH_PAGE);
        tryClickText(TEXT_SKIP_AD);
    }

    private void updateState(GrabState state) {
        this.lastState = state;
    }

    private boolean tryClickTextWithInterval(String text, long interval) {
        return tryClickTextWithInterval(text, interval, COUNT_CHECK_TEXT);
    }

    private boolean tryClickTextWithInterval(String text, long interval, long maxCount) {

        long now = System.currentTimeMillis();
        if (now - lastClickPayTime < CLICK_PAY_INTERVAL) {
            return false;
        }
        lastClickPayTime = now;
        return tryClickText(text, interval, interval);
    }

    private boolean tryClickText(String text) {
        return tryClickText(text, 200, COUNT_CHECK_TEXT);
    }

    private boolean tryClickText(String text, long interval, long maxCount) {
        for (int i = 0; i < maxCount; i++) {
            boolean clicked = Helper.actionText(text);
            if (clicked) {
                return true;
            }
            sleep(interval);
        }

        return false;
    }

    private void selectTimeAndPlaceOrder(AccessibilityNodeInfo rootNode, AccessibilityService service) {
        Log.d(TAG, "page s3 ");
        updateState(GrabState.PAYMENT_PAGE);

        //0.额外弹窗
        clickDialogIfExist(rootNode, service);

        //1.是否切换到送货上门
        boolean isDeliveryPage = Helper.hasText(rootNode, TEXT_DELIVERY_TIME);

        logd("selectTimeAndPlaceOrder s1, isDeliveryPage? " + isDeliveryPage);

        if (!isDeliveryPage) {
            boolean clicked = tryClickText(TEXT_DELIVERY_HOME);

            if (!clicked) {
                //没有加载到，可能是有弹窗？
                clickDialogIfExist(rootNode, service);
                return;
            }
        }

        //2.是否选择送达时间
        boolean hasDeliveryTimeSelectText = Helper.hasText(rootNode, TEXT_DELIVERY_TIME_SELECT);
        Log.d(TAG, "selectTimeAndPlaceOrder s2, hasDeliveryTimeSelectText? " + hasDeliveryTimeSelectText );
        if (hasDeliveryTimeSelectText) {
            //需要去选择时间
            updateState(GrabState.SELECT_DELIVERY_TIME_PAGE);
            tryClickText(TEXT_DELIVERY_TIME_SELECT);
            return;
        }

        //3.下单
        clickPay(rootNode, service);
    }

    void clickPay(AccessibilityNodeInfo rootNode, AccessibilityService service) {
        boolean clicked = tryClickTextWithInterval(TEXT_PAY_NOW, 100, 3);
        if (clicked) {
            return;
        }

        boolean clickedPay = tryClickTextWithInterval(TEXT_PAY, 100);
        loge("clicked pay? " + clickedPay);
        if (!clickedPay) {
            clickDialogIfExist(rootNode, service);
        }
    }

    void clickDialogIfExist(AccessibilityNodeInfo rootNode, AccessibilityService service) {

        if (Helper.hasText(rootNode, TEXT_FINE)) {
            boolean clicked = tryClickText(TEXT_FINE);

            if (clicked) {
                //点击后切换一下 tab
                Helper.clickNode(rootNode, ID_TAB_MINE);
            }
        }


        if (Helper.hasText(rootNode, TEXT_ENSURE_ADDRESS)) {
            tryClickText(TEXT_ENSURE_ADDRESS);
        }

        //返回购物车
        if (Helper.hasText(rootNode, TEXT_BACK_HOME)) {
            tryClickText(TEXT_BACK_HOME);
            updateState(GrabState.CHART_PAGE);
        }


        if (Helper.findNode(rootNode, ID_TAB_CHART) != null) {
            checkChartAndOrder(rootNode, service);
        } else {
            loge("find ID_TAB_CHART false ");
        }

        if (Helper.hasText(rootNode, TEXT_GIVE_UP)) {
            tryClickText(TEXT_GIVE_UP);
        }
    }

    /**
     * 选择送达时间
     * @param rootNode
     * @param service
     */
    void selectDeliveryTime(AccessibilityNodeInfo rootNode, AccessibilityService service) {
        List<AccessibilityNodeInfo> timeNodes = Helper.tryGetNodes(rootNode, "0-", 200, 5);
        int nodeSize = timeNodes == null ? 0 : timeNodes.size();
        loge("timeNodes: " + nodeSize);

        for (int i = nodeSize-1; i >= 0; i--) {
            boolean clicked = Helper.clickNode(timeNodes.get(i));
            if (clicked) {
                updateState(GrabState.PAYMENT_PAGE);
                break;
            }
        }

    }


    /**
     * 如果没进入购物车页面，先进入购物车，然后结算（没选择物品前，结算可能无法点击，提醒用户）
     * @param rootNode
     * @param service
     */
    private void checkChartAndOrder(AccessibilityNodeInfo rootNode, AccessibilityService service) {
        Log.d(TAG, "page s2 ");
        updateState(GrabState.HOME_PAGE);

        boolean isChartPage = Helper.hasText(rootNode, TEXT_ORDER_BTN);

        logd(" checkChartAndOrder, isChartPage? " + isChartPage);
        if (!isChartPage) {
            //切换到购物车
            boolean clicked = Helper.clickNode(rootNode, ID_TAB_CHART);
            return;
        }

        AccessibilityNodeInfo orderNode = Helper.getNodeByText(rootNode, TEXT_ORDER_BTN);
        if (orderNode != null && orderNode.getText() != null) {
            String text = orderNode.getText().toString();
            loge("text: " + text);
            if (TextUtils.equals("结算(0)", text) || TextUtils.equals("结算", text)) {
                //没有可结算物品
                toastWithVoice(service, "没有可结算物品，请先加到购物车并选择");
                return;
            }
        }

        updateState(GrabState.CHART_PAGE);
        //点击结算
        boolean placeOrdered = tryClickText(TEXT_ORDER_BTN, 100, COUNT_CHECK_TEXT);
        Log.e(TAG, "placeOrdered: " + placeOrdered);

        if (!placeOrdered) {
            clickDialogIfExist(rootNode, service);
        }
    }

    /**
     * todo: 加上声音提示：
     * @param context
     * @param info
     */
    void toastWithVoice(Context context, String info) {

        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
    }

    void toast(Context context, String info) {

        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
    }

    void logd(String s) {
        Log.d(TAG, s);
    }

    void loge(String s) {
        Log.e(TAG, s);
    }
}
