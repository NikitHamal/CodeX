// Import modules
import { ProjectManager } from './projectManager.js';
import { UIManager } from './uiManager.js';

// Initialize the app when the DOM is fully loaded
document.addEventListener('DOMContentLoaded', () => {
    const projectManager = new ProjectManager();
    const uiManager = new UIManager(projectManager);
    
    // Initialize UI
    uiManager.init();
}); 