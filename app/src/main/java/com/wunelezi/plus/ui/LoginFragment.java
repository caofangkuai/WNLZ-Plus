package com.wunelezi.plus.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.himiuix.MiuixBasicView;
import com.hchen.himiuix.MiuixEditTextView;
import com.hchen.himiuix.dialog.MiuixAlertDialog;
import com.hchen.himiuix.fragment.Fragment;
import com.wunelezi.plus.MainActivity;
import com.wunelezi.plus.R;
import com.wunelezi.plus.core.Prefs;
import com.wunelezi.plus.core.WeiYanApi;

public class LoginFragment extends Fragment {

    private MiuixEditTextView kamiInput;
    private MiuixBasicView noticeView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        kamiInput = view.findViewById(R.id.et_kami);
        noticeView = view.findViewById(R.id.notice_view);
        View btnLogin = view.findViewById(R.id.btn_login);
        View btnUnbind = view.findViewById(R.id.btn_unbind);

        kamiInput.setHint("请输入卡密");
        kamiInput.setText(new Prefs(view.getContext()).get("kami", ""));

        btnLogin.setOnClickListener(v -> doLogin());
        btnUnbind.setOnClickListener(v -> doUnbind());

        loadNotice();
        checkUpdate();
    }

    private void loadNotice() {
        WeiYanApi.fetchNotice(requireContext(), notice -> {
            if (!isAdded()) return;
            noticeView.setSummary(notice != null && !notice.isEmpty() ? notice : "暂无公告");
        });
    }

    private void checkUpdate() {
        WeiYanApi.checkUpdate(requireContext(), (needUpdate, show, url, must) -> {
            if (!isAdded()) return;
            if (needUpdate && show != null && !show.isEmpty()) {
                new MiuixAlertDialog(requireContext())
                        .setTitle("更新")
                        .setMessage(show)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }

    private void doLogin() {
        String kami = kamiInput.getText().toString().trim();
        if (kami.isEmpty()) {
            showError("请输入卡密");
            return;
        }
        new Prefs(requireContext()).put("kami", kami);
        WeiYanApi.login(requireContext(), kami, (success, message, vipSeconds) -> {
            if (!isAdded()) return;
            if (success) {
                new Prefs(requireContext()).put("vip_until", String.valueOf(vipSeconds * 1000));
                ((MainActivity) requireActivity()).showMain();
            } else {
                showError(message);
            }
        });
    }

    private void doUnbind() {
        String kami = kamiInput.getText().toString().trim();
        if (kami.isEmpty()) {
            showError("请输入卡密");
            return;
        }
        WeiYanApi.unbind(requireContext(), kami, (success, message) -> {
            if (!isAdded()) return;
            new MiuixAlertDialog(requireContext())
                    .setTitle(success ? "成功" : "失败")
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    private void showError(String message) {
        if (!isAdded()) return;
        new MiuixAlertDialog(requireContext())
                .setTitle("错误")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }
}
