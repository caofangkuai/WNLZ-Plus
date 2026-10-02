package com.wunelezi.plus.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.himiuix.MiuixCheckBoxView;
import com.hchen.himiuix.MiuixSwitchView;
import com.hchen.himiuix.MiuixViewGroup;
import com.hchen.himiuix.dialog.MiuixAlertDialog;
import com.hchen.himiuix.fragment.Fragment;
import com.wunelezi.plus.R;
import com.wunelezi.plus.core.ModuleInfo;
import com.wunelezi.plus.core.ModuleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XpFragment extends Fragment {

    private MiuixViewGroup xpList;
    private View selectionBar;
    private TextView selectionCount;
    private View btnDeleteSelected;
    private ModuleRepository.Kind kind = ModuleRepository.Kind.XP;

    private boolean selectionMode = false;
    private final Set<String> selected = new HashSet<>();

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            exitSelection();
        }
    };

    private final ActivityResultLauncher<String> importLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    importModule(result);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        xpList = view.findViewById(R.id.xp_list);
        selectionBar = view.findViewById(R.id.selection_bar);
        selectionCount = view.findViewById(R.id.selection_count);
        btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);
        View btnCancelSelection = view.findViewById(R.id.btn_cancel_selection);
        View btnImport = view.findViewById(R.id.btn_import_xp);

        btnImport.setOnClickListener(v -> importLauncher.launch(kind.mimeType()));
        btnCancelSelection.setOnClickListener(v -> exitSelection());
        btnDeleteSelected.setOnClickListener(v -> confirmDelete());

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);

        refreshList();
    }

    private void importModule(Uri uri) {
        final Context app = requireContext().getApplicationContext();
        new Thread(() -> {
            String error = ModuleRepository.importZip(app, kind, uri);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                refreshList();
                new MiuixAlertDialog(requireContext())
                        .setTitle(error == null ? "成功" : "导入失败")
                        .setMessage(error == null ? "导入完成" : "导入出错")
                        .setPositiveButton("确定", null)
                        .show();
            });
        }).start();
    }

    private void refreshList() {
        xpList.removeAllViews();
        List<ModuleInfo> modules = ModuleRepository.load(requireContext(), kind);
        for (ModuleInfo info : modules) {
            if (selectionMode) addSelectableRow(info);
            else addSwitchRow(info);
        }
        updateSelectionBar();
    }

    private void addSwitchRow(ModuleInfo info) {
        MiuixSwitchView item = new MiuixSwitchView(requireContext());
        item.setTitle(info.displayName());
        item.setSummaryOn("已启用");
        item.setSummaryOff("已禁用");
        item.setChecked(info.enabled);
        item.setOnStateChangeListener(newValue -> {
            info.enabled = newValue;
            final Context app = requireContext().getApplicationContext();
            new Thread(() -> ModuleRepository.setEnabled(app, kind, info, newValue)).start();
            return true;
        });
        item.setOnLongClickListener(v -> {
            enterSelection(info.zipName);
            return true;
        });
        xpList.addView(item);
    }

    private void addSelectableRow(ModuleInfo info) {
        MiuixCheckBoxView item = new MiuixCheckBoxView(requireContext());
        item.setTitle(info.displayName());
        item.setChecked(selected.contains(info.zipName));
        item.setOnStateChangeListener(newValue -> {
            if (newValue) selected.add(info.zipName);
            else selected.remove(info.zipName);
            updateSelectionBar();
            return true;
        });
        xpList.addView(item);
    }

    private void enterSelection(String zipName) {
        selectionMode = true;
        selected.clear();
        if (zipName != null) selected.add(zipName);
        backCallback.setEnabled(true);
        refreshList();
    }

    private void exitSelection() {
        selectionMode = false;
        selected.clear();
        backCallback.setEnabled(false);
        refreshList();
    }

    private void updateSelectionBar() {
        if (selectionMode) {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText("已选 " + selected.size() + " 项");
            boolean hasSelection = !selected.isEmpty();
            btnDeleteSelected.setEnabled(hasSelection);
            btnDeleteSelected.setAlpha(hasSelection ? 1f : 0.5f);
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }

    private void confirmDelete() {
        if (selected.isEmpty()) return;
        new MiuixAlertDialog(requireContext())
                .setTitle("删除")
                .setMessage("确认删除选中的 " + selected.size() + " 个XP模块？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> deleteSelected())
                .show();
    }

    private void deleteSelected() {
        final Set<String> targets = new HashSet<>(selected);
        final Context app = requireContext().getApplicationContext();
        new Thread(() -> {
            ModuleRepository.deleteAll(app, kind, targets);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                selectionMode = false;
                selected.clear();
                backCallback.setEnabled(false);
                refreshList();
            });
        }).start();
    }
}
