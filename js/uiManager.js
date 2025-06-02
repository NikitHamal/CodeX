/**
 * UIManager - Handles UI interactions and DOM manipulations
 */
export class UIManager {
    /**
     * @param {ProjectManager} projectManager - Instance of ProjectManager
     */
    constructor(projectManager) {
        this.projectManager = projectManager;
        
        // DOM Elements
        this.projectsContainer = document.getElementById('projectsContainer');
        this.emptyState = document.getElementById('emptyState');
        this.overlay = document.getElementById('overlay');
        this.projectOptionsModal = document.getElementById('projectOptionsModal');
        this.newProjectModal = document.getElementById('newProjectModal');
        
        // Current state
        this.currentProjectId = null;
    }
    
    /**
     * Initialize the UI
     */
    init() {
        this.setupEventListeners();
        this.renderProjects();
    }
    
    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // New Project buttons
        document.getElementById('newProjectBtn').addEventListener('click', () => this.showNewProjectModal());
        document.getElementById('createFirstProjectBtn').addEventListener('click', () => this.showNewProjectModal());
        
        // Import Project button
        document.getElementById('importProjectBtn').addEventListener('click', () => this.handleImportProject());
        
        // New Project Modal
        document.getElementById('createProjectBtn').addEventListener('click', () => this.handleCreateProject());
        
        // Project Options Modal
        document.getElementById('renameProject').addEventListener('click', () => this.handleRenameProject());
        document.getElementById('exportProject').addEventListener('click', () => this.handleExportProject());
        document.getElementById('deleteProject').addEventListener('click', () => this.handleDeleteProject());
        
        // Close modals
        this.overlay.addEventListener('click', () => this.closeAllModals());
        document.querySelectorAll('.close-modal').forEach(button => {
            button.addEventListener('click', () => this.closeAllModals());
        });
        
        // Settings button
        document.querySelector('.settings-button').addEventListener('click', () => this.openSettings());
    }
    
    /**
     * Render projects in the UI
     */
    renderProjects() {
        const projects = this.projectManager.getAllProjects();
        
        // Show empty state if no projects
        if (projects.length === 0) {
            this.projectsContainer.style.display = 'none';
            this.emptyState.style.display = 'flex';
            return;
        }
        
        // Hide empty state and show projects
        this.projectsContainer.style.display = 'grid';
        this.emptyState.style.display = 'none';
        
        // Clear container
        this.projectsContainer.innerHTML = '';
        
        // Add project cards
        projects.forEach(project => {
            const projectCard = this.createProjectCard(project);
            this.projectsContainer.appendChild(projectCard);
        });
    }
    
    /**
     * Create a project card element
     * @param {Object} project - Project data
     * @returns {HTMLElement} Project card element
     */
    createProjectCard(project) {
        const card = document.createElement('div');
        card.className = 'project-card';
        card.dataset.id = project.id;
        
        card.innerHTML = `
            <div class="project-icon">
                <span class="material-icons">folder</span>
                <h3 class="project-title">${project.name}</h3>
            </div>
            <div class="project-time">Last modified: ${this.projectManager.constructor.formatRelativeTime(project.lastModified)}</div>
            <button class="icon-button project-more" aria-label="More options">
                <span class="material-icons">more_vert</span>
            </button>
        `;
        
        // Add event listeners
        card.addEventListener('click', (e) => {
            if (!e.target.closest('.project-more')) {
                this.openProject(project.id);
            }
        });
        
        const moreButton = card.querySelector('.project-more');
        moreButton.addEventListener('click', (e) => {
            e.stopPropagation();
            this.showProjectOptionsModal(project.id);
        });
        
        return card;
    }
    
    /**
     * Show the new project modal
     */
    showNewProjectModal() {
        this.overlay.style.display = 'block';
        this.newProjectModal.style.display = 'block';
        document.getElementById('projectName').focus();
    }
    
    /**
     * Show the project options modal
     * @param {String} projectId - ID of the project
     */
    showProjectOptionsModal(projectId) {
        this.currentProjectId = projectId;
        this.overlay.style.display = 'block';
        this.projectOptionsModal.style.display = 'block';
    }
    
    /**
     * Close all modals
     */
    closeAllModals() {
        this.overlay.style.display = 'none';
        this.projectOptionsModal.style.display = 'none';
        this.newProjectModal.style.display = 'none';
        document.getElementById('projectName').value = '';
        this.currentProjectId = null;
    }
    
    /**
     * Handle create project action
     */
    handleCreateProject() {
        const projectNameInput = document.getElementById('projectName');
        const projectName = projectNameInput.value.trim();
        
        if (projectName) {
            this.projectManager.createProject(projectName);
            this.renderProjects();
            this.closeAllModals();
        } else {
            projectNameInput.focus();
        }
    }
    
    /**
     * Handle rename project action
     */
    handleRenameProject() {
        if (!this.currentProjectId) return;
        
        const newName = prompt('Enter new project name:');
        
        if (newName && newName.trim()) {
            this.projectManager.renameProject(this.currentProjectId, newName.trim());
            this.renderProjects();
        }
        
        this.closeAllModals();
    }
    
    /**
     * Handle delete project action
     */
    handleDeleteProject() {
        if (!this.currentProjectId) return;
        
        const confirmDelete = confirm('Are you sure you want to delete this project? This action cannot be undone.');
        
        if (confirmDelete) {
            this.projectManager.deleteProject(this.currentProjectId);
            this.renderProjects();
        }
        
        this.closeAllModals();
    }
    
    /**
     * Handle export project action
     */
    handleExportProject() {
        if (!this.currentProjectId) return;
        
        this.projectManager.exportProject(this.currentProjectId)
            .then(({ projectName, data }) => {
                // Create a download link
                const blob = new Blob([data], { type: 'application/json' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `${projectName}.codex`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(url);
            })
            .catch(error => {
                console.error('Export failed:', error);
                alert('Failed to export project.');
            });
        
        this.closeAllModals();
    }
    
    /**
     * Handle import project action
     */
    handleImportProject() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.codex,.zip';
        
        input.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;
            
            this.projectManager.importProject(file)
                .then(() => {
                    this.renderProjects();
                })
                .catch(error => {
                    console.error('Import failed:', error);
                    alert('Failed to import project: ' + error.message);
                });
        });
        
        input.click();
    }
    
    /**
     * Open a project in the editor
     * @param {String} projectId - ID of the project to open
     */
    openProject(projectId) {
        // Store the current project ID in session storage
        sessionStorage.setItem('currentProjectId', projectId);
        
        // Navigate to the editor page
        window.location.href = 'editor.html';
    }
    
    /**
     * Open settings page
     */
    openSettings() {
        window.location.href = 'settings.html';
    }
} 