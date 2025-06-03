/**
 * MonacoEditorManager - Handles Monaco editor functionality
 */
export class MonacoEditorManager {
    constructor(editor, fileManager) {
        this.editor = editor;
        this.fileManager = fileManager;
        this.monacoInstance = null;
        this.editors = {}; // Stores normal editor instances
        this.diffEditors = {}; // Stores diff editor instances
        
        this.initMonacoEditor();
    }
    
    /**
     * Initialize Monaco Editor
     */
    initMonacoEditor() {
        // Configure Monaco loader
        require.config({
            paths: {
                'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.36.1/min/vs'
            }
        });
        
        // Load Monaco Editor
        require(['vs/editor/editor.main'], () => {
            // Set editor theme
            monaco.editor.defineTheme('codex-dark', {
                base: 'vs-dark',
                inherit: true,
                rules: [],
                colors: {
                    'editor.background': '#121212',
                    'editor.lineHighlightBackground': '#1e1e1e',
                    'editorLineNumber.foreground': '#6e6e6e',
                    'editorLineNumber.activeForeground': '#ffffff',
                    // Diff editor colors (standard Monaco names)
                    'diffEditor.insertedTextBackground': '#28a74533', // Green with transparency
                    'diffEditor.removedTextBackground': '#dc354533', // Red with transparency
                    // 'diffEditor.insertedLineBackground': '#28a74522', // Optional: for the whole line
                    // 'diffEditor.removedLineBackground': '#dc354522', // Optional: for the whole line
                }
            });
            
            monaco.editor.setTheme('codex-dark');
            
            this.monacoInstance = monaco;
            
            // Load files if any
            this.editor.loadFiles();
        });
    }
    
