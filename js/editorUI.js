/**
 * EditorUI - Handles the editor UI components
 */
export class EditorUI {
    constructor(editor) {
        this.editor = editor;
        
        // DOM elements
        this.projectTitle = document.getElementById('projectTitle');
        this.tabsContainer = document.getElementById('tabs');
        this.editorContent = document.getElementById('editorContent');
        this.explorerContainer = document.getElementById('explorer-panel');
        this.overlay = document.getElementById('overlay');
        this.newFileModal = document.getElementById('newFileModal');
        this.newFolderModal = document.getElementById('newFolderModal');
        this.renameModal = document.getElementById('renameModal');
        this.folderPathSelect = document.getElementById('folderPath');
        this.parentFolderPathSelect = document.getElementById('parentFolderPath');
        this.contextMenu = document.getElementById('contextMenu');
        this.chatEmptyState = document.getElementById('chatEmptyState');
        this.chatMessages = document.getElementById('chatMessages');
        this.previewFrame = document.getElementById('previewFrame');
        this.consoleOutput = document.getElementById('consoleOutput');
        
        // New AI chat elements
        this.modelSelectorButton = document.getElementById('modelSelectorButton');
        this.chatModeSelect = document.getElementById('chatModeSelect');
        this.modelSelectionModal = document.getElementById('modelSelectionModal');
        
        // Set up event listeners
        this.setupEventListeners();
    }
    
    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Back button
        const backButton = document.getElementById('backButton');
        if (backButton) {
            backButton.addEventListener('click', () => {
                window.location.href = 'index.html';
            });
        }
        
        // Settings button
        document.getElementById('settingsButton').addEventListener('click', () => {
            window.location.href = 'settings.html';
        });
        
        // Drawer toggle for mobile
        const drawerToggle = document.getElementById('drawerToggle');
        if (drawerToggle) {
            drawerToggle.addEventListener('click', () => {
                this.toggleSidebar();
            });
        }
        
        // Bottom navigation
        document.querySelectorAll('.bottom-nav-item').forEach(item => {
            item.addEventListener('click', () => {
                const tabId = item.dataset.tab;
                this.switchMobileTab(tabId, item);
            });
        });
        
