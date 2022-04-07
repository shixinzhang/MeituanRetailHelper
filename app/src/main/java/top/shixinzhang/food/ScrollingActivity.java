package top.shixinzhang.food;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import top.shixinzhang.food.service.GrabService;
import top.shixinzhang.food.util.Helper;

public class ScrollingActivity extends AppCompatActivity {

    public final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle("买菜助手（美团）");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String packageName = getTargetAppPackageName();
                if (Helper.isAccessibilitySettingsOn(ScrollingActivity.this)) {
                    if (Helper.checkAppInstalled(ScrollingActivity.this, packageName)) {
                        Helper.startApplication(ScrollingActivity.this, packageName);
                        Toast.makeText(ScrollingActivity.this,
                                "开始执行！", Toast.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(view, "美团买菜未安装，请先安装！", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                } else {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);

                    Toast.makeText(ScrollingActivity.this,
                            "请先给予[买菜助手]无障碍权限，这样才能自动操作！", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    String getTargetAppPackageName() {
        //todo 根据选择打开目标 app
        return GrabService.PACKAGE_MEITUAN;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Uri uri = Uri.parse("https://blog.csdn.net/u011240877");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_report) {
            new AlertDialog.Builder(ScrollingActivity.this)
                    .setTitle("功能反馈方式")
                    .setMessage("即将复制作者公众号名称，打开微信搜索后可发消息反馈。你的每个反馈，都将帮助更多买到菜！")
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            contractAuthor();
                        }
                    }).show();

        }
        return super.onOptionsItemSelected(item);
    }

    private void contractAuthor() {

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (cm != null) {
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", "拭心又在思考了我的天");
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);

            Toast.makeText(ScrollingActivity.this, "内容已复制，请到微信搜索", Toast.LENGTH_SHORT).show();
        }

        Helper.openWechat(ScrollingActivity.this);
    }

}