/**
 * FileExplorer - Handles the file explorer functionality
 */
export class FileExplorer {
    constructor(editor, fileManager) {
        this.editor = editor;
        this.fileManager = fileManager;
        this.currentFolderPath = '/';
        this.contextItem = null;
    }
    
    /**
     * Load project files and folders
     */
    loadFiles() {
        // Clear explorer content
        const explorerContent = document.createElement('div');
        explorerContent.className = 'explorer-content';
        
        // Load folders
        const folders = this.fileManager.getFolders();
        const rootFolders = folders.filter(folder => folder.parentPath === '/');
        
        // Load root files
        const rootFiles = this.fileManager.getFilesInFolder('/');
        
        // Add folders first, then files
        rootFolders.forEach(folder => {
            if (folder.path !== '/') {
                const folderElement = this.createFolderElement(folder);
                explorerContent.appendChild(folderElement);
            }
        });
        
        rootFiles.forEach(file => {
            const fileElement = this.createFileElement(file);
            explorerContent.appendChild(fileElement);
        });
        
        // Replace the explorer content
        const explorerContainer = document.getElementById('explorer-panel');
        const oldContent = explorerContainer.querySelector('.explorer-content');
        if (oldContent) {
            explorerContainer.replaceChild(explorerContent, oldContent);
        } else {
            // Find the panel header
            const panelHeader = explorerContainer.querySelector('.panel-header');
            if (panelHeader) {
                // Insert after the panel header
                panelHeader.insertAdjacentElement('afterend', explorerContent);
            } else {
                // Fallback: append to the container
                explorerContainer.appendChild(explorerContent);
            }
        }
        
        // Update folder selects in modals
        this.updateFolderSelects();
        
        // Open first file if no file is open
        if (rootFiles.length > 0 && !this.editor.currentFile) {
            this.editor.openFile(rootFiles[0]);
        }
    }
    
    /**
     * Update folder selects in modals
     */
    updateFolderSelects() {
        const folders = this.fileManager.getFolders();
        const folderPathSelect = document.getElementById('folderPath');
        const parentFolderPathSelect = document.getElementById('parentFolderPath');
        
        // Clear existing options except root
        while (folderPathSelect.options.length > 1) {
            folderPathSelect.remove(1);
        }
        
        while (parentFolderPathSelect.options.length > 1) {
            parentFolderPathSelect.remove(1);
        }
        
        // Add folder options
        folders.forEach(folder => {
            if (folder.path !== '/') {
                const option1 = document.createElement('option');
                option1.value = folder.path;
                option1.textContent = folder.path;
                folderPathSelect.appendChild(option1);
                
                const option2 = document.createElement('option');
                option2.value = folder.path;
                option2.textContent = folder.path;
                parentFolderPathSelect.appendChild(option2);
            }
        });
    }
    
