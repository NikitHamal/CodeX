package com.codex.apk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.codex.apk.SimpleSoraTabAdapter;

import java.io.File;
import java.util.List;

public class CodeEditorFragment extends Fragment implements SimpleSoraTabAdapter.TabActionListener {

    private static final String TAG = "CodeEditorFragment";

    private ViewPager2 fileViewPager;
    private SimpleSoraTabAdapter tabAdapter;

    // New UI elements for indexing progress
    private LinearLayout layoutIndexingProgress;
    private ProgressBar progressBarIndexing;
    private TextView textIndexingStatus;

    // Listener to communicate with EditorActivity
    private CodeEditorFragmentListener listener;

    /**
     * Interface for actions related to code editing that need to be handled by the parent activity.
     * This interface is implemented by EditorActivity.
     */
    public interface CodeEditorFragmentListener {
        List<TabItem> getOpenTabsList();
        DialogHelper getDialogHelper();
        FileManager getFileManager();
        String getProjectPath();
        String getProjectName();
        void openFile(File file);
        void closeTab(int position, boolean confirmIfModified);
        void closeOtherTabs(int keepPosition);
        void closeAllTabs(boolean confirmIfModified);
        void saveFile(TabItem tabItem);
        void showTabOptionsMenu(View anchorView, int position);
        void onActiveTabContentChanged(String content, String fileName);
        void onActiveTabChanged(File newFile);
        void onIndexingStarted(int totalFiles);
        void onIndexingProgress(int indexedCount, int totalFiles, String currentFile);
        void onIndexingCompleted();
        void onIndexingError(String errorMessage);
    }

