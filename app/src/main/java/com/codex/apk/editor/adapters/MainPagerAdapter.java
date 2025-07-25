package com.codex.apk.editor.adapters;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.codex.apk.AIChatFragment;
import com.codex.apk.CodeEditorFragment;
import com.codex.apk.EditorActivity; // Need EditorActivity to set fragment references

public class MainPagerAdapter extends FragmentStateAdapter {
    private final EditorActivity activity; // Keep a reference to the activity

    public MainPagerAdapter(@NonNull AppCompatActivity fragmentActivity) {
        super(fragmentActivity);
        this.activity = (EditorActivity) fragmentActivity; // Cast to EditorActivity
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) { // Chat tab
            AIChatFragment aiChatFragment = AIChatFragment.newInstance(activity.getProjectPath()); // Pass projectPath
            activity.setAIChatFragment(aiChatFragment); // Set reference in activity
            return aiChatFragment;
        } else { // position == 1, Code tab
            CodeEditorFragment codeEditorFragment = CodeEditorFragment.newInstance();
            activity.setCodeEditorFragment(codeEditorFragment); // Set reference in activity
            activity.onCodeEditorFragmentReady();
            return codeEditorFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2; // We now have two main tabs: "Chat", "Code" (Preview is now separate activity)
    }
}
