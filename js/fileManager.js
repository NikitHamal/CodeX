/**
 * FileManager - Handles file operations for a project
 */
export class FileManager {
    /**
     * @param {String} projectId - ID of the project
     * @param {ProjectManager} projectManager - Instance of ProjectManager
     */
    constructor(projectId, projectManager) {
        this.projectId = projectId;
        this.projectManager = projectManager;
    }
    
    /**
     * Get all files for the current project
     * @returns {Array} Array of file objects
     */
    getFiles() {
        const project = this.projectManager.getProject(this.projectId);
        return project ? project.files : [];
    }
    
    /**
     * Get a specific file by ID
     * @param {String} fileId - ID of the file to get
     * @returns {Object|null} File object or null if not found
     */
    getFile(fileId) {
        const files = this.getFiles();
        return files.find(file => file.id === fileId) || null;
    }
    
    /**
     * Get all folders in the project
     * @returns {Array} Array of folder objects
     */
    getFolders() {
        const project = this.projectManager.getProject(this.projectId);
        return project ? project.folders || [] : [];
    }
    
    /**
     * Get a specific folder by path
     * @param {String} folderPath - Path of the folder to get
     * @returns {Object|null} Folder object or null if not found
     */
    getFolder(folderPath) {
        const folders = this.getFolders();
        return folders.find(folder => folder.path === folderPath) || null;
    }
    
    /**
     * Initialize folders array if it doesn't exist
     */
    initFolders() {
        const project = this.projectManager.getProject(this.projectId);
        if (project && !project.folders) {
            project.folders = [
                {
                    id: 'root',
                    name: project.name,
                    path: '/',
                    createdAt: project.createdAt,
                    lastModified: project.lastModified
                }
            ];
            this.projectManager.saveProjects();
        }
        return project ? project.folders : [];
    }
    
    /**
     * Create a new file
     * @param {String} fileName - Name of the file to create
     * @param {String} folderPath - Path of the folder to create the file in
     * @returns {Object} Newly created file object
     */
    createFile(fileName, folderPath = '/') {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            throw new Error('Project not found');
        }
        
        // Initialize folders if needed
        this.initFolders();
        
        // Ensure the folder exists
        const folder = this.getFolder(folderPath);
        if (!folder && folderPath !== '/') {
            throw new Error(`Folder not found: ${folderPath}`);
        }
        
        // Create default content based on file extension
        let defaultContent = '';
        const extension = fileName.split('.').pop().toLowerCase();
        
