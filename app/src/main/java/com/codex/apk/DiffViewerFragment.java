package com.codex.apk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DiffViewerFragment extends Fragment {

    private static final String ARG_FILE_NAME = "file_name";
    private static final String ARG_DIFF_CONTENT = "diff_content";

    private DiffViewModel mViewModel;
    private RecyclerView mRecyclerView;
    private InlineDiffAdapter mInlineAdapter;
    private SplitDiffAdapter mSplitAdapter;
    private TextView mFileNameTextView;
    private TextView mDiffSummaryTextView;
    private ToggleButton mToggleViewModeButton;

    public static DiffViewerFragment newInstance(String fileName, String diffContent) {
        DiffViewerFragment fragment = new DiffViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, fileName);
        args.putString(ARG_DIFF_CONTENT, diffContent);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(DiffViewModel.class);
        if (getArguments() != null) {
            String fileName = getArguments().getString(ARG_FILE_NAME);
            String diffContent = getArguments().getString(ARG_DIFF_CONTENT);
            mViewModel.setDiff(fileName, diffContent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diff_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.diff_recycler_view);
        mFileNameTextView = view.findViewById(R.id.diff_file_name);
        mDiffSummaryTextView = view.findViewById(R.id.diff_summary);
        mToggleViewModeButton = view.findViewById(R.id.toggle_view_mode);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mViewModel.getFileName().observe(getViewLifecycleOwner(), fileName -> mFileNameTextView.setText(fileName));

        mViewModel.getDiffLines().observe(getViewLifecycleOwner(), diffLines -> {
            if (diffLines != null) {
                mInlineAdapter = new InlineDiffAdapter(getContext(), diffLines);
                mSplitAdapter = new SplitDiffAdapter(getContext(), diffLines);
                updateAdapter();

                int[] counts = DiffUtils.countAddRemoveFromContents("", getArguments().getString(ARG_DIFF_CONTENT));
                mDiffSummaryTextView.setText(getString(R.string.diff_summary_format, counts[0], counts[1]));
            }
        });

        mViewModel.isSplitView().observe(getViewLifecycleOwner(), isSplit -> {
            mToggleViewModeButton.setChecked(isSplit);
            updateAdapter();
        });

        mToggleViewModeButton.setOnClickListener(v -> mViewModel.toggleView());
    }

    private void updateAdapter() {
        if (mViewModel.isSplitView().getValue() != null && mViewModel.isSplitView().getValue()) {
            mRecyclerView.setAdapter(mSplitAdapter);
        } else {
            mRecyclerView.setAdapter(mInlineAdapter);
        }
    }
}