    /**
     * Create tab for a file or a diff view
     */
    createTab(file, isDiff = false, tabTitlePrefix = '') {
        const tabId = isDiff ? `diff-${file.id}` : file.id;
        const existingTab = document.querySelector(`.tab[data-id="${tabId}"]`);
        if (existingTab) return; // Tab already exists

        const tab = document.createElement('div');
        tab.className = 'tab';
        if (isDiff) {
            tab.classList.add('diff-view-tab');
        }
        tab.dataset.id = tabId;
        
        const icon = document.createElement('span');
        icon.className = 'material-icons tab-icon';
        icon.textContent = isDiff ? 'compare_arrows' : 'description'; 
        
        if (!isDiff) {
            const fileType = this.getFileType(file.name);
            if (fileType) {
                icon.classList.add(`file-icon-${fileType}`);
            }
        }
        
        const title = document.createElement('span');
        title.className = 'tab-title';
        title.textContent = `${tabTitlePrefix}${file.name}`;
        
        const closeBtn = document.createElement('span');
        closeBtn.className = 'material-icons tab-close';
        closeBtn.textContent = 'close';
        
        tab.appendChild(icon);
        tab.appendChild(title);
        tab.appendChild(closeBtn);
        
        tab.addEventListener('click', () => {
            this.activateTab(tabId);
            if (isDiff) {
                this.showDiffEditor(tabId);
            } else {
                this.editor.openFile(file); // openFile handles showing normal editor
            }
        });
        
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.closeTab(tabId, isDiff);
        });
        
        const tabsContainer = document.getElementById('tabs');
        tabsContainer.appendChild(tab);
    }
    
    /**
     * Activate a tab (normal or diff)
     */
    activateTab(tabId) {
        document.querySelectorAll('.tab').forEach(tab => {
            tab.classList.remove('active');
        });
        const tab = document.querySelector(`.tab[data-id="${tabId}"]`);
        if (tab) {
            tab.classList.add('active');
        }
    }
    
    /**
     * Close a tab (normal or diff)
     */
    closeTab(tabId, isDiff = false) {
        const tab = document.querySelector(`.tab[data-id="${tabId}"]`);
        if (tab) {
            tab.remove();
        }
        
        const editorContainerId = isDiff ? `diff-editor-${tabId}` : `editor-${tabId}`;
        const editorContainer = document.getElementById(editorContainerId);
        if (editorContainer) {
            editorContainer.remove();
        }
        
        if (isDiff) {
            if (this.diffEditors[tabId]) {
                this.diffEditors[tabId].dispose();
                // Dispose models associated with the diff editor
                const models = this.diffEditors[tabId].getModel();
                if (models && models.original) models.original.dispose();
                if (models && models.modified) models.modified.dispose();
                delete this.diffEditors[tabId];
            }
        } else {
            if (this.editors[tabId]) {
                this.editors[tabId].dispose();
                delete this.editors[tabId];
            }
        }
        
        // If current file/diff view is closed, open another file or clear view
        const activeFileId = isDiff ? tabId.replace('diff-','') : tabId;
        if (this.editor.currentFile && this.editor.currentFile.id === activeFileId || 
            document.querySelector(`.tab.active[data-id="${tabId}"]`)) { // Check if the closed tab was active
            
            const remainingTabs = document.querySelectorAll('.tab');
            if (remainingTabs.length > 0) {
                const nextTabToActivate = remainingTabs[0];
                const nextTabId = nextTabToActivate.dataset.id;
                this.activateTab(nextTabId);

                if (nextTabToActivate.classList.contains('diff-view-tab')) {
                    this.showDiffEditor(nextTabId);
                } else {
                    const nextFile = this.fileManager.getFile(nextTabId);
                    if (nextFile) {
                        this.editor.openFile(nextFile);
                    }
                }
            } else {
                this.editor.currentFile = null;
                // Potentially clear editor content area or show a placeholder
                 document.getElementById('editorContent').innerHTML = ''; 
            }
        }
    }
    
    /**
     * Create Monaco editor for a file
     */
    createEditor(file) {
        const editorContainerId = `editor-${file.id}`;
        let editorContainer = document.getElementById(editorContainerId);
        if (!editorContainer) {
            editorContainer = document.createElement('div');
            editorContainer.id = editorContainerId;
            editorContainer.className = 'monaco-editor-instance';
            editorContainer.style.width = '100%';
            editorContainer.style.height = '100%';
            document.getElementById('editorContent').appendChild(editorContainer);
        }
        
        this.fileManager.getFileContent(file.id)
            .then(content => {
                const editor = this.monacoInstance.editor.create(editorContainer, {
                    value: content,
                    language: this.getLanguageForFile(file.name),
                    theme: 'codex-dark',
                    automaticLayout: true,
                    minimap: { enabled: false },
                    scrollBeyondLastLine: false,
                    fontSize: 14,
                    tabSize: 4,
                    insertSpaces: true,
                    wordWrap: 'on'
                });
                this.editors[file.id] = editor;
                editor.onDidChangeModelContent(() => {
                    const currentContent = editor.getValue();
                    this.saveFileContent(file.id, currentContent);
                    if (this.editor.autoRefreshPreview) {
                        this.editor.refreshPreview();
                    }
                });
                editor.onDidChangeCursorPosition(() => {
                    const position = editor.getPosition();
                    document.getElementById('currentPosition').textContent = `Ln ${position.lineNumber}, Col ${position.column}`;
                });
                this.showEditor(file.id);
            });
    }
    
    /**
     * Get language ID for Monaco Editor based on file extension
     */
    getLanguageForFile(fileName) {
        const extension = fileName.split('.').pop().toLowerCase();
        
        switch (extension) {
            case 'html':
                return 'html';
            case 'css':
                return 'css';
            case 'js':
                return 'javascript';
            case 'json':
                return 'json';
            case 'md':
                return 'markdown';
            case 'txt':
                return 'plaintext';
            default:
                return 'plaintext';
        }
    }
    
    /**
     * Get file type for icon coloring
     */
    getFileType(fileName) {
        const extension = fileName.split('.').pop().toLowerCase();
        
        switch (extension) {
            case 'html':
                return 'html';
            case 'css':
                return 'css';
            case 'js':
            case 'jsx':
            case 'ts':
            case 'tsx':
                return 'js';
            case 'json':
                return 'json';
            case 'md':
                return 'md';
            case 'jpg':
            case 'jpeg':
            case 'png':
            case 'gif':
            case 'svg':
                return 'image';
            default:
                return null;
        }
    }
    
    /**
     * Save file content
     */
    saveFileContent(fileId, content) {
        this.fileManager.updateFileContent(fileId, content);
    }
    
    /**
     * Update status bar information
     */
    updateStatusBar(file) {
        const extension = file.name.split('.').pop().toLowerCase();
        document.getElementById('fileType').textContent = extension.toUpperCase();
    }
    
    /**
     * Resize all editors when window size changes
     */
    resizeEditors() {
        for (const id in this.editors) {
            if (this.editors[id] && document.getElementById(`editor-${id}`).style.display !== 'none') {
                this.editors[id].layout();
            }
        }
        for (const id in this.diffEditors) {
            if (this.diffEditors[id] && document.getElementById(`diff-editor-${id}`).style.display !== 'none') {
                this.diffEditors[id].layout();
            }
        }
    }
    
    /**
     * Show editor for a specific file
     */
    showEditor(fileId) {
        // Hide all other editors (normal and diff)
        document.querySelectorAll('.monaco-editor-instance').forEach(el => el.style.display = 'none');
        // Hide special content like chat/preview if they are in editorContent
        document.querySelectorAll('#editorContent > .chat-container, #editorContent > .preview-container').forEach(el => el.style.display = 'none');

        const editorContainer = document.getElementById(`editor-${fileId}`);
        if (editorContainer) {
            editorContainer.style.display = 'block';
            if (this.editors[fileId]) {
                this.editors[fileId].layout();
                 // Update status bar for normal files
                const file = this.fileManager.getFile(fileId);
                if(file) this.updateStatusBar(file);
                document.querySelector('.status-bar').style.display = 'flex';
            }
        }
    }

    showDiffEditor(diffTabId) {
        // Hide all other editors (normal and diff)
        document.querySelectorAll('.monaco-editor-instance').forEach(el => el.style.display = 'none');
        // Hide special content like chat/preview if they are in editorContent
        document.querySelectorAll('#editorContent > .chat-container, #editorContent > .preview-container').forEach(el => el.style.display = 'none');

        const diffEditorContainer = document.getElementById(`diff-editor-${diffTabId}`);
        if (diffEditorContainer) {
            diffEditorContainer.style.display = 'block';
            if (this.diffEditors[diffTabId]) {
                this.diffEditors[diffTabId].layout();
                 // Hide status bar for diff views or show specific info
                document.querySelector('.status-bar').style.display = 'none';
            }
        }
    }

    /**
     * Open a Diff View
     */
    openDiffView(fileId, filePath, originalContent, modifiedContent, tabTitlePrefix = 'Diff: ') {
        if (!this.monacoInstance) {
            console.error("Monaco instance not available for diff view.");
            return;
        }

        const diffTabId = `diff-${fileId}`;
        const file = { id: fileId, name: filePath.split('/').pop() }; // Create a mock file object for createTab

        this.createTab(file, true, tabTitlePrefix);
        this.activateTab(diffTabId);

        let diffEditorContainer = document.getElementById(`diff-editor-${diffTabId}`);
        if (!diffEditorContainer) {
            diffEditorContainer = document.createElement('div');
            diffEditorContainer.id = `diff-editor-${diffTabId}`;
            diffEditorContainer.className = 'monaco-editor-instance diff-editor-instance'; // Add specific class for diff
            diffEditorContainer.style.width = '100%';
            diffEditorContainer.style.height = '100%';
            document.getElementById('editorContent').appendChild(diffEditorContainer);
        }

        const originalModel = this.monacoInstance.editor.createModel(originalContent, this.getLanguageForFile(file.name));
        const modifiedModel = this.monacoInstance.editor.createModel(modifiedContent, this.getLanguageForFile(file.name));

        // Dispose existing diff editor if any for this tabId to prevent conflicts
        if (this.diffEditors[diffTabId]) {
            this.diffEditors[diffTabId].dispose();
            const oldModels = this.diffEditors[diffTabId].getModel();
            if (oldModels && oldModels.original) oldModels.original.dispose();
            if (oldModels && oldModels.modified) oldModels.modified.dispose();
        }

        const diffEditor = this.monacoInstance.editor.createDiffEditor(diffEditorContainer, {
            theme: 'codex-dark',
            automaticLayout: true,
            readOnly: false, // Main editor is not read-only, original pane is by default
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 14,
            wordWrap: 'on',
            renderSideBySide: true, // Default, can be toggled
            originalEditable: false // Make the original editor (left side) read-only
        });

        diffEditor.setModel({
            original: originalModel,
            modified: modifiedModel
        });

        this.diffEditors[diffTabId] = diffEditor;
        this.showDiffEditor(diffTabId);
        this.editor.currentFile = null; // Clear current normal file when diff view is active
    }
} 