package com.codex.apk.editor;

import androidx.lifecycle.ViewModel;
import com.codex.apk.FileItem;
import com.codex.apk.TabItem;
import java.util.ArrayList;
import java.util.List;

public class EditorViewModel extends ViewModel {
    private List<FileItem> fileItems = new ArrayList<>();
    private List<TabItem> openTabs = new ArrayList<>();

    public List<FileItem> getFileItems() {
        return fileItems;
    }

    public void setFileItems(List<FileItem> fileItems) {
        this.fileItems = fileItems;
    }

    public List<TabItem> getOpenTabs() {
        return openTabs;
    }

    public void setOpenTabs(List<TabItem> openTabs) {
        this.openTabs = openTabs;
    }
}
