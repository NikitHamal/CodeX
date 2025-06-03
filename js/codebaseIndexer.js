/**
 * CodebaseIndexer - Handles indexing the codebase to provide context to AI
 */
export class CodebaseIndexer {
    constructor(fileManager) {
        this.fileManager = fileManager;
        this.codebaseIndex = {};
        this.fileContentCache = {};
        this.indexUpdateTime = null;
    }

    /**
     * Index the entire codebase
     */
    async indexCodebase() {
        console.log('Indexing codebase...');
        const files = this.fileManager.getFiles();
        
        // Clear existing index
        this.codebaseIndex = {};
        this.fileContentCache = {};
        
        // Process all files
        for (const file of files) {
            await this.indexFile(file);
        }
        
        this.indexUpdateTime = new Date();
        console.log(`Codebase indexing completed at ${this.indexUpdateTime.toLocaleString()}`);
        
        return this.codebaseIndex;
    }
    
    /**
     * Index a single file
     */
    async indexFile(file) {
        try {
            const content = await this.fileManager.getFileContent(file.id);
            this.fileContentCache[file.id] = content;
            
            const fileType = this.getFileType(file.name);
            const tokens = this.tokenizeContent(content, fileType);
            
            this.codebaseIndex[file.id] = {
                path: file.path,
                name: file.name,
                fileType: fileType,
                tokens: tokens,
                content: content,
                summary: this.generateFileSummary(content, fileType)
            };
            
            return true;
        } catch (error) {
            console.error(`Error indexing file ${file.name}:`, error);
            return false;
        }
    }
    
    /**
     * Generate a summary of a file
     */
    generateFileSummary(content, fileType) {
        // Simple summary for now: first 100 chars + size
        const size = content.length;
        const preview = content.substring(0, 100).replace(/\n/g, ' ');
        return `${preview}... (${size} bytes)`;
    }
    
    /**
     * Tokenize file content based on file type
     */
    tokenizeContent(content, fileType) {
        // Simple tokenization for now
        const tokens = content
            .replace(/[^\w\s]/g, ' ')
            .split(/\s+/)
            .filter(token => token.length > 2)
            .map(token => token.toLowerCase());
            
        return Array.from(new Set(tokens)); // Remove duplicates
    }
    
    /**
     * Get file type from file name
     */
    getFileType(fileName) {
        const extension = fileName.split('.').pop().toLowerCase();
        const fileTypes = {
            'js': 'javascript',
            'jsx': 'javascript',
            'ts': 'typescript',
            'tsx': 'typescript',
            'html': 'html',
            'css': 'css',
            'json': 'json',
            'md': 'markdown',
            'txt': 'text',
            'py': 'python',
            'java': 'java',
            'c': 'c',
            'cpp': 'cpp',
            'h': 'cpp',
            'cs': 'csharp',
            'php': 'php'
        };
        
        return fileTypes[extension] || 'unknown';
    }
    
    /**
     * Search for relevant files based on query
     */
    searchCodebase(query) {
        if (!query || Object.keys(this.codebaseIndex).length === 0) {
            return [];
        }
        
        const queryTokens = this.tokenizeContent(query, 'text');
        const results = [];
        
        // Search through all indexed files
        for (const fileId in this.codebaseIndex) {
            const file = this.codebaseIndex[fileId];
            let score = 0;
            
            // Check for token matches
            queryTokens.forEach(token => {
                if (file.tokens.includes(token)) {
                    score += 1;
                }
                
                // Check if token appears in file name or path (higher weight)
                if (file.name.toLowerCase().includes(token) || 
                    file.path.toLowerCase().includes(token)) {
                    score += 3;
                }
                
                // Check raw content for exact matches
                const tokenRegex = new RegExp(token, 'gi');
                const matches = (file.content.match(tokenRegex) || []).length;
                score += matches * 0.5;
            });
            
            if (score > 0) {
                results.push({
                    fileId: fileId,
                    file: file,
                    score: score
                });
            }
        }
        
        // Sort by score (descending)
        return results.sort((a, b) => b.score - a.score);
    }
    
    /**
     * Get recent file changes
     */
    getRecentChanges(limit = 5) {
        // This would track file changes, but for now just returns the most recently indexed files
        const files = Object.values(this.codebaseIndex);
        return files.slice(0, limit);
    }
    
    /**
     * Get project structure for context
     */
    getProjectStructure() {
        const structure = {};
        
        for (const fileId in this.codebaseIndex) {
            const file = this.codebaseIndex[fileId];
            
            // Skip files with undefined path
            if (!file || !file.path) {
                continue;
            }
            
            const pathParts = file.path.split('/').filter(part => part);
            
            let current = structure;
            for (let i = 0; i < pathParts.length; i++) {
                const part = pathParts[i];
                
                if (i === pathParts.length - 1) {
                    // This is the file
                    current[part] = {
                        type: 'file',
                        fileType: file.fileType,
                        summary: file.summary
                    };
                } else {
                    // This is a directory
                    if (!current[part]) {
                        current[part] = {
                            type: 'directory',
                            children: {}
                        };
                    }
                    current = current[part].children;
                }
            }
        }
        
        return structure;
    }
    
    /**
     * Get context for AI based on current file
     */
    getContextForFile(fileId, additionalFiles = []) {
        const context = {
            mainFile: null,
            relatedFiles: [],
            projectStructure: this.getProjectStructure()
        };
        
        // Add main file
        if (fileId && this.codebaseIndex[fileId]) {
            context.mainFile = {
                path: this.codebaseIndex[fileId].path,
                content: this.codebaseIndex[fileId].content,
                fileType: this.codebaseIndex[fileId].fileType
            };
        }
        
        // Add additional files specified
        for (const additionalFileId of additionalFiles) {
            if (this.codebaseIndex[additionalFileId]) {
                context.relatedFiles.push({
                    path: this.codebaseIndex[additionalFileId].path,
                    content: this.codebaseIndex[additionalFileId].content,
                    fileType: this.codebaseIndex[additionalFileId].fileType
                });
            }
        }
        
        // Auto-detect related files
        if (fileId && this.codebaseIndex[fileId]) {
            const mainFile = this.codebaseIndex[fileId];
            const mainFileTokens = mainFile.tokens;
            
            for (const candidateFileId in this.codebaseIndex) {
                if (candidateFileId === fileId || additionalFiles.includes(candidateFileId)) {
                    continue; // Skip main file and already added files
                }
                
                const candidateFile = this.codebaseIndex[candidateFileId];
                
                // Calculate token overlap
                const overlap = mainFileTokens.filter(token => 
                    candidateFile.tokens.includes(token)
                ).length;
                
                if (overlap > 5) {
                    context.relatedFiles.push({
                        path: candidateFile.path,
                        content: candidateFile.content,
                        fileType: candidateFile.fileType
                    });
                    
                    // Limit to 5 auto-detected related files
                    if (context.relatedFiles.length >= 5) {
                        break;
                    }
                }
            }
        }
        
        return context;
    }
} 