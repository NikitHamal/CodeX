// Import modules
import { ProjectManager } from './projectManager.js';
import { FileManager } from './fileManager.js';
import { EditorUI } from './editorUI.js';
import { FileExplorer } from './fileExplorer.js';
import { MonacoEditorManager } from './monacoEditor.js';
import { PreviewManager } from './previewManager.js';
import { CodebaseIndexer } from './codebaseIndexer.js';
import { AIAssistant } from './aiAssistant.js';

/**
 * EditorManager - Main editor class that coordinates all editor functionality
 */
export class EditorManager {
    constructor() {
        // Get project ID from session storage
        this.projectId = sessionStorage.getItem('currentProjectId');
        if (!this.projectId) {
            window.location.href = 'index.html';
            return;
        }
        
        // Initialize managers
        this.projectManager = new ProjectManager();
        this.fileManager = new FileManager(this.projectId, this.projectManager);
        
        // Current state
        this.currentFile = null;
        this.currentFolderPath = '/';
        this.clipboardItem = null;
        this.clipboardOperation = null; // 'copy' or 'cut'
        this.autoRefreshPreview = true;
        
        // Initialize UI components
        this.ui = new EditorUI(this);
        this.fileExplorer = new FileExplorer(this, this.fileManager);
        this.monacoEditor = new MonacoEditorManager(this, this.fileManager);
        this.previewManager = new PreviewManager(this, this.fileManager);
        
        // Initialize AI components
        this.codebaseIndexer = new CodebaseIndexer(this.fileManager);
        this.aiAssistant = new AIAssistant(this, this.codebaseIndexer);
        
        // Load project
        this.loadProject();
        
        // Index codebase for AI
        this.indexCodebase();
    }
    
    /**
     * Load project data
     */
    loadProject() {
        const project = this.projectManager.getProject(this.projectId);
        if (!project) {
            window.location.href = 'index.html';
            return;
        }
        
        // Initialize folders if needed
        this.fileManager.initFolders();
        
        // Update project name in the UI
        document.getElementById('projectTitle').textContent = project.name;
        document.title = `${project.name} - CodeX`;
    }
    
    /**
     * Load project files and folders
     */
    loadFiles() {
        this.fileExplorer.loadFiles();
    }
    
    /**
     * Open a file in the editor
     */
    openFile(file) {
        // Switch to code tab on mobile
        const codeTab = document.querySelector('.bottom-nav-item[data-tab="code"]');
        if (codeTab && !codeTab.classList.contains('active')) {
            this.ui.switchMobileTab('code', codeTab);
        }

        // Create tab if it doesn't exist
        if (!document.querySelector(`.tab[data-id="${file.id}"]`)) {
            this.monacoEditor.createTab(file);
        }
        
        // Activate tab
        this.monacoEditor.activateTab(file.id);
        
        // Create editor if it doesn't exist
        if (!this.monacoEditor.editors[file.id]) {
            this.monacoEditor.createEditor(file);
        } else {
            // Show editor
            this.monacoEditor.showEditor(file.id);
        }
        
        // Update current file
        this.currentFile = file;
        
        // Update status bar
        this.monacoEditor.updateStatusBar(file);
        
        // Close sidebar on mobile
        this.ui.closeSidebar();
    }
    
    /**
     * Show new file modal
     */
    showNewFileModal() {
        document.getElementById('overlay').style.display = 'block';
        document.getElementById('newFileModal').style.display = 'block';
        document.getElementById('fileName').focus();
        
        // Set current folder as default
        const folderPathSelect = document.getElementById('folderPath');
        if (this.currentFolderPath && folderPathSelect) {
            for (let i = 0; i < folderPathSelect.options.length; i++) {
                if (folderPathSelect.options[i].value === this.currentFolderPath) {
                    folderPathSelect.selectedIndex = i;
                    break;
                }
            }
        }
    }
    
