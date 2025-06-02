/**
 * PreviewManager - Handles the preview functionality
 */
export class PreviewManager {
    constructor(editor, fileManager) {
        this.editor = editor;
        this.fileManager = fileManager;
        this.previewFrame = document.getElementById('previewFrame');
        this.consoleOutput = document.getElementById('consoleOutput');
        
        // Set up message listener for console messages from iframe
        window.addEventListener('message', (event) => {
            if (event.data && event.data.type === 'console') {
                this.addConsoleMessage(event.data.message, event.data.messageType);
            }
        });
    }
    
    /**
     * Refresh the website preview
     */
    refreshPreview() {
        // Get all HTML, CSS, and JS files
        this.fileManager.getProjectFiles(this.editor.projectId)
            .then(files => {
                let htmlFiles = [];
                let cssFiles = [];
                let jsFiles = [];
                
                files.forEach(file => {
                    const extension = file.name.split('.').pop().toLowerCase();
                    if (extension === 'html') {
                        htmlFiles.push(file);
                    } else if (extension === 'css') {
                        cssFiles.push(file);
                    } else if (extension === 'js') {
                        jsFiles.push(file);
                    }
                });
                
                // Find index.html or first HTML file
                let mainHtmlFile = htmlFiles.find(f => f.name.toLowerCase() === 'index.html') || htmlFiles[0];
                
                if (mainHtmlFile) {
                    this.generatePreview(mainHtmlFile, cssFiles, jsFiles);
                } else {
                    this.updatePreviewFrame('<html><body><h1>No HTML file found</h1><p>Create an HTML file to see the preview</p></body></html>');
                }
            });
    }
    
    /**
     * Generate preview HTML with all resources included
     */
    generatePreview(htmlFile, cssFiles, jsFiles) {
        // Get HTML content
        this.fileManager.getFileContent(htmlFile.id)
            .then(htmlContent => {
                // Load all CSS files
                const cssPromises = cssFiles.map(file => 
                    this.fileManager.getFileContent(file.id)
                        .then(content => ({ name: file.name, content }))
                );
                
                // Load all JS files
                const jsPromises = jsFiles.map(file => 
                    this.fileManager.getFileContent(file.id)
                        .then(content => ({ name: file.name, content }))
                );
                
                // Wait for all resources to load
                Promise.all([Promise.all(cssPromises), Promise.all(jsPromises)])
                    .then(([cssResults, jsResults]) => {
                        // Inject CSS into HTML
                        let modifiedHtml = htmlContent;
                        
                        // Replace external CSS links with actual CSS content
                        cssResults.forEach(css => {
                            const linkRegex = new RegExp(`<link[^>]*href=["']([^"']*${css.name})["'][^>]*>`, 'g');
                            modifiedHtml = modifiedHtml.replace(linkRegex, `<style>${css.content}</style>`);
                        });
                        
                        // Add any CSS that wasn't linked
                        const headEndIndex = modifiedHtml.indexOf('</head>');
                        if (headEndIndex !== -1) {
                            const cssToInject = cssResults
                                .filter(css => !modifiedHtml.includes(css.name))
                                .map(css => `<style>/* ${css.name} */\n${css.content}</style>`)
                                .join('\n');
                            
                            if (cssToInject) {
                                modifiedHtml = modifiedHtml.slice(0, headEndIndex) + cssToInject + modifiedHtml.slice(headEndIndex);
                            }
                        }
                        
                        // Replace external JS scripts with actual JS content
                        jsResults.forEach(js => {
                            const scriptRegex = new RegExp(`<script[^>]*src=["']([^"']*${js.name})["'][^>]*></script>`, 'g');
                            modifiedHtml = modifiedHtml.replace(scriptRegex, `<script>${js.content}</script>`);
                        });
                        
                        // Add any JS that wasn't linked
                        const bodyEndIndex = modifiedHtml.indexOf('</body>');
                        if (bodyEndIndex !== -1) {
                            const jsToInject = jsResults
                                .filter(js => !modifiedHtml.includes(js.name))
                                .map(js => `<script>/* ${js.name} */\n${js.content}</script>`)
                                .join('\n');
                            
                            if (jsToInject) {
                                modifiedHtml = modifiedHtml.slice(0, bodyEndIndex) + jsToInject + modifiedHtml.slice(bodyEndIndex);
                            }
                        }
                        
                        // Add console interceptor
                        const consoleInterceptor = `
                        <script>
                        (function() {
                            const originalConsole = {
                                log: console.log,
                                error: console.error,
                                warn: console.warn,
                                info: console.info
                            };
                            
                            function sendToParent(message, type) {
                                window.parent.postMessage({ 
                                    type: 'console', 
                                    message: message,
                                    messageType: type
                                }, '*');
                            }
                            
                            console.log = function() {
                                const args = Array.from(arguments).map(function(arg) {
                                    return typeof arg === 'object' ? JSON.stringify(arg) : String(arg);
                                }).join(' ');
                                sendToParent(args, 'log');
                                originalConsole.log.apply(console, arguments);
                            };
                            
                            console.error = function() {
                                const args = Array.from(arguments).map(function(arg) {
                                    return typeof arg === 'object' ? JSON.stringify(arg) : String(arg);
                                }).join(' ');
                                sendToParent(args, 'error');
                                originalConsole.error.apply(console, arguments);
                            };
                            
                            console.warn = function() {
                                const args = Array.from(arguments).map(function(arg) {
                                    return typeof arg === 'object' ? JSON.stringify(arg) : String(arg);
                                }).join(' ');
                                sendToParent(args, 'warning');
                                originalConsole.warn.apply(console, arguments);
                            };
                            
                            console.info = function() {
                                const args = Array.from(arguments).map(function(arg) {
                                    return typeof arg === 'object' ? JSON.stringify(arg) : String(arg);
                                }).join(' ');
                                sendToParent(args, 'info');
                                originalConsole.info.apply(console, arguments);
                            };
                            
                            window.addEventListener('error', function(event) {
                                sendToParent(event.message + ' at ' + event.filename + ':' + event.lineno + ':' + event.colno, 'error');
                            });
                        })();
                        </script>
                        `;
                        
                        // Add console interceptor before closing body tag
                        if (bodyEndIndex !== -1) {
                            modifiedHtml = modifiedHtml.slice(0, bodyEndIndex) + consoleInterceptor + modifiedHtml.slice(bodyEndIndex);
                        }
                        
                        // Update preview
                        this.updatePreviewFrame(modifiedHtml);
                    });
            });
    }
    
    /**
     * Update preview iframe with content
     */
    updatePreviewFrame(htmlContent) {
        if (this.previewFrame) {
            const iframeDoc = this.previewFrame.contentDocument || this.previewFrame.contentWindow.document;
            iframeDoc.open();
            iframeDoc.write(htmlContent);
            iframeDoc.close();
        }
    }
    
    /**
     * Add message to console output
     */
    addConsoleMessage(message, type = '') {
        if (this.consoleOutput) {
            const lineElement = document.createElement('div');
            lineElement.className = `console-line ${type}`;
            lineElement.textContent = message;
            
            // Clear "Console output will appear here" message if it's the first real message
            if (this.consoleOutput.children.length === 1 && 
                this.consoleOutput.children[0].textContent.includes('Console output will appear here')) {
                this.consoleOutput.innerHTML = '';
            }
            
            this.consoleOutput.appendChild(lineElement);
            this.consoleOutput.scrollTop = this.consoleOutput.scrollHeight;
        }
    }
} 