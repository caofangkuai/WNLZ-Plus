package com.wunelezi.plus.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.himiuix.MiuixBasicView;
import com.hchen.himiuix.fragment.Fragment;
import com.hchen.himiuix.dialog.MiuixAlertDialog;
import com.wunelezi.plus.R;
import com.wunelezi.plus.core.InjectionManager;
import com.wunelezi.plus.core.Prefs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CoreFragment extends Fragment {

    private TextView injectStatus;
    private MiuixBasicView vipInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_core, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        injectStatus = view.findViewById(R.id.inject_status);
        vipInfo = view.findViewById(R.id.vip_info);
        View btnInject = view.findViewById(R.id.btn_inject);

        btnInject.setOnClickListener(v -> doInject());

        long vipUntil = Long.parseLong(new Prefs(view.getContext()).get("vip_until", "0"));
        if (vipUntil > 0) {
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(vipUntil));
            vipInfo.setSummary(date);
        }
    }

    private void doInject() {
        injectStatus.setText("注入中...");
        InjectionManager.inject(requireContext(), (success, detail) -> {
            if (!isAdded()) return;
            injectStatus.setText(success ? "完成" : "失败");
            if (success) {
                new MiuixAlertDialog(requireContext())
                        .setTitle("成功")
                        .setMessage("注入完成")
                        .setPositiveButton("确定", null)
                        .show();
            } else {
                new MiuixAlertDialog(requireContext())
                        .setTitle("注入失败")
                        .setMessage("注入出错")
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }
}
