package com.codex.apk;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class DiffViewModel extends ViewModel {
    private final MutableLiveData<List<DiffUtils.DiffLine>> mDiffLines = new MutableLiveData<>();
    private final MutableLiveData<String> mFileName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsSplitView = new MutableLiveData<>(false);

    public void setDiff(String fileName, String diffText) {
        mFileName.setValue(fileName);
        mDiffLines.setValue(DiffUtils.parseUnifiedDiff(diffText));
    }

    public LiveData<List<DiffUtils.DiffLine>> getDiffLines() {
        return mDiffLines;
    }

    public LiveData<String> getFileName() {
        return mFileName;
    }

    public LiveData<Boolean> isSplitView() {
        return mIsSplitView;
    }

    public void toggleView() {
        mIsSplitView.setValue(Boolean.FALSE.equals(mIsSplitView.getValue()));
    }
}