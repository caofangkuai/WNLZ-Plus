package com.wunelezi.plus;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.hchen.himiuix.MiuixBottomNavigatorView;
import com.hchen.himiuix.dialog.MiuixAlertDialog;
import com.wunelezi.plus.core.SecurityGuard;
import com.wunelezi.plus.ui.CoreFragment;
import com.wunelezi.plus.ui.LoginFragment;
import com.wunelezi.plus.ui.PluginFragment;
import com.wunelezi.plus.ui.XpFragment;

public class MainActivity extends AppCompatActivity {

    private View content;
    private MiuixBottomNavigatorView navigation;
    private boolean validated = false;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        content = findViewById(R.id.content);
        navigation = findViewById(R.id.navigation);

        applyWindowInsets();

        navigation.setVisibility(View.GONE);
        navigation.setOnItemSelectedListener((item, fromUser) -> {
            int id = item.getId();
            if (id == R.id.nav_core) {
                selectTab(0);
            } else if (id == R.id.nav_plugin) {
                selectTab(1);
            } else if (id == R.id.nav_xp) {
                selectTab(2);
            }
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (validated && currentTab != 0) {
                    selectTab(0);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (!SecurityGuard.gate(this)) {
            showFatal();
            return;
        }
        showLogin();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void showFatal() {
        new MiuixAlertDialog(this)
                .setTitle("错误")
                .setMessage("应用初始化失败，请重试。")
                .setPositiveButton("确定", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showLogin() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, new LoginFragment())
                .commitAllowingStateLoss();
    }

    public void showMain() {
        validated = true;
        navigation.setVisibility(View.VISIBLE);
        selectTab(0);
    }

    private void selectTab(int tab) {
        currentTab = tab;
        Fragment f;
        if (tab == 0) {
            f = new CoreFragment();
        } else if (tab == 1) {
            f = new PluginFragment();
        } else {
            f = new XpFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, f)
                .commitAllowingStateLoss();
        navigation.check(tab == 0 ? R.id.nav_core : tab == 1 ? R.id.nav_plugin : R.id.nav_xp);
    }
}
