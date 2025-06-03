/**
 * AIAssistant - Handles AI assistant functionalities
 */
export class AIAssistant {
    constructor(editor, codebaseIndexer) {
        this.editor = editor;
        this.codebaseIndexer = codebaseIndexer;
        this.messages = [];
        this.isProcessing = false;
        this.currentModel = 'gemini-2.0-flash';
        this.currentMode = 'agent';
        this.apiKey = localStorage.getItem('gemini_api_key');
        this.apiKeyRequested = false;
        this.apiKeyInvalid = !this.apiKey; // Initially true if no key
        this.pendingMessage = null; // To store message that triggered API key prompt
        
        // Function mapping for agent actions
        this.agentActions = {
            createFile: this.createFile.bind(this),
            updateFile: this.updateFile.bind(this),
            deleteFile: this.deleteFile.bind(this),
            copyFile: this.copyFile.bind(this),
            moveFile: this.moveFile.bind(this),
            createFolder: this.createFolder.bind(this),
            deleteFolder: this.deleteFolder.bind(this),
            explainCode: this.explainCode.bind(this)
        };
        
        this.initializeModels();
    }
    
    /**
     * Initialize available models
     */
    initializeModels() {
        this.availableModels = [
            {
                id: 'gemini-2.0-flash',
                name: 'gemini-2.0-flash',
                description: 'Fast responses, good for quick questions',
                contextLength: 128000,
                requestUrl: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent'
            },
            {
                id: 'gemini-2.0-pro',
                name: 'gemini-2.0-pro',
                description: 'Balanced performance for most tasks',
                contextLength: 128000,
                requestUrl: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-pro:generateContent'
            },
            {
                id: 'gemini-1.5-pro',
                name: 'gemini-1.5-pro',
                description: 'Advanced capabilities for complex tasks',
                contextLength: 1000000,
                requestUrl: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent'
            },
            {
                id: 'gemini-1.5-flash',
                name: 'gemini-1.5-flash',
                description: 'Efficient model with good performance',
                contextLength: 1000000,
                requestUrl: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent'
            }
        ];
    }
    