    /**
     * Show new folder modal
     */
    showNewFolderModal() {
        document.getElementById('overlay').style.display = 'block';
        document.getElementById('newFolderModal').style.display = 'block';
        document.getElementById('folderName').focus();
        
        // Set current folder as default parent
        const parentFolderPathSelect = document.getElementById('parentFolderPath');
        if (this.currentFolderPath && parentFolderPathSelect) {
            for (let i = 0; i < parentFolderPathSelect.options.length; i++) {
                if (parentFolderPathSelect.options[i].value === this.currentFolderPath) {
                    parentFolderPathSelect.selectedIndex = i;
                    break;
                }
            }
        }
    }
    
    /**
     * Close all modals
     */
    closeAllModals() {
        document.getElementById('overlay').style.display = 'none';
        document.getElementById('newFileModal').style.display = 'none';
        document.getElementById('newFolderModal').style.display = 'none';
        document.getElementById('renameModal').style.display = 'none';
        document.getElementById('modelSelectionModal').style.display = 'none';
        document.getElementById('fileName').value = '';
        document.getElementById('folderName').value = '';
        document.getElementById('newName').value = '';
    }
    
    /**
     * Create a new file
     */
    createNewFile() {
        const fileName = document.getElementById('fileName').value.trim();
        const folderPath = document.getElementById('folderPath').value;
        
        if (fileName) {
            try {
                const newFile = this.fileManager.createFile(fileName, folderPath);
                this.loadFiles();
                this.openFile(newFile);
                this.closeAllModals();
            } catch (error) {
                alert(error.message);
            }
        }
    }
    
    /**
     * Create a new folder
     */
    createNewFolder() {
        const folderName = document.getElementById('folderName').value.trim();
        const parentPath = document.getElementById('parentFolderPath').value;
        
        if (folderName) {
            try {
                this.fileManager.createFolder(folderName, parentPath);
                this.loadFiles();
                this.closeAllModals();
            } catch (error) {
                alert(error.message);
            }
        }
    }
    
    /**
     * Show context menu
     */
    showContextMenu(event, itemType, item) {
        this.contextItem = this.fileExplorer.showContextMenu(event, itemType, item);
    }
    
    /**
     * Hide context menu
     */
    hideContextMenu() {
        this.fileExplorer.hideContextMenu();
    }
    
    /**
     * Show rename modal
     */
    showRenameModal() {
        if (!this.contextItem) return;
        
        this.hideContextMenu();
        
        const item = this.contextItem;
        const title = item.type === 'file' ? 'Rename File' : 'Rename Folder';
        const currentName = item.type === 'file' ? item.data.name : item.data.name;
        
        document.getElementById('renameModalTitle').textContent = title;
        document.getElementById('newName').value = currentName;
        
        document.getElementById('overlay').style.display = 'block';
        document.getElementById('renameModal').style.display = 'block';
        document.getElementById('newName').focus();
    }
    
    /**
     * Rename the selected item
     */
    renameItem() {
        if (!this.contextItem) return;
        
        const newName = document.getElementById('newName').value.trim();
        
        if (!newName) {
            alert('Name cannot be empty');
            return;
        }
        
        try {
            if (this.contextItem.type === 'file') {
                this.fileManager.renameFile(this.contextItem.data.id, newName);
            } else {
                this.fileManager.renameFolder(this.contextItem.data.path, newName);
            }
            
            this.loadFiles();
            this.closeAllModals();
        } catch (error) {
            alert(error.message);
        }
    }
    
    /**
     * Copy the selected item to clipboard
     */
    copyItem() {
        if (!this.contextItem) return;
        
        this.clipboardItem = this.contextItem;
        this.clipboardOperation = 'copy';
        this.hideContextMenu();
    }
    
    /**
     * Cut the selected item to clipboard
     */
    cutItem() {
        if (!this.contextItem) return;
        
        this.clipboardItem = this.contextItem;
        this.clipboardOperation = 'cut';
        this.hideContextMenu();
    }
    