        switch (extension) {
            case 'html':
                defaultContent = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Page</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <h1>Hello, World!</h1>
    <p>This is a sample HTML page.</p>
    <script src="script.js"></script>
</body>
</html>`;
                break;
            case 'css':
                defaultContent = `/* Styles for the page */
body {
    font-family: Arial, sans-serif;
    margin: 0;
    padding: 20px;
    line-height: 1.6;
}

h1 {
    color: #333;
}`;
                break;
            case 'js':
                defaultContent = `// JavaScript code
document.addEventListener('DOMContentLoaded', () => {
    console.log('Page loaded');
});`;
                break;
            case 'json':
                defaultContent = `{
    "name": "My Project",
    "version": "1.0.0",
    "description": "A sample project"
}`;
                break;
            case 'md':
                defaultContent = `# My Project

## Introduction
This is a sample Markdown file.

## Features
- Feature 1
- Feature 2
- Feature 3`;
                break;
        }
        
        // Normalize path
        const normalizedPath = folderPath === '/' ? '/' : `${folderPath}/`.replace(/\/+/g, '/');
        
        // Create new file object
        const newFile = {
            id: Date.now().toString(),
            name: fileName,
            path: normalizedPath + fileName,
            folderPath: normalizedPath,
            content: defaultContent,
            createdAt: new Date().toISOString(),
            lastModified: new Date().toISOString()
        };
        
        // Add file to project
        project.files.push(newFile);
        project.lastModified = new Date().toISOString();
        
        // Update folder lastModified
        if (folderPath !== '/') {
            const folder = this.getFolder(folderPath);
            if (folder) {
                folder.lastModified = new Date().toISOString();
            }
        }
        
        // Save changes
        this.projectManager.saveProjects();
        
        return newFile;
    }
    
    /**
     * Create a new folder
     * @param {String} folderName - Name of the folder to create
     * @param {String} parentPath - Path of the parent folder
     * @returns {Object} Newly created folder object
     */
    createFolder(folderName, parentPath = '/') {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            throw new Error('Project not found');
        }
        
        // Initialize folders if needed
        this.initFolders();
        
        // Ensure the parent folder exists
        const parentFolder = this.getFolder(parentPath);
        if (!parentFolder && parentPath !== '/') {
            throw new Error(`Parent folder not found: ${parentPath}`);
        }
        
        // Normalize path
        const normalizedParentPath = parentPath === '/' ? '/' : `${parentPath}/`.replace(/\/+/g, '/');
        const folderPath = normalizedParentPath === '/' ? `/${folderName}` : `${normalizedParentPath}${folderName}`;
        
        // Check if folder already exists
        const existingFolder = this.getFolder(folderPath);
        if (existingFolder) {
            throw new Error(`Folder already exists: ${folderPath}`);
        }
        
        // Create new folder object
        const newFolder = {
            id: Date.now().toString(),
            name: folderName,
            path: folderPath,
            parentPath: normalizedParentPath,
            createdAt: new Date().toISOString(),
            lastModified: new Date().toISOString()
        };
        
        // Add folder to project
        if (!project.folders) {
            project.folders = [];
        }
        project.folders.push(newFolder);
        project.lastModified = new Date().toISOString();
        
        // Update parent folder lastModified
        if (parentPath !== '/') {
            const parentFolder = this.getFolder(parentPath);
            if (parentFolder) {
                parentFolder.lastModified = new Date().toISOString();
            }
        }
        
        // Save changes
        this.projectManager.saveProjects();
        
        return newFolder;
    }
    
    /**
     * Get files in a specific folder
     * @param {String} folderPath - Path of the folder
     * @returns {Array} Array of file objects in the folder
     */
    getFilesInFolder(folderPath) {
        const files = this.getFiles();
        return files.filter(file => file.folderPath === folderPath);
    }
    
    /**
     * Get subfolders of a specific folder
     * @param {String} folderPath - Path of the folder
     * @returns {Array} Array of subfolder objects
     */
    getSubfolders(folderPath) {
        const folders = this.getFolders();
        return folders.filter(folder => folder.parentPath === folderPath);
    }
    
    /**
     * Update file content
     * @param {String} fileId - ID of the file to update
     * @param {String} content - New content for the file
     * @returns {Boolean} Success status
     */
    updateFileContent(fileId, content) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            return false;
        }
        
        const file = project.files.find(f => f.id === fileId);
        
        if (file) {
            file.content = content;
            file.lastModified = new Date().toISOString();
            project.lastModified = new Date().toISOString();
            
            // Save changes
            this.projectManager.saveProjects();
            return true;
        }
        
        return false;
    }
    
    /**
     * Rename a file
     * @param {String} fileId - ID of the file to rename
     * @param {String} newName - New name for the file
     * @returns {Boolean} Success status
     */
    renameFile(fileId, newName) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            return false;
        }
        
        const file = project.files.find(f => f.id === fileId);
        
        if (file) {
            file.name = newName;
            file.path = file.folderPath + newName;
            file.lastModified = new Date().toISOString();
            project.lastModified = new Date().toISOString();
            
            // Save changes
            this.projectManager.saveProjects();
            return true;
        }
        
        return false;
    }
    
    /**
     * Rename a folder
     * @param {String} folderPath - Path of the folder to rename
     * @param {String} newName - New name for the folder
     * @returns {Boolean} Success status
     */
    renameFolder(folderPath, newName) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            return false;
        }
        
        const folder = this.getFolder(folderPath);
        
        if (folder) {
            const oldPath = folder.path;
            const parentPath = folder.parentPath;
            const newPath = parentPath === '/' ? `/${newName}` : `${parentPath}${newName}`;
            
            // Update folder
            folder.name = newName;
            folder.path = newPath;
            folder.lastModified = new Date().toISOString();
            
            // Update paths of all subfolders and files
            this.updatePathsAfterFolderRename(oldPath, newPath);
            
            project.lastModified = new Date().toISOString();
            
            // Save changes
            this.projectManager.saveProjects();
            return true;
        }
        
        return false;
    }
    
    /**
     * Update paths after folder rename
     * @param {String} oldPath - Old folder path
     * @param {String} newPath - New folder path
     */
    updatePathsAfterFolderRename(oldPath, newPath) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            return;
        }
        
        // Update subfolders
        if (project.folders) {
            project.folders.forEach(folder => {
                if (folder.path !== oldPath && folder.path.startsWith(oldPath + '/')) {
                    folder.path = folder.path.replace(oldPath, newPath);
                }
                if (folder.parentPath === oldPath) {
                    folder.parentPath = newPath;
                }
            });
        }
        
        // Update files
        project.files.forEach(file => {
            if (file.folderPath === oldPath + '/') {
                file.folderPath = newPath + '/';
                file.path = file.path.replace(oldPath + '/', newPath + '/');
            }
        });
    }
    
    /**
     * Delete a file
     * @param {String} fileId - ID of the file to delete
     * @returns {Boolean} Success status
     */
    deleteFile(fileId) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project) {
            return false;
        }
        
        const initialLength = project.files.length;
        project.files = project.files.filter(file => file.id !== fileId);
        
        if (project.files.length !== initialLength) {
            project.lastModified = new Date().toISOString();
            
            // Save changes
            this.projectManager.saveProjects();
            return true;
        }
        
        return false;
    }
    
    /**
     * Delete a folder and all its contents
     * @param {String} folderPath - Path of the folder to delete
     * @returns {Boolean} Success status
     */
    deleteFolder(folderPath) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project || folderPath === '/') {
            return false;
        }
        
        // Delete subfolders
        if (project.folders) {
            const subfolderPaths = [];
            project.folders.forEach(folder => {
                if (folder.path === folderPath || folder.path.startsWith(folderPath + '/')) {
                    subfolderPaths.push(folder.path);
                }
            });
            
            if (subfolderPaths.length > 0) {
                project.folders = project.folders.filter(folder => !subfolderPaths.includes(folder.path));
            }
        }
        
        // Delete files in folder and subfolders
        const initialFileCount = project.files.length;
        project.files = project.files.filter(file => 
            !file.folderPath.startsWith(folderPath === '/' ? '/' : folderPath + '/')
        );
        
        if (project.folders.length !== initialFileCount || project.files.length !== initialFileCount) {
            project.lastModified = new Date().toISOString();
            
            // Save changes
            this.projectManager.saveProjects();
            return true;
        }
        
        return false;
    }
    
    /**
     * Copy a folder and all its contents to a new location
     * @param {String} sourceFolderPath - Path of the folder to copy
     * @param {String} targetParentPath - Path of the target parent folder
     * @returns {Object|null} The new folder object or null if failed
     */
    copyFolder(sourceFolderPath, targetParentPath) {
        const project = this.projectManager.getProject(this.projectId);
        
        if (!project || sourceFolderPath === '/') {
            return null;
        }
        
        // Get source folder
        const sourceFolder = this.getFolder(sourceFolderPath);
        if (!sourceFolder) {
            return null;
        }
        
        // Get target parent folder
        const targetParent = this.getFolder(targetParentPath);
        if (!targetParent && targetParentPath !== '/') {
            return null;
        }
        
        // Create new folder in target location
        const newFolder = this.createFolder(sourceFolder.name, targetParentPath);
        
        // Get all subfolders of the source folder
        const subfolders = this.getFolders().filter(folder => 
            folder.path !== sourceFolderPath && 
            folder.path.startsWith(sourceFolderPath + '/')
        );
        
        // Create subfolders in the new location
        const folderMapping = {};
        folderMapping[sourceFolderPath] = newFolder.path;
        
        // Sort subfolders by path length to ensure parent folders are created first
        subfolders.sort((a, b) => a.path.length - b.path.length);
        
        subfolders.forEach(subfolder => {
            // Find parent path in the mapping
            const relativePath = subfolder.path.substring(sourceFolderPath.length);
            const parentPath = subfolder.parentPath;
            const newParentPath = folderMapping[parentPath];
            
            if (newParentPath) {
                const newSubfolder = this.createFolder(subfolder.name, newParentPath);
                folderMapping[subfolder.path] = newSubfolder.path;
            }
        });
        
        // Copy files
        const files = this.getFiles().filter(file => 
            file.folderPath.startsWith(sourceFolderPath === '/' ? '/' : sourceFolderPath + '/')
        );
        
        files.forEach(file => {
            // Determine the new folder path
            const oldFolderPath = file.folderPath;
            let newFolderPath;
            
            // Handle files directly in the source folder
            if (oldFolderPath === sourceFolderPath + '/') {
                newFolderPath = newFolder.path;
            } else {
                // Find the corresponding folder in the mapping
                const parentFolderPath = oldFolderPath.substring(0, oldFolderPath.length - 1); // Remove trailing slash
                newFolderPath = folderMapping[parentFolderPath];
            }
            
            if (newFolderPath) {
                // Create the file in the new location
                const newFile = this.createFile(file.name, newFolderPath);
                this.updateFileContent(newFile.id, file.content);
            }
        });
        
        return newFolder;
    }
    
    /**
     * Get file content by ID
     * @param {String} fileId - ID of the file to get content for
     * @returns {Promise<String>} Promise resolving to file content
     */
    getFileContent(fileId) {
        return new Promise((resolve, reject) => {
            const file = this.getFile(fileId);
            if (file) {
                resolve(file.content || '');
            } else {
                reject(new Error(`File not found with ID: ${fileId}`));
            }
        });
    }
    
    /**
     * Get all files for the project
     * @param {String} projectId - ID of the project
     * @returns {Promise<Array>} Promise resolving to array of file objects
     */
    getProjectFiles(projectId) {
        return new Promise((resolve) => {
            const project = this.projectManager.getProject(projectId || this.projectId);
            resolve(project ? project.files : []);
        });
    }
} 