        // Preview tabs
        document.querySelectorAll('.preview-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                const previewId = tab.dataset.preview;
                this.switchPreviewTab(previewId, tab);
            });
        });
        
        // Chat suggestions
        document.querySelectorAll('.chat-suggestion-item').forEach(item => {
            item.addEventListener('click', () => {
                const prompt = item.dataset.prompt;
                if (prompt) {
                    document.getElementById('chatInput').value = prompt;
                    this.editor.sendChatMessage();
                }
            });
        });
        
        // Chat send button
        const sendChatBtn = document.getElementById('sendChatBtn');
        const chatInput = document.getElementById('chatInput');
        if (sendChatBtn && chatInput) {
            sendChatBtn.addEventListener('click', () => {
                this.editor.sendChatMessage();
            });
            
            chatInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.editor.sendChatMessage();
                }
            });
        }
        
        // AI Model selector button
        if (this.modelSelectorButton) {
            this.modelSelectorButton.addEventListener('click', () => {
                this.showModelSelectionModal();
            });
        }
        
        // Chat mode selector
        if (this.chatModeSelect) {
            this.chatModeSelect.addEventListener('change', () => {
                if (this.editor.aiAssistant) {
                    this.editor.aiAssistant.setMode(this.chatModeSelect.value);
                }
            });
        }
        
        // Model selection items
        document.querySelectorAll('.model-item').forEach(item => {
            item.addEventListener('click', () => {
                const modelId = item.dataset.model;
                this.selectModel(modelId);
                this.editor.closeAllModals();
            });
        });
        
        // Refresh preview button
        const refreshPreviewBtn = document.getElementById('refreshPreviewBtn');
        if (refreshPreviewBtn) {
            refreshPreviewBtn.addEventListener('click', () => {
                this.editor.refreshPreview();
            });
        }
        
        // Sidebar panel switching
        document.querySelectorAll('.sidebar-icon').forEach(icon => {
            icon.addEventListener('click', () => {
                const panelId = icon.dataset.panel;
                this.switchSidebarPanel(panelId, icon);
            });
        });
        
        // New file button
        document.getElementById('newFileBtn').addEventListener('click', () => {
            this.editor.showNewFileModal();
        });
        
        // New folder button
        document.getElementById('newFolderBtn').addEventListener('click', () => {
            this.editor.showNewFolderModal();
        });
        
        // Create file button
        document.getElementById('createFileBtn').addEventListener('click', () => {
            this.editor.createNewFile();
        });
        
        // Create folder button
        document.getElementById('createFolderBtn').addEventListener('click', () => {
            this.editor.createNewFolder();
        });
        
        // Refresh button
        document.getElementById('refreshBtn').addEventListener('click', () => {
            this.editor.loadFiles();
        });
        
        // Close modals
        this.overlay.addEventListener('click', () => this.editor.closeAllModals());
        document.querySelectorAll('.close-modal').forEach(button => {
            button.addEventListener('click', () => this.editor.closeAllModals());
        });
        
        // Window resize event
        window.addEventListener('resize', () => {
            this.editor.resizeEditors();
        });
        
        // Context menu
        document.addEventListener('click', () => {
            this.editor.hideContextMenu();
        });
        
        // Context menu items
        document.getElementById('contextMenuRename').addEventListener('click', () => {
            this.editor.showRenameModal();
        });
        
        document.getElementById('contextMenuCopy').addEventListener('click', () => {
            this.editor.copyItem();
        });
        
        document.getElementById('contextMenuCut').addEventListener('click', () => {
            this.editor.cutItem();
        });
        
        document.getElementById('contextMenuPaste').addEventListener('click', () => {
            this.editor.pasteItem();
        });
        
        document.getElementById('contextMenuDelete').addEventListener('click', () => {
            this.editor.deleteItem();
        });
        
        // Rename confirmation
        document.getElementById('confirmRenameBtn').addEventListener('click', () => {
            this.editor.renameItem();
        });
        
        // Prevent default context menu
        document.addEventListener('contextmenu', (e) => {
            e.preventDefault();
        });
    }
    
    /**
     * Toggle sidebar visibility on mobile
     */
    toggleSidebar() {
        const sidebar = document.querySelector('.sidebar');
        sidebar.classList.toggle('open');
    }
    
    /**
     * Close sidebar on mobile
     */
    closeSidebar() {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }
    }
    
    /**
     * Switch between mobile tabs
     */
    switchMobileTab(tabId, clickedItem) {
        // Update active tab in bottom navigation
        document.querySelectorAll('.bottom-nav-item').forEach(item => {
            item.classList.remove('active');
        });
        clickedItem.classList.add('active');
        
        // Show the selected tab content
        document.querySelectorAll('.mobile-tab-content').forEach(content => {
            content.classList.remove('active');
        });
        
        const tabContent = document.getElementById(`${tabId}-tab-content`);
        if (tabContent) {
            tabContent.classList.add('active');
        }
        
        // Toggle status bar visibility based on active tab
        const statusBar = document.querySelector('.status-bar');
        if (statusBar) {
            statusBar.style.display = tabId === 'code' ? 'flex' : 'none';
        }
        
        // If preview tab is selected, refresh the preview
        if (tabId === 'preview') {
            this.editor.refreshPreview();
        }
        
        // Resize editors if code tab is selected
        if (tabId === 'code') {
            setTimeout(() => {
                this.editor.resizeEditors();
            }, 10);
        }
    }
    
    /**
     * Switch between preview tabs
     */
    switchPreviewTab(previewId, clickedTab) {
        // Update active tab
        document.querySelectorAll('.preview-tab').forEach(tab => {
            tab.classList.remove('active');
        });
        clickedTab.classList.add('active');
        
        // Show selected preview panel
        document.querySelectorAll('.preview-panel').forEach(panel => {
            panel.classList.remove('active');
        });
        document.getElementById(`${previewId}-preview`).classList.add('active');
        
        // Refresh the preview content
        if (previewId === 'website') {
            this.editor.refreshPreview();
        }
    }
    
    /**
     * Switch between sidebar panels
     */
    switchSidebarPanel(panelId, clickedIcon) {
        // For chat and preview, just open the tab without changing sidebar
        if (panelId === 'chat' || panelId === 'preview') {
            // Create tab if it doesn't exist
            const tabId = `${panelId}-tab`;
            if (!document.querySelector(`.tab[data-id="${tabId}"]`)) {
                this.createSpecialTab(panelId);
            }
            
            // Activate tab
            this.activateSpecialTab(tabId);
            
            // Show corresponding content
            this.showSpecialContent(panelId);
            return;
        }
        
        // For other panels (explorer, search, extensions), proceed with normal behavior
        // Update active icon
        document.querySelectorAll('.sidebar-icon').forEach(icon => {
            icon.classList.remove('active');
        });
        clickedIcon.classList.add('active');
        
        // Show selected panel
        document.querySelectorAll('.sidebar-panel').forEach(panel => {
            panel.classList.remove('active');
        });
        document.getElementById(`${panelId}-panel`).classList.add('active');
        
        // For explorer, search, etc. show the code content if there's an active file
        const activeTab = document.querySelector('.tab.active');
        if (activeTab && activeTab.dataset.id !== 'chat-tab' && activeTab.dataset.id !== 'preview-tab') {
            const fileId = activeTab.dataset.id;
            if (this.editor.monacoEditor.editors[fileId]) {
                this.editor.monacoEditor.showEditor(fileId);
            }
        }

        // Show corresponding main content area for mobile
        document.querySelectorAll('.mobile-tab-content').forEach(content => {
            content.classList.remove('active');
        });

        let mainContentToShow = null;
        if (panelId === 'explorer') {
            mainContentToShow = document.getElementById('code-tab-content');
        } else if (panelId === 'chat') {
            mainContentToShow = document.getElementById('chat-tab-content');
        } else if (panelId === 'preview') {
            mainContentToShow = document.getElementById('preview-tab-content');
            this.editor.refreshPreview();
        }

        if (mainContentToShow) {
            mainContentToShow.classList.add('active');
        }

        // Resize editors if code tab is implicitly selected
        if (panelId === 'explorer') {
            setTimeout(() => {
                this.editor.resizeEditors();
            }, 10);
        }
    }
    
    /**
     * Create a tab for chat or preview
     */
    createSpecialTab(panelType) {
        const tabId = `${panelType}-tab`;
        const tab = document.createElement('div');
        tab.className = 'tab';
        tab.dataset.id = tabId;
        
        const icon = document.createElement('span');
        icon.className = 'material-icons tab-icon';
        
        const title = document.createElement('span');
        title.className = 'tab-title';
        
        if (panelType === 'chat') {
            icon.textContent = 'chat';
            title.textContent = 'Chat';
        } else if (panelType === 'preview') {
            icon.textContent = 'visibility';
            title.textContent = 'Preview';
        }
        
        const closeBtn = document.createElement('span');
        closeBtn.className = 'material-icons tab-close';
        closeBtn.textContent = 'close';
        
        tab.appendChild(icon);
        tab.appendChild(title);
        tab.appendChild(closeBtn);
        
        // Add click events
        tab.addEventListener('click', () => {
            this.activateSpecialTab(tabId);
            this.showSpecialContent(panelType);
        });
        
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.closeSpecialTab(tabId);
        });
        
        const tabsContainer = document.getElementById('tabs');
        tabsContainer.appendChild(tab);
    }
    
    /**
     * Activate a special tab (chat or preview)
     */
    activateSpecialTab(tabId) {
        // Deactivate all tabs
        document.querySelectorAll('.tab').forEach(tab => {
            tab.classList.remove('active');
        });
        
        // Activate selected tab
        const tab = document.querySelector(`.tab[data-id="${tabId}"]`);
        if (tab) {
            tab.classList.add('active');
        }
    }
    
    /**
     * Show special content (chat or preview)
     */
    showSpecialContent(panelType) {
        // Hide all editors
        Object.keys(this.editor.monacoEditor.editors).forEach(id => {
            const editorContainer = document.getElementById(`editor-${id}`);
            if (editorContainer) {
                editorContainer.style.display = 'none';
            }
        });
        
        // Hide special content containers first
        const chatContainer = document.querySelector('.chat-container');
        const previewContainer = document.querySelector('.preview-container');
        
        if (chatContainer) chatContainer.style.display = 'none';
        if (previewContainer) previewContainer.style.display = 'none';
        
        // Hide status bar for special content
        const statusBar = document.querySelector('.status-bar');
        if (statusBar) statusBar.style.display = 'none';
        
        // Show the requested content
        if (panelType === 'chat') {
            if (chatContainer) {
                chatContainer.style.display = 'flex';
                // Move chat container to editor content area
                document.getElementById('editorContent').appendChild(chatContainer);
            }
        } else if (panelType === 'preview') {
            if (previewContainer) {
                previewContainer.style.display = 'flex';
                // Move preview container to editor content area
                document.getElementById('editorContent').appendChild(previewContainer);
                this.editor.refreshPreview();
            }
        }
    }
    
    /**
     * Close a special tab
     */
    closeSpecialTab(tabId) {
        // Remove tab
        const tab = document.querySelector(`.tab[data-id="${tabId}"]`);
        if (tab) {
            tab.remove();
        }
        
        // If there are other tabs, activate the first one
        const remainingTabs = document.querySelectorAll('.tab');
        if (remainingTabs.length > 0) {
            const nextTabId = remainingTabs[0].dataset.id;
            
            if (nextTabId.endsWith('-tab')) {
                // It's a special tab
                const panelType = nextTabId.replace('-tab', '');
                this.activateSpecialTab(nextTabId);
                this.showSpecialContent(panelType);
            } else {
                // It's a file tab
                const nextFile = this.editor.fileManager.getFile(nextTabId);
                if (nextFile) {
                    this.editor.openFile(nextFile);
                }
            }
        }
    }
    
    /**
     * Add a message to the chat
     */
    addChatMessage(message, sender) {
        const chatMessages = document.getElementById('chatMessages');
        if (!chatMessages) return;
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${sender}`;
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'chat-message-content';

        // Check if the message is an AI action result for file operations
        if (sender === 'ai' && typeof message === 'object' && message.action && message.filePath) {
            let labelText = '';
            let labelClass = '';
            let isClickable = false;

            switch (message.action) {
                case 'createFile':
                    labelText = 'NEW';
                    labelClass = 'label-new';
                    isClickable = true;
                    break;
                case 'updateFile':
                    labelText = 'UPDATED';
                    labelClass = 'label-updated';
                    isClickable = true;
                    break;
                case 'deleteFile':
                    labelText = 'DELETED';
                    labelClass = 'label-deleted';
                    break;
            }

            const actionMessage = document.createElement('p');
            const labelSpan = document.createElement('span');
            labelSpan.className = `chat-action-label ${labelClass}`;
            labelSpan.textContent = labelText;
            actionMessage.appendChild(labelSpan);

            if (isClickable) {
                const fileLink = document.createElement('a');
                fileLink.href = '#';
                fileLink.className = 'chat-file-link';
                fileLink.textContent = ` ${message.filePath}`;
                fileLink.dataset.action = message.action;
                fileLink.dataset.fileId = message.fileId;
                fileLink.dataset.filePath = message.filePath;
                if (message.action === 'createFile') {
                    fileLink.dataset.modifiedContent = message.content; 
                } else if (message.action === 'updateFile') {
                    fileLink.dataset.originalContent = message.originalContent;
                    fileLink.dataset.modifiedContent = message.modifiedContent;
                }
                fileLink.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.handleFileActionClick(e.currentTarget.dataset);
                });
                actionMessage.appendChild(fileLink);
            } else {
                actionMessage.appendChild(document.createTextNode(` ${message.filePath}`));
            }
            contentDiv.appendChild(actionMessage);
            // Optionally, add the AI's conversational message if it exists
            if (typeof message.message === 'string' && !message.message.startsWith('File ')) {
                 const followupP = document.createElement('p');
                 followupP.textContent = message.message;
                 contentDiv.appendChild(followupP);
            }

        } else if (sender === 'ai') {
            // Handle standard AI text message with markdown
            const formattedMessage = this.formatMarkdown(typeof message === 'string' ? message : message.message || 'Unexpected AI response format');
            contentDiv.innerHTML = formattedMessage;
        } else {
            // User message (plain text)
            const messageText = document.createElement('p');
            messageText.textContent = message;
            contentDiv.appendChild(messageText);
        }
        
        messageDiv.appendChild(contentDiv);
        
        // Add to DOM
        // If there's a typing indicator, insert before it, otherwise prepend
        const typingIndicator = chatMessages.querySelector('.typing-indicator-container');
        if (typingIndicator) {
            chatMessages.insertBefore(messageDiv, typingIndicator);
        } else {
            chatMessages.prepend(messageDiv); // Prepend to keep newest at bottom due to flex-direction: column-reverse
        }
        
        // Hide empty state if it's visible
        if (this.chatEmptyState && this.chatEmptyState.style.display !== 'none') {
            this.chatEmptyState.style.display = 'none';
        }
        
        // Scroll to bottom (which is top due to column-reverse)
        chatMessages.scrollTop = 0;
    }

    handleFileActionClick(dataset) {
        const { action, fileId, filePath, originalContent, modifiedContent } = dataset;
        
        if (action === 'createFile') {
            this.editor.monacoEditor.openDiffView(fileId, filePath, '', modifiedContent, 'New File');
        } else if (action === 'updateFile') {
            this.editor.monacoEditor.openDiffView(fileId, filePath, originalContent, modifiedContent, 'File Update');
        }
    }
    
    /**
     * Format markdown for chat messages
     */
    formatMarkdown(text) {
        // This is a very simple markdown parser, in a real application
        // you might want to use a library like marked.js
        
        // Code blocks
        text = text.replace(/```(\w*)([\s\S]*?)```/g, (match, lang, code) => {
            return `<pre><code class="language-${lang}">${this.escapeHtml(code.trim())}</code></pre>`;
        });
        
        // Inline code
        text = text.replace(/`([^`]+)`/g, '<code>$1</code>');
        
        // Headers
        text = text.replace(/^### (.*$)/gm, '<h3>$1</h3>');
        text = text.replace(/^## (.*$)/gm, '<h2>$1</h2>');
        text = text.replace(/^# (.*$)/gm, '<h1>$1</h1>');
        
        // Bold
        text = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        
        // Italic
        text = text.replace(/\*(.*?)\*/g, '<em>$1</em>');
        
        // Lists
        text = text.replace(/^\s*- (.*$)/gm, '<li>$1</li>');
        text = text.replace(/(<li>.*<\/li>)/gms, '<ul>$1</ul>');
        
        // Paragraphs
        text = text.replace(/^(?!<[hou]|<li|<pre)(.+)$/gm, '<p>$1</p>');
        
        return text;
    }
    
    /**
     * Escape HTML for code blocks
     */
    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
    
    /**
     * Show typing indicator for AI
     */
    showTypingIndicator() {
        this.hideTypingIndicator(); // Remove any existing indicator first

        const chatMessagesEl = document.getElementById('chatMessages');
        if (!chatMessagesEl) return;

        const indicatorEl = document.createElement('div');
        indicatorEl.classList.add('chat-message', 'ai', 'typing-indicator-container'); // Added a container class
        indicatorEl.innerHTML = `<div class="chat-message-content typing-indicator-text">AI is typing...</div>`;

        // If chat is scrolled to the bottom (or very close), append and scroll.
        // Otherwise, it might be less intrusive to not auto-scroll if the user has scrolled up to read history.
        const isScrolledToBottom = chatMessagesEl.scrollHeight - chatMessagesEl.clientHeight <= chatMessagesEl.scrollTop + 50;

        chatMessagesEl.appendChild(indicatorEl);

        if (isScrolledToBottom) {
            chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight;
        }
    }

    /**
     * Hide typing indicator for AI
     */
    hideTypingIndicator() {
        const typingIndicator = document.querySelector('.typing-indicator-container');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }
    
    /**
     * Show model selection modal
     */
    showModelSelectionModal() {
        document.getElementById('overlay').style.display = 'block';
        document.getElementById('modelSelectionModal').style.display = 'block';
    }
    
    /**
     * Select an AI model
     */
    selectModel(modelId) {
        if (this.editor.aiAssistant) {
            if (this.editor.aiAssistant.setModel(modelId)) {
                // Update selected model indicator
                document.querySelectorAll('.model-item .model-selected').forEach(el => {
                    el.textContent = '';
                });
                
                const selectedEl = document.querySelector(`.model-item[data-model="${modelId}"] .model-selected`);
                if (selectedEl) {
                    selectedEl.textContent = 'check';
                }
                
                // Update model name in button
                const modelNameEl = this.modelSelectorButton.querySelector('.model-name');
                if (modelNameEl) {
                    modelNameEl.textContent = modelId;
                }
            }
        }
    }
    
    /**
     * Add message to console output
     */
    addConsoleMessage(message, type = '') {
        const consoleOutput = document.getElementById('consoleOutput');
        if (consoleOutput) {
            const lineElement = document.createElement('div');
            lineElement.className = `console-line ${type}`;
            lineElement.textContent = message;
            
            // Clear "Console output will appear here" message if it's the first real message
            if (consoleOutput.children.length === 1 && 
                consoleOutput.children[0].textContent.includes('Console output will appear here')) {
                consoleOutput.innerHTML = '';
            }
            
            consoleOutput.appendChild(lineElement);
            consoleOutput.scrollTop = consoleOutput.scrollHeight;
        }
    }
} 