    /**
     * Paste the item from clipboard
     */
    pasteItem() {
        if (!this.contextItem || !this.clipboardItem || this.contextItem.type !== 'folder') {
            this.hideContextMenu();
            return;
        }
        
        const targetPath = this.contextItem.data.path;
        
        try {
            if (this.clipboardItem.type === 'file') {
                const file = this.clipboardItem.data;
                const fileName = file.name;
                
                // Create a copy of the file in the new location
                const newFile = this.fileManager.createFile(fileName, targetPath);
                this.fileManager.updateFileContent(newFile.id, file.content);
                
                // If it was cut, delete the original
                if (this.clipboardOperation === 'cut') {
                    this.fileManager.deleteFile(file.id);
                    this.clipboardItem = null;
                }
            } else {
                const folder = this.clipboardItem.data;
                
                // Copy the folder and its contents
                this.fileManager.copyFolder(folder.path, targetPath);
                
                // If it was cut, delete the original
                if (this.clipboardOperation === 'cut') {
                    this.fileManager.deleteFolder(folder.path);
                    this.clipboardItem = null;
                }
            }
            
            this.loadFiles();
        } catch (error) {
            alert(error.message);
        }
        
        this.hideContextMenu();
    }
    
    /**
     * Delete the selected item
     */
    deleteItem() {
        if (!this.contextItem) return;
        
        const confirmMessage = this.contextItem.type === 'file' 
            ? `Are you sure you want to delete the file "${this.contextItem.data.name}"?`
            : `Are you sure you want to delete the folder "${this.contextItem.data.name}" and all its contents?`;
        
        if (confirm(confirmMessage)) {
            try {
                if (this.contextItem.type === 'file') {
                    this.fileManager.deleteFile(this.contextItem.data.id);
                    
                    // Close tab if open
                    this.monacoEditor.closeTab(this.contextItem.data.id);
                } else {
                    this.fileManager.deleteFolder(this.contextItem.data.path);
                    
                    // Close tabs of files in this folder
                    const files = this.fileManager.getFiles();
                    files.forEach(file => {
                        if (file.folderPath.startsWith(this.contextItem.data.path)) {
                            this.monacoEditor.closeTab(file.id);
                        }
                    });
                }
                
                this.loadFiles();
            } catch (error) {
                alert(error.message);
            }
        }
        
        this.hideContextMenu();
    }
    
    /**
     * Resize all editors
     */
    resizeEditors() {
        this.monacoEditor.resizeEditors();
    }
    
    /**
     * Refresh preview
     */
    refreshPreview() {
        this.previewManager.refreshPreview();
    }
    
    /**
     * Send chat message
     */
    sendChatMessage() {
        const chatInput = document.getElementById('chatInput');
        const message = chatInput.value.trim();
        
        if (message) {
            // Clear input
            chatInput.value = '';
            
            // Process message with AI assistant
            this.aiAssistant.processMessage(message);
        }
    }
    
    /**
     * Index the codebase for AI context
     */
    async indexCodebase() {
        try {
            await this.codebaseIndexer.indexCodebase();
            console.log('Codebase indexed successfully');
        } catch (error) {
            console.error('Error indexing codebase:', error);
        }
    }
}

// Add CSS for file type icons
document.addEventListener('DOMContentLoaded', () => {
    const style = document.createElement('style');
    style.textContent = `
        .file-icon-html { color: #e44d26; }
        .file-icon-css { color: #264de4; }
        .file-icon-js { color: #f7df1e; }
        .file-icon-json { color: #5498d7; }
        .file-icon-md { color: #03a9f4; }
        .file-icon-image { color: #4caf50; }
        
        .explorer-content {
            padding: 8px;
            overflow-y: auto;
            flex-grow: 1;
        }
    `;
    document.head.appendChild(style);
    
    // Initialize the editor
    new EditorManager();
});