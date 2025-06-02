/**
 * ProjectManager - Handles all project data operations
 */
export class ProjectManager {
    constructor() {
        this.projects = this.loadProjects();
    }
    
    /**
     * Load projects from localStorage
     * @returns {Array} Array of project objects
     */
    loadProjects() {
        const savedProjects = localStorage.getItem('codex_projects');
        return savedProjects ? JSON.parse(savedProjects) : [];
    }
    
    /**
     * Save projects to localStorage
     */
    saveProjects() {
        localStorage.setItem('codex_projects', JSON.stringify(this.projects));
    }
    
    /**
     * Get all projects
     * @returns {Array} Array of project objects
     */
    getAllProjects() {
        return this.projects;
    }
    
    /**
     * Get a specific project by ID
     * @param {String} projectId - ID of the project to get
     * @returns {Object|null} Project object or null if not found
     */
    getProject(projectId) {
        return this.projects.find(project => project.id === projectId) || null;
    }
    
    /**
     * Create a new project
     * @param {String} name - Project name
     * @returns {Object} Newly created project
     */
    createProject(name) {
        const newProject = {
            id: Date.now().toString(),
            name: name,
            createdAt: new Date().toISOString(),
            lastModified: new Date().toISOString(),
            files: []
        };
        
        this.projects.push(newProject);
        this.saveProjects();
        return newProject;
    }
    
    /**
     * Delete a project
     * @param {String} projectId - ID of the project to delete
     * @returns {Boolean} Success status
     */
    deleteProject(projectId) {
        const initialLength = this.projects.length;
        this.projects = this.projects.filter(project => project.id !== projectId);
        
        if (this.projects.length !== initialLength) {
            this.saveProjects();
            return true;
        }
        return false;
    }
    
    /**
     * Rename a project
     * @param {String} projectId - ID of the project to rename
     * @param {String} newName - New project name
     * @returns {Boolean} Success status
     */
    renameProject(projectId, newName) {
        const project = this.projects.find(p => p.id === projectId);
        
        if (project) {
            project.name = newName;
            project.lastModified = new Date().toISOString();
            this.saveProjects();
            return true;
        }
        return false;
    }
    
    /**
     * Export project as a zip file
     * @param {String} projectId - ID of the project to export
     * @returns {Promise} Promise resolving to a Blob containing the zip file
     */
    exportProject(projectId) {
        // This is a placeholder for the export functionality
        // We'll implement the actual zip creation in a future update
        const project = this.projects.find(p => p.id === projectId);
        
        if (!project) {
            return Promise.reject(new Error('Project not found'));
        }
        
        return Promise.resolve({
            projectName: project.name,
            // In a real implementation, this would be a Blob containing the zip file
            data: JSON.stringify(project)
        });
    }
    
    /**
     * Import a project from a zip file
     * @param {File} zipFile - The zip file to import
     * @returns {Promise} Promise resolving to the imported project
     */
    importProject(zipFile) {
        // This is a placeholder for the import functionality
        // We'll implement the actual zip extraction in a future update
        return new Promise((resolve, reject) => {
            // In a real implementation, we would extract the zip file
            // and create a new project from its contents
            const reader = new FileReader();
            
            reader.onload = (e) => {
                try {
                    // For now, we'll just assume the zip contains a JSON representation of a project
                    const projectData = JSON.parse(e.target.result);
                    
                    const newProject = {
                        id: Date.now().toString(),
                        name: projectData.name || zipFile.name.replace(/\.zip$/, ''),
                        createdAt: new Date().toISOString(),
                        lastModified: new Date().toISOString(),
                        files: projectData.files || []
                    };
                    
                    this.projects.push(newProject);
                    this.saveProjects();
                    resolve(newProject);
                } catch (error) {
                    reject(new Error('Invalid project file'));
                }
            };
            
            reader.onerror = () => reject(new Error('Error reading file'));
            reader.readAsText(zipFile);
        });
    }
    
    /**
     * Format relative time (e.g., "2 hours ago")
     * @param {String} dateString - ISO date string
     * @returns {String} Formatted relative time
     */
    static formatRelativeTime(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);
        
        if (diffInSeconds < 60) {
            return `${diffInSeconds} second${diffInSeconds !== 1 ? 's' : ''} ago`;
        }
        
        const diffInMinutes = Math.floor(diffInSeconds / 60);
        if (diffInMinutes < 60) {
            return `${diffInMinutes} minute${diffInMinutes !== 1 ? 's' : ''} ago`;
        }
        
        const diffInHours = Math.floor(diffInMinutes / 60);
        if (diffInHours < 24) {
            return `${diffInHours} hour${diffInHours !== 1 ? 's' : ''} ago`;
        }
        
        const diffInDays = Math.floor(diffInHours / 24);
        if (diffInDays < 30) {
            return `${diffInDays} day${diffInDays !== 1 ? 's' : ''} ago`;
        }
        
        const diffInMonths = Math.floor(diffInDays / 30);
        if (diffInMonths < 12) {
            return `${diffInMonths} month${diffInMonths !== 1 ? 's' : ''} ago`;
        }
        
        const diffInYears = Math.floor(diffInMonths / 12);
        return `${diffInYears} year${diffInYears !== 1 ? 's' : ''} ago`;
    }
} 