    /**
     * Create folder element for the file tree
     */
    createFolderElement(folder) {
        const folderItem = document.createElement('div');
        folderItem.className = 'tree-item';
        folderItem.dataset.path = folder.path;
        folderItem.dataset.type = 'folder';
        
        const folderHeader = document.createElement('div');
        folderHeader.className = 'tree-item-header';
        
        const icon = document.createElement('span');
        icon.className = 'material-icons folder-icon';
        icon.textContent = 'folder';
        
        const folderName = document.createElement('span');
        folderName.className = 'tree-item-name';
        folderName.textContent = folder.name;
        
        const moreIcon = document.createElement('span');
        moreIcon.className = 'material-icons more-icon';
        moreIcon.textContent = 'more_vert';
        moreIcon.title = 'Options';
        
        folderHeader.appendChild(icon);
        folderHeader.appendChild(folderName);
        folderHeader.appendChild(moreIcon);
        folderItem.appendChild(folderHeader);
        
        // Add children container
        const childrenContainer = document.createElement('div');
        childrenContainer.className = 'tree-item-children';
        folderItem.appendChild(childrenContainer);
        
        // Add click event to toggle folder
        folderHeader.addEventListener('click', (e) => {
            // Prevent opening folder when clicking more icon
            if (e.target === moreIcon || e.target.closest('.more-icon')) {
                return;
            }
            
            if (folderItem.classList.contains('expanded')) {
                folderItem.classList.remove('expanded');
                icon.textContent = 'folder';
            } else {
                folderItem.classList.add('expanded');
                icon.textContent = 'folder_open';
                this.loadFolderContents(folder.path, childrenContainer);
            }
        });
        
        // Add context menu event
        folderHeader.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            this.editor.showContextMenu(e, 'folder', folder);
        });
        
        // Add more icon click event
        moreIcon.addEventListener('click', (e) => {
            e.stopPropagation();
            this.editor.showContextMenu(e, 'folder', folder);
        });
        
        return folderItem;
    }
    
    /**
     * Load folder contents
     */
    loadFolderContents(folderPath, container) {
        // Clear container
        container.innerHTML = '';
        
        // Get subfolders and files
        const subfolders = this.fileManager.getSubfolders(folderPath);
        const files = this.fileManager.getFilesInFolder(folderPath + '/');
        
        // Add subfolders first
        subfolders.forEach(subfolder => {
            const folderElement = this.createFolderElement(subfolder);
            container.appendChild(folderElement);
        });
        
        // Add files
        files.forEach(file => {
            const fileElement = this.createFileElement(file);
            container.appendChild(fileElement);
        });
    }
    
    /**
     * Create file element for the file tree
     */
    createFileElement(file) {
        const fileItem = document.createElement('div');
        fileItem.className = 'tree-item';
        fileItem.dataset.id = file.id;
        fileItem.dataset.type = 'file';
        
        const fileHeader = document.createElement('div');
        fileHeader.className = 'tree-item-header';
        
        const icon = document.createElement('span');
        icon.className = 'material-icons file-icon';
        icon.textContent = 'description'; // Use generic file icon
        
        // Add color class based on file type
        const fileType = this.getFileType(file.name);
        if (fileType) {
            icon.classList.add(`file-icon-${fileType}`);
        }
        
        const fileName = document.createElement('span');
        fileName.className = 'tree-item-name';
        fileName.textContent = file.name;
        
        const moreIcon = document.createElement('span');
        moreIcon.className = 'material-icons more-icon';
        moreIcon.textContent = 'more_vert';
        moreIcon.title = 'Options';
        
        fileHeader.appendChild(icon);
        fileHeader.appendChild(fileName);
        fileHeader.appendChild(moreIcon);
        fileItem.appendChild(fileHeader);
        
        // Add click event
        fileHeader.addEventListener('click', (e) => {
            // Prevent opening file when clicking more icon
            if (e.target === moreIcon || e.target.closest('.more-icon')) {
                return;
            }
            
            this.editor.openFile(file);
        });
        
        // Add context menu event
        fileHeader.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            this.editor.showContextMenu(e, 'file', file);
        });
        
        // Add more icon click event
        moreIcon.addEventListener('click', (e) => {
            e.stopPropagation();
            this.editor.showContextMenu(e, 'file', file);
        });
        
        return fileItem;
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
     * Show context menu
     */
    showContextMenu(event, itemType, item) {
        // Store the selected item
        this.contextItem = {
            type: itemType,
            data: item
        };
        
        const contextMenu = document.getElementById('contextMenu');
        
        // Position the menu
        const x = event.clientX;
        const y = event.clientY;
        
        // Check if triggered by more icon (more_vert)
        const isFromMoreIcon = event.target.classList.contains('more-icon') || 
                              event.target.closest('.more-icon');
        
        if (isFromMoreIcon) {
            // Position near the more icon
            const iconRect = event.target.getBoundingClientRect();
            contextMenu.style.left = `${iconRect.right - 10}px`;
            contextMenu.style.top = `${iconRect.bottom}px`;
        } else {
            // Position at cursor for right-click
            contextMenu.style.left = `${x}px`;
            contextMenu.style.top = `${y}px`;
        }
        
        contextMenu.style.display = 'block';
        
        // Enable/disable paste option based on clipboard
        const pasteOption = document.getElementById('contextMenuPaste');
        
        if (itemType === 'folder' && this.editor.clipboardItem) {
            pasteOption.classList.remove('disabled');
        } else {
            pasteOption.classList.add('disabled');
        }
        
        return this.contextItem;
    }
    
    /**
     * Hide context menu
     */
    hideContextMenu() {
        const contextMenu = document.getElementById('contextMenu');
        contextMenu.style.display = 'none';
    }
} 