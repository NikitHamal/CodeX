/**
 * MonacoEditorManager - Handles Monaco editor functionality
 */
export class MonacoEditorManager {
    constructor(editor, fileManager) {
        this.editor = editor;
        this.fileManager = fileManager;
        this.monacoInstance = null;
        this.editors = {};
        
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
                    'editorLineNumber.activeForeground': '#ffffff'
                }
            });
            
            monaco.editor.setTheme('codex-dark');
            
            this.monacoInstance = monaco;
            
            // Load files if any
            this.editor.loadFiles();
        });
    }
    
    /**
     * Create tab for a file
     */
    createTab(file) {
        const tab = document.createElement('div');
        tab.className = 'tab';
        tab.dataset.id = file.id;
        
        const icon = document.createElement('span');
        icon.className = 'material-icons tab-icon';
        icon.textContent = 'description'; // Use generic file icon
        
        // Add color class based on file type
        const fileType = this.getFileType(file.name);
        if (fileType) {
            icon.classList.add(`file-icon-${fileType}`);
        }
        
        const title = document.createElement('span');
        title.className = 'tab-title';
        title.textContent = file.name;
        
        const closeBtn = document.createElement('span');
        closeBtn.className = 'material-icons tab-close';
        closeBtn.textContent = 'close';
        
        tab.appendChild(icon);
        tab.appendChild(title);
        tab.appendChild(closeBtn);
        
        // Add click events
        tab.addEventListener('click', () => {
            this.activateTab(file.id);
            this.editor.openFile(file);
        });
        
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.closeTab(file.id);
        });
        
        const tabsContainer = document.getElementById('tabs');
        tabsContainer.appendChild(tab);
    }
    
    /**
     * Activate a tab
     */
    activateTab(fileId) {
        // Deactivate all tabs
        document.querySelectorAll('.tab').forEach(tab => {
            tab.classList.remove('active');
        });
        
        // Activate selected tab
        const tab = document.querySelector(`.tab[data-id="${fileId}"]`);
        if (tab) {
            tab.classList.add('active');
        }
    }
    
    /**
     * Close a tab
     */
    closeTab(fileId) {
        // Remove tab
        const tab = document.querySelector(`.tab[data-id="${fileId}"]`);
        if (tab) {
            tab.remove();
        }
        
        // Remove editor
        const editorContainer = document.getElementById(`editor-${fileId}`);
        if (editorContainer) {
            editorContainer.remove();
        }
        
        // Clean up
        if (this.editors[fileId]) {
            this.editors[fileId].dispose();
            delete this.editors[fileId];
        }
        
        // If current file is closed, open another file
        if (this.editor.currentFile && this.editor.currentFile.id === fileId) {
            const remainingTabs = document.querySelectorAll('.tab');
            if (remainingTabs.length > 0) {
                const nextFileId = remainingTabs[0].dataset.id;
                const nextFile = this.fileManager.getFile(nextFileId);
                if (nextFile) {
                    this.editor.openFile(nextFile);
                } else {
                    this.editor.currentFile = null;
                }
            } else {
                this.editor.currentFile = null;
            }
        }
    }
    
    /**
     * Create Monaco editor for a file
     */
    createEditor(file) {
        // Create editor container
        const editorContainer = document.createElement('div');
        editorContainer.id = `editor-${file.id}`;
        editorContainer.className = 'monaco-editor-instance';
        editorContainer.style.width = '100%';
        editorContainer.style.height = '100%';
        
        const editorContent = document.getElementById('editorContent');
        editorContent.appendChild(editorContainer);
        
        // Get file content
        this.fileManager.getFileContent(file.id)
            .then(content => {
                // Create Monaco editor
                const editor = monaco.editor.create(editorContainer, {
                    value: content,
                    language: this.getLanguageForFile(file.name),
                    theme: 'codex-dark',
                    automaticLayout: true,
                    minimap: {
                        enabled: false
                    },
                    scrollBeyondLastLine: false,
                    fontSize: 14,
                    tabSize: 4,
                    insertSpaces: true,
                    wordWrap: 'on'
                });
                
                // Store editor instance
                this.editors[file.id] = editor;
                
                // Set up change event to save file
                editor.onDidChangeModelContent((e) => {
                    const content = editor.getValue();
                    this.saveFileContent(file.id, content);
                    
                    // Auto-refresh preview if enabled
                    if (this.editor.autoRefreshPreview) {
                        this.editor.refreshPreview();
                    }
                });
                
                // Set up cursor position change event
                editor.onDidChangeCursorPosition((e) => {
                    const position = editor.getPosition();
                    document.getElementById('currentPosition').textContent = `Ln ${position.lineNumber}, Col ${position.column}`;
                });
                
                // Show editor
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
        Object.values(this.editors).forEach(editor => {
            editor.layout();
        });
    }
    
    /**
     * Show editor for a specific file
     */
    showEditor(fileId) {
        // Hide all editors
        Object.keys(this.editors).forEach(id => {
            const editorContainer = document.getElementById(`editor-${id}`);
            if (editorContainer) {
                editorContainer.style.display = 'none';
            }
        });
        
        // Show the status bar when showing code editors
        const statusBar = document.querySelector('.status-bar');
        if (statusBar) statusBar.style.display = 'flex';
        
        // Show the requested editor
        const editorContainer = document.getElementById(`editor-${fileId}`);
        if (editorContainer) {
            editorContainer.style.display = 'block';
        }
    }
} 