    /**
     * Show API key prompt in chat interface
     */
    showApiKeyPrompt() {
        if (this.apiKeyRequested) return;
        
        // If there's a pending message, ensure it's not added to chat history yet
        // as it will be processed after key is entered.
        // The user's input that triggered this prompt is already in the input bar or stored in this.pendingMessage.

        const apiKeyPromptHtml = `
            <div class="api-key-prompt">
                <h4>Gemini API Key Required</h4>
                <p>To use the AI assistant, please enter your Gemini API key. You can get a key from <a href="https://makersuite.google.com/app/apikey" target="_blank">Google AI Studio</a>.</p>
                <div class="api-key-input">
                    <input type="password" id="chatApiKeyInput" placeholder="Enter your Gemini API key">
                </div>
                <div class="api-key-actions">
                    <button class="api-key-button primary" id="saveChatApiKeyBtn">Save Key</button>
                    <button class="api-key-button secondary" id="cancelApiKeyBtn">Cancel</button>
                </div>
                <p class="api-key-settings-link">You can also set the API key in <a href="settings.html">Settings</a></p>
            </div>
        `;
        
        // Insert the API key prompt in the chat
        const chatMessagesEl = document.getElementById('chatMessages');
        
        // Remove empty state if it exists
        const emptyState = document.getElementById('chatEmptyState');
        if (emptyState) {
            emptyState.style.display = 'none';
        }
        
        // Add the prompt to the beginning of the chat
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = apiKeyPromptHtml;
        // Ensure chatMessagesEl exists and is clean before prepending
        if (chatMessagesEl) {
            const existingPrompt = chatMessagesEl.querySelector('.api-key-prompt');
            if (existingPrompt) existingPrompt.remove(); // Remove old prompt if any
            chatMessagesEl.prepend(tempDiv.firstElementChild);
        }
        
        // Set up event listeners
        document.getElementById('saveChatApiKeyBtn').addEventListener('click', () => {
            this.saveApiKeyFromChat();
        });
        
        document.getElementById('cancelApiKeyBtn').addEventListener('click', () => {
            this.cancelApiKeyPrompt();
        });
        
        // Allow pressing Enter to save
        document.getElementById('chatApiKeyInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                this.saveApiKeyFromChat();
            }
        });
        
        this.apiKeyRequested = true;
    }
    
    /**
     * Save API key entered in chat
     */
    saveApiKeyFromChat() {
        const apiKeyInput = document.getElementById('chatApiKeyInput');
        const apiKey = apiKeyInput.value.trim();
        
            if (apiKey) {
            // Save the API key to localStorage
                localStorage.setItem('gemini_api_key', apiKey);
            this.apiKey = apiKey;
            this.apiKeyInvalid = false; // Key is now considered valid
            
            // Remove the API key prompt
            const apiKeyPrompt = document.querySelector('.api-key-prompt');
            if (apiKeyPrompt) {
                apiKeyPrompt.remove();
            }
            
            // Show confirmation message
            this.addMessage('ai', 'API key saved successfully. You can now use the AI assistant.');
            
            // Reset flag
            this.apiKeyRequested = false;

            // If there was a pending message, process it now
            if (this.pendingMessage) {
                const messageToProcess = this.pendingMessage;
                this.pendingMessage = null;
                this.processMessage(messageToProcess); 
            }
        } else {
            alert('Please enter a valid API key.');
        }
    }
    
    /**
     * Cancel API key prompt
     */
    cancelApiKeyPrompt() {
        // Remove the API key prompt
        const apiKeyPrompt = document.querySelector('.api-key-prompt');
        if (apiKeyPrompt) {
            apiKeyPrompt.remove();
        }
        
        // Show the empty state if no messages (and no pending message that would repopulate)
        if (this.messages.length === 0 && !this.pendingMessage) {
            const emptyState = document.getElementById('chatEmptyState');
            if (emptyState) {
                emptyState.style.display = 'flex';
            }
        }
        
        // Reset flag
        this.apiKeyRequested = false;
        // If a message was pending, it remains pending. User needs to interact again or set key via settings.
    }
    
    /**
     * Set current AI model
     */
    setModel(modelId) {
        const model = this.availableModels.find(m => m.id === modelId);
        if (model) {
            this.currentModel = modelId;
            return true;
        }
        return false;
    }
    
    /**
     * Set current AI mode (agent or ask)
     */
    setMode(mode) {
        if (mode === 'agent' || mode === 'ask') {
            this.currentMode = mode;
            return true;
        }
        return false;
    }
    
    /**
     * Get current model info
     */
    getCurrentModel() {
        return this.availableModels.find(m => m.id === this.currentModel);
    }
    
    /**
     * Process a user message
     */
    async processMessage(message) {
        if (!message || this.isProcessing) return false;
        
        // Check if API key is missing or known to be invalid
        if (!this.apiKey || this.apiKeyInvalid) {
            this.pendingMessage = message; // Store the message
            this.showApiKeyPrompt();
            return false; // Stop processing until key is provided
        }
        
        this.isProcessing = true;
        
        try {
            // Add user message to history ONLY if it's not a re-process of a pending message
            // that was already handled by the API key prompt logic.
            // However, the current logic stores the original message in this.pendingMessage,
            // and processMessage is called with it. So it's okay to add it here.
            // The key prompt itself does not add the user message to chat.
            this.addMessage('user', message);
            
            // Show typing indicator
            this.editor.ui.showTypingIndicator();
            
            // Get context
            const context = this.getContext();
            
            // Prepare system prompt based on mode
            const systemPrompt = this.currentMode === 'agent' 
                ? this.getAgentSystemPrompt(context)
                : this.getAskSystemPrompt(context);
            
            // Prepare conversation history
            const promptMessages = [
                { role: 'system', content: systemPrompt },
                ...this.getMessageHistory()
            ];
            
            // Call the model API
            const response = await this.callModelAPI(promptMessages);
            
            // Process the response
            if (response) {
                if (this.currentMode === 'agent' && response.includes('ACTION:')) {
                    // Handle agent action
                    await this.handleAgentAction(response);
                } else {
                    // Regular response
                    this.addMessage('ai', response);
                }
            } else {
                this.addMessage('ai', 'Sorry, I encountered an error processing your request.');
            }
            
            // Hide typing indicator
            this.editor.ui.hideTypingIndicator();
            
            return true;
        } catch (error) {
            console.error('Error processing message:', error);
            this.addMessage('ai', `Error: ${error.message || 'Unknown error occurred'}`);
            this.editor.ui.hideTypingIndicator();
            return false;
        } finally {
            this.isProcessing = false;
        }
    }
    
    /**
     * Add a message to the history
     */
    addMessage(role, content) {
        const message = {
            role: role,
            content: content,
            timestamp: new Date().getTime()
        };
        
        this.messages.push(message);
        
        // Update UI
        if (role === 'user') {
            this.editor.ui.addChatMessage(content, 'user');
        } else {
            this.editor.ui.addChatMessage(content, 'ai');
        }
        
        return message;
    }
    
    /**
     * Get message history for the AI context
     */
    getMessageHistory(limit = 10) {
        // Get last N messages
        const recentMessages = this.messages.slice(-limit);
        
        // Format for API
        return recentMessages.map(msg => ({
            role: msg.role === 'ai' ? 'model' : msg.role,
            content: msg.content
        }));
    }
    
    /**
     * Call the Gemini API
     */
    async callModelAPI(messages) {
        // This initial check is redundant if processMessage handles it, but good for direct calls.
        if (!this.apiKey || this.apiKeyInvalid) {
            // If called directly and key is invalid, trigger prompt through processMessage flow
            // This specific path might be unlikely if UI always goes through processMessage.
            // For robustness:
            this.apiKeyInvalid = true; // Ensure it's marked
            this.showApiKeyPrompt(); // This will set pendingMessage if called with a user intent
            throw new Error('API key is required. Please provide it in the prompt above.');
        }
        
        const model = this.getCurrentModel();
        
        // Prepare request body
        const body = {
            contents: messages.map(msg => ({
                role: msg.role === 'ai' ? 'model' : msg.role,
                parts: [{ text: msg.content }]
            })),
            generationConfig: {
                temperature: 0.7,
                topP: 0.95,
                topK: 40,
                maxOutputTokens: 4096
            },
            safetySettings: [
                {
                    category: 'HARM_CATEGORY_HARASSMENT',
                    threshold: 'BLOCK_MEDIUM_AND_ABOVE'
                },
                {
                    category: 'HARM_CATEGORY_HATE_SPEECH',
                    threshold: 'BLOCK_MEDIUM_AND_ABOVE'
                },
                {
                    category: 'HARM_CATEGORY_SEXUALLY_EXPLICIT',
                    threshold: 'BLOCK_MEDIUM_AND_ABOVE'
                },
                {
                    category: 'HARM_CATEGORY_DANGEROUS_CONTENT',
                    threshold: 'BLOCK_MEDIUM_AND_ABOVE'
                }
            ]
        };
        
        try {
            const response = await fetch(`${model.requestUrl}?key=${this.apiKey}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                
                // Check if the error is related to the API key
                if (errorData.error && (
                    errorData.error.status === 'INVALID_ARGUMENT' || 
                    errorData.error.status === 'PERMISSION_DENIED' ||
                    errorData.error.status === 'UNAUTHENTICATED' ||
                    (errorData.error.message && errorData.error.message.toLowerCase().includes("api key not valid"))
                )) {
                    // Clear the invalid API key
                    this.apiKey = null;
                    localStorage.removeItem('gemini_api_key');
                    this.apiKeyInvalid = true; // Mark key as invalid
                    // Do not throw "Invalid API key" here directly if we want the prompt to show.
                    // The error will be caught by processMessage, which will then trigger the prompt.
                    // However, to make the console error clearer:
                    throw new Error('Invalid API key. Please provide a valid Gemini API key.');
                }
                
                throw new Error(`API Error: ${errorData.error.message || response.statusText}`);
            }
            
            const data = await response.json();
            
            if (data.candidates && data.candidates.length > 0 && 
                data.candidates[0].content && 
                data.candidates[0].content.parts && 
                data.candidates[0].content.parts.length > 0) {
                return data.candidates[0].content.parts[0].text;
            } else {
                throw new Error('Invalid response format from API');
            }
        } catch (error) {
            console.error('API call error:', error);
            throw error;
        }
    }
    
    /**
     * Get context for AI
     */
    getContext() {
        // Get current file if any
        const currentFile = this.editor.currentFile;
        const currentFileId = currentFile ? currentFile.id : null;
        
        // Get open files
        const openFiles = this.editor.monacoEditor ? 
            Object.keys(this.editor.monacoEditor.editors) : [];
        
        // Get context from indexer
        return this.codebaseIndexer.getContextForFile(currentFileId, openFiles);
    }
    
    /**
     * Get system prompt for agent mode
     */
    getAgentSystemPrompt(context) {
        return `You are CodeX AI, an advanced coding assistant integrated with a code editor.
You can help users with their coding tasks, including creating, updating, and explaining code.
You have access to the user's codebase and can perform actions like creating files, updating code, etc.

CAPABILITIES:
- Create new files
- Update existing files
- Delete files
- Create folders
- Delete folders
- Copy files
- Move files
- Explain code

When you need to perform an action, use the following format:
ACTION: <action_name>
PARAMS: <JSON string of parameters>
REASONING: <explain why you're taking this action>

Available actions:
- createFile(path, content)
- updateFile(fileId, content)
- deleteFile(fileId)
- copyFile(sourceId, destinationPath)
- moveFile(sourceId, destinationPath)
- createFolder(path)
- deleteFolder(path)
- explainCode(fileId, startLine, endLine)

Current project context:
${context.mainFile ? `Current file: ${context.mainFile.path}` : 'No file currently open'}
${context.relatedFiles.length > 0 ? `Related files: ${context.relatedFiles.map(f => f.path).join(', ')}` : ''}

Respond conversationally to the user. When they ask you to perform a task, use the appropriate action.
For code explanation, be thorough but concise. Format code examples with markdown.`;
    }
    
    /**
     * Get system prompt for ask mode
     */
    getAskSystemPrompt(context) {
        return `You are CodeX AI, an advanced coding assistant integrated with a code editor.
You help users with their coding questions and explanations. In this mode, you cannot directly
modify files, but you can explain code and provide guidance.

Current project context:
${context.mainFile ? `Current file: ${context.mainFile.path}` : 'No file currently open'}
${context.relatedFiles.length > 0 ? `Related files: ${context.relatedFiles.map(f => f.path).join(', ')}` : ''}

Respond conversationally to the user. Format code examples with markdown.
If the user asks you to perform actions that modify files, kindly explain that you're in "Ask mode" and
can only provide explanations in this mode. Suggest they switch to "Agent mode" for file operations.`;
    }
    
    /**
     * Parse and handle agent actions
     */
    async handleAgentAction(response) {
        try {
            // Split response to get the action part
            const parts = response.split('ACTION:');
            
            if (parts.length < 2) {
                // No action found, treat as regular response
                this.addMessage('ai', response);
                return;
            }
            
            // Add the first part (conversational text before the action) as AI message
            if (parts[0].trim()) {
                this.addMessage('ai', parts[0].trim());
            }
            
            // Process the action part (parts[1])
            const actionPart = parts[1];
            
            // Extract action name
            const actionNameMatch = actionPart.match(/^([a-zA-Z]+)/);
            if (!actionNameMatch) {
                throw new Error('Invalid action format: Missing action name');
            }
            const actionName = actionNameMatch[1];
            
            // Extract params
            const paramsMatch = actionPart.match(/PARAMS:([\s\S]*?)(?=REASONING:|$)/);
            if (!paramsMatch) {
                throw new Error('Invalid action format: Missing parameters');
            }
            const paramsString = paramsMatch[1].trim();
            const params = JSON.parse(paramsString);
            
            // Extract reasoning (optional)
            const reasoningMatch = actionPart.match(/REASONING:([\s\S]*?)(?=ACTION:|$)/);
            const reasoning = reasoningMatch ? reasoningMatch[1].trim() : 'No reasoning provided.';
            
            // Execute the action
            if (this.agentActions[actionName]) {
                const result = await this.agentActions[actionName](params);
                
                // Add result as a special AI message if it was a file operation with structured data
                // otherwise, add a generic success/failure message.
                if (result.success && (result.action === 'createFile' || result.action === 'updateFile' || result.action === 'deleteFile')) {
                    // Pass the whole result object which includes filePath, content etc.
                    this.addMessage('ai', result); 
                } else if (result.success) {
                    this.addMessage('ai', `Action ${actionName} completed successfully. ${result.message || ''}`);
                } else {
                    this.addMessage('ai', `Action ${actionName} failed. ${result.message || 'Unknown error.'}`);
                }
                
                // If there are more actions in the same response, process them recursively
                // This handles chained actions from the AI.
                const remainingActionString = actionPart.substring(actionNameMatch[0].length + 
                                                                (paramsMatch ? paramsMatch[0].length : 0) + 
                                                                (reasoningMatch ? reasoningMatch[0].length : 0)).trim();
                if (remainingActionString.startsWith('ACTION:')) {
                     await this.handleAgentAction(remainingActionString); // Pass the remaining part starting with ACTION:
                } else if (remainingActionString) {
                    // If there's trailing text that's not another action, add it as a message.
                    this.addMessage('ai', remainingActionString);
                }

            } else {
                throw new Error(`Unknown action: ${actionName}`);
            }
        } catch (error) {
            console.error('Error handling agent action:', error);
            this.addMessage('ai', `I encountered an error while trying to perform the action: ${error.message}`);
        }
    }
    
    /**
     * Create a new file
     */
    async createFile(params) {
        try {
            const { path, content } = params;
            const fileContent = content || ''; // Ensure content is not undefined
            
            if (!path) {
                throw new Error('File path is required');
            }
            
            // Extract folder path and file name
            const parts = path.split('/');
            const fileName = parts.pop();
            const folderPath = parts.length > 0 ? `/${parts.join('/')}` : '/';
            
            // Create the file
            const newFile = this.editor.fileManager.createFile(fileName, folderPath, fileContent);
            
            // Refresh file explorer
            this.editor.loadFiles();
            
            // Open the new file (optional, consider if diff view should open it)
            // this.editor.openFile(newFile);
            
            // Add to index
            this.codebaseIndexer.indexFile(newFile);
            
            return {
                success: true,
                action: 'createFile',
                fileId: newFile.id,
                filePath: newFile.path, // AIAssistant.js needs path, not name
                content: fileContent,
                message: `File "${newFile.path}" has been created successfully.`
            };
        } catch (error) {
            console.error('Error creating file:', error);
            return {
                success: false,
                message: `Failed to create file: ${error.message}`
            };
        }
    }
    
    /**
     * Update a file
     */
    async updateFile(params) {
        try {
            const { fileId, content: newContent } = params;
            
            if (!fileId || typeof newContent !== 'string') { // Check type of newContent
                throw new Error('File ID and content (string) are required');
            }
            
            // Get the file
            const file = this.editor.fileManager.getFile(fileId);
            if (!file) {
                throw new Error(`File with ID ${fileId} not found`);
            }

            // Get original content BEFORE updating
            const originalContent = await this.editor.fileManager.getFileContent(fileId);
            
            // Update the file
            this.editor.fileManager.updateFile(fileId, newContent);
            
            // Refresh the editor if the file is open and not in a diff view
            const activeTab = document.querySelector(`.tab.active[data-id="${fileId}"]`);
            const isDiffView = activeTab && activeTab.classList.contains('diff-view-tab');

            if (this.editor.monacoEditor.editors[fileId] && !isDiffView) {
                this.editor.monacoEditor.updateEditorContent(fileId, newContent);
            }
            
            // Update in index
            // Create a new file object with updated content for indexing
            const updatedFileForIndex = { ...file, content: newContent };
            this.codebaseIndexer.indexFile(updatedFileForIndex);
            
            return {
                success: true,
                action: 'updateFile',
                fileId: file.id,
                filePath: file.path,
                originalContent: originalContent,
                modifiedContent: newContent,
                message: `File "${file.path}" has been updated successfully.`
            };
        } catch (error) {
            console.error('Error updating file:', error);
            return {
                success: false,
                message: `Failed to update file: ${error.message}`
            };
        }
    }
    
    /**
     * Delete a file
     */
    async deleteFile(params) {
        try {
            const { fileId } = params;
            
            if (!fileId) {
                throw new Error('File ID is required');
            }
            
            // Get the file
            const file = this.editor.fileManager.getFile(fileId);
            if (!file) {
                throw new Error(`File with ID ${fileId} not found`);
            }
            
            const filePath = file.path;
            
            // Delete the file
            this.editor.fileManager.deleteFile(fileId);
            
            // Close the file if open (including any diff views for this fileId)
            this.editor.monacoEditor.closeTab(fileId); // Normal tab
            this.editor.monacoEditor.closeTab(`diff-${fileId}`); // Diff tab
            
            // Refresh file explorer
            this.editor.loadFiles();
            
            // Remove from index
            delete this.codebaseIndexer.codebaseIndex[fileId];
            
            return {
                success: true,
                action: 'deleteFile',
                fileId: fileId, // Return fileId for consistency, though less used for delete UI
                filePath: filePath,
                message: `File "${filePath}" has been deleted successfully.`
            };
        } catch (error) {
            console.error('Error deleting file:', error);
            return {
                success: false,
                message: `Failed to delete file: ${error.message}`
            };
        }
    }
    
    /**
     * Copy a file
     */
    async copyFile(params) {
        try {
            const { sourceId, destinationPath } = params;
            
            if (!sourceId || !destinationPath) {
                throw new Error('Source file ID and destination path are required');
            }
            
            // Get the source file
            const sourceFile = this.editor.fileManager.getFile(sourceId);
            if (!sourceFile) {
                throw new Error(`Source file with ID ${sourceId} not found`);
            }
            
            // Extract destination folder path and file name
            const parts = destinationPath.split('/');
            const fileName = parts.pop();
            const folderPath = parts.length > 0 ? `/${parts.join('/')}` : '/';
            
            // Get file content
            const content = await this.editor.fileManager.getFileContent(sourceId);
            
            // Create the new file
            const newFile = this.editor.fileManager.createFile(fileName, folderPath, content);
            
            // Refresh file explorer
            this.editor.loadFiles();
            
            // Index the new file
            this.codebaseIndexer.indexFile(newFile);
            
            return {
                success: true,
                fileId: newFile.id,
                message: `File "${sourceFile.path}" has been copied to "${destinationPath}" successfully.`
            };
        } catch (error) {
            console.error('Error copying file:', error);
            return {
                success: false,
                message: `Failed to copy file: ${error.message}`
            };
        }
    }
    
    /**
     * Move a file
     */
    async moveFile(params) {
        try {
            const { sourceId, destinationPath } = params;
            
            if (!sourceId || !destinationPath) {
                throw new Error('Source file ID and destination path are required');
            }
            
            // Get the source file
            const sourceFile = this.editor.fileManager.getFile(sourceId);
            if (!sourceFile) {
                throw new Error(`Source file with ID ${sourceId} not found`);
            }
            
            // Extract destination folder path and file name
            const parts = destinationPath.split('/');
            const fileName = parts.pop();
            const folderPath = parts.length > 0 ? `/${parts.join('/')}` : '/';
            
            // Get file content
            const content = await this.editor.fileManager.getFileContent(sourceId);
            
            // Create the new file
            const newFile = this.editor.fileManager.createFile(fileName, folderPath, content);
            
            // Delete the old file
            this.editor.fileManager.deleteFile(sourceId);
            
            // Close the old file if open
            if (this.editor.monacoEditor.editors[sourceId]) {
                this.editor.monacoEditor.closeTab(sourceId);
            }
            
            // Refresh file explorer
            this.editor.loadFiles();
            
            // Open the new file
            this.editor.openFile(newFile);
            
            // Update index
            delete this.codebaseIndexer.codebaseIndex[sourceId];
            this.codebaseIndexer.indexFile(newFile);
            
            return {
                success: true,
                fileId: newFile.id,
                message: `File "${sourceFile.path}" has been moved to "${destinationPath}" successfully.`
            };
        } catch (error) {
            console.error('Error moving file:', error);
            return {
                success: false,
                message: `Failed to move file: ${error.message}`
            };
        }
    }
    
    /**
     * Create a folder
     */
    async createFolder(params) {
        try {
            const { path } = params;
            
            if (!path) {
                throw new Error('Folder path is required');
            }
            
            // Extract parent folder path and folder name
            const parts = path.split('/').filter(p => p);
            const folderName = parts.pop();
            const parentPath = parts.length > 0 ? `/${parts.join('/')}` : '/';
            
            // Create the folder
            this.editor.fileManager.createFolder(folderName, parentPath);
            
            // Refresh file explorer
            this.editor.loadFiles();
            
            return {
                success: true,
                message: `Folder "${path}" has been created successfully.`
            };
        } catch (error) {
            console.error('Error creating folder:', error);
            return {
                success: false,
                message: `Failed to create folder: ${error.message}`
            };
        }
    }
    
    /**
     * Delete a folder
     */
    async deleteFolder(params) {
        try {
            const { path } = params;
            
            if (!path) {
                throw new Error('Folder path is required');
            }
            
            // Delete the folder
            this.editor.fileManager.deleteFolder(path);
            
            // Close any open files from this folder
            const folderId = path.replace(/^\//, '').replace(/\//g, '_');
            
            // Refresh file explorer
            this.editor.loadFiles();
            
            // Update index (remove all files in this folder)
            for (const fileId in this.codebaseIndexer.codebaseIndex) {
                const file = this.codebaseIndexer.codebaseIndex[fileId];
                if (file.path.startsWith(path)) {
                    delete this.codebaseIndexer.codebaseIndex[fileId];
                }
            }
            
            return {
                success: true,
                message: `Folder "${path}" has been deleted successfully.`
            };
        } catch (error) {
            console.error('Error deleting folder:', error);
            return {
                success: false,
                message: `Failed to delete folder: ${error.message}`
            };
        }
    }
    
    /**
     * Explain code
     */
    async explainCode(params) {
        try {
            const { fileId, startLine, endLine } = params;
            
            if (!fileId) {
                throw new Error('File ID is required');
            }
            
            // Get the file
            const file = this.editor.fileManager.getFile(fileId);
            if (!file) {
                throw new Error(`File with ID ${fileId} not found`);
            }
            
            // Get file content
            let content = await this.editor.fileManager.getFileContent(fileId);
            
            // Extract the specified lines if provided
            if (startLine !== undefined && endLine !== undefined) {
                const lines = content.split('\n');
                if (startLine > 0 && endLine <= lines.length && startLine <= endLine) {
                    content = lines.slice(startLine - 1, endLine).join('\n');
                }
            }
            
            // This is a special case where we don't actually perform a file operation
            // but we still use the agent action framework. We'll just return a message
            // that will be displayed to the user.
            
            return {
                success: true,
                message: `Here's the explanation for the code in "${file.path}":
                
I need to analyze this code further. Let me examine it for you.`,
                content: content,
                filePath: file.path,
                fileType: this.codebaseIndexer.getFileType(file.name)
            };
        } catch (error) {
            console.error('Error explaining code:', error);
            return {
                success: false,
                message: `Failed to explain code: ${error.message}`
            };
        }
    }
} 