    /**
     * Factory method to create a new instance of this fragment.
     * @return A new instance of fragment CodeEditorFragment.
     */
    public static CodeEditorFragment newInstance() {
        return new CodeEditorFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the hosting activity implements the listener interface
        if (context instanceof CodeEditorFragmentListener) {
            listener = (CodeEditorFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement CodeEditorFragmentListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_code_editor_tab, container, false);

        fileViewPager = view.findViewById(R.id.file_view_pager);

        // Initialize new UI elements
        layoutIndexingProgress = view.findViewById(R.id.layout_indexing_progress_code_tab);
        progressBarIndexing = view.findViewById(R.id.progress_bar_indexing_code_tab);
        textIndexingStatus = view.findViewById(R.id.text_indexing_status_code_tab);

        setupFileTabsAndViewPager();

        // Initially hide the progress bar
        layoutIndexingProgress.setVisibility(View.GONE);

        return view;
    }

    /**
     * Sets up the file tabs and ViewPager2 for displaying code editor views.
     * This method retrieves the list of open tabs from the activity via the listener
     * and initializes the TabAdapter and TabLayoutMediator.
     */
    private void setupFileTabsAndViewPager() {
        if (listener == null) {
            Log.e(TAG, "Listener is null in setupFileTabsAndViewPager");
            return;
        }

        List<TabItem> openTabs = listener.getOpenTabsList();
        // Pass 'this' as TabActionListener so TabAdapter can call back to this fragment
        tabAdapter = new SimpleSoraTabAdapter(getContext(), openTabs, this, listener.getFileManager()); // 'this' refers to CodeEditorFragment

        fileViewPager.setAdapter(tabAdapter);

    }

    /**
     * Implementation of TabAdapter.TabActionListener.
     * Called by TabAdapter when a tab's modified state changes.
     * This triggers a refresh of the file tab layout to update icons.
     */
    @Override
    public void onTabModifiedStateChanged() {
        refreshFileTabLayout();
    }

    /**
     * Implementation of TabAdapter.TabActionListener.
     * Called by TabAdapter when the content of the active tab changes.
     * This method notifies the parent activity.
     * @param content The new content of the active file.
     * @param fileName The name of the active file.
     */
    @Override
    public void onActiveTabContentChanged(String content, String fileName) {
        if (listener != null) {
            listener.onActiveTabContentChanged(content, fileName);
        }
    }

    /**
     * Implementation of TabAdapter.TabActionListener.
     * Called by TabAdapter when the active tab changes (e.g., user switches tabs).
     * This method notifies the parent activity.
     * @param newFile The File object of the newly active tab.
     */
    @Override
    public void onActiveTabChanged(File newFile) {
        if (listener != null) {
            listener.onActiveTabChanged(newFile);
        }
    }

    /**
     * Refreshes the file tab layout. This method is called by the EditorActivity
     * when changes to the openTabs list occur (e.g., file saved, new file opened, file deleted).
     * It now simply notifies the adapter that its data set has changed.
     */
    public void refreshFileTabLayout() {
        if (fileViewPager == null || tabAdapter == null || listener == null) {
            Log.e(TAG, "refreshFileTabLayout: One or more UI components or listener are null.");
            return;
        }
        // Simply notify the adapter that data has changed.
        tabAdapter.notifyDataSetChanged();
    }

    /**
     * Notifies the TabAdapter that a new tab has been added.
     * @param tabItem The new tab item.
     */
    public void addFileTab(TabItem tabItem) {
        if (tabAdapter != null) {
            tabAdapter.notifyItemInserted(listener.getOpenTabsList().size() - 1);
        }
    }

    /**
     * Notifies the TabAdapter that a tab has been removed.
     * @param position The position of the removed tab.
     */
    public void removeFileTab(int position) {
        if (tabAdapter != null) {
            tabAdapter.notifyItemRemoved(position);
            tabAdapter.notifyItemRangeChanged(position, listener.getOpenTabsList().size());
        }
    }

    /**
     * Notifies the TabAdapter to refresh all its items.
     * Useful when the entire list of open tabs has changed significantly.
     */
    public void refreshAllFileTabs() {
        if (tabAdapter != null) {
            tabAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Sets the current item of the file ViewPager2.
     * @param position The position to set.
     * @param smoothScroll True for smooth scrolling, false otherwise.
     */
    public void setFileViewPagerCurrentItem(int position, boolean smoothScroll) {
        if (fileViewPager != null && position >= 0 && position < listener.getOpenTabsList().size()) {
            fileViewPager.setCurrentItem(position, smoothScroll);
        }
    }

    /**
     * Returns the SimpleSoraTabAdapter instance used by this fragment.
     * @return The SimpleSoraTabAdapter.
     */
    public SimpleSoraTabAdapter getFileTabAdapter() {
        return tabAdapter;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up resources held by the SimpleSoraTabAdapter when the view is destroyed
        if (tabAdapter != null) {
            tabAdapter.cleanup();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Clear the listener to prevent memory leaks
    }

    // --- Indexing progress methods for CodeEditorFragmentListener ---
    public void onIndexingStarted(int totalFiles) {
        if (layoutIndexingProgress != null) {
            layoutIndexingProgress.setVisibility(View.VISIBLE);
            progressBarIndexing.setMax(totalFiles);
            progressBarIndexing.setProgress(0);
            textIndexingStatus.setText("Indexing project (0/" + totalFiles + ")");
        }
    }

    public void onIndexingProgress(int indexedCount, int totalFiles, String currentFile) {
        if (layoutIndexingProgress != null) {
            layoutIndexingProgress.setVisibility(View.VISIBLE);
            progressBarIndexing.setMax(totalFiles);
            progressBarIndexing.setProgress(indexedCount);
            String statusText = "Indexing " + indexedCount + "/" + totalFiles;
            if (currentFile != null && !currentFile.isEmpty()) {
                statusText += " (" + currentFile + ")";
            }
            textIndexingStatus.setText(statusText);
        }
    }

    public void onIndexingCompleted() {
        if (layoutIndexingProgress != null) {
            layoutIndexingProgress.setVisibility(View.GONE);
        }
    }

    public void onIndexingError(String errorMessage) {
        if (layoutIndexingProgress != null) {
            layoutIndexingProgress.setVisibility(View.GONE);
            // Optionally show a temporary error message in the UI, or log it
            Log.e(TAG, "Indexing error: " + errorMessage);
        }
    }
}
