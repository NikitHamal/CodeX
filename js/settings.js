/**
 * Settings Manager - Handles user preferences and settings
 */
class SettingsManager {
    constructor() {
        this.settings = this.loadSettings();
        this.setupEventListeners();
        this.applySettings();
    }
    
    /**
     * Default settings
     */
    get defaultSettings() {
        return {
            theme: 'dark',
            fontSize: 14,
            tabSize: 4,
            wordWrap: true,
            lineNumbers: true
        };
    }
    
    /**
     * Load settings from localStorage
     */
    loadSettings() {
        const savedSettings = localStorage.getItem('codex_settings');
        return savedSettings ? { ...this.defaultSettings, ...JSON.parse(savedSettings) } : this.defaultSettings;
    }
    
    /**
     * Save settings to localStorage
     */
    saveSettings() {
        localStorage.setItem('codex_settings', JSON.stringify(this.settings));
    }
    
    /**
     * Setup event listeners for settings controls
     */
    setupEventListeners() {
        // Theme select
        const themeSelect = document.getElementById('themeSelect');
        themeSelect.value = this.settings.theme;
        themeSelect.addEventListener('change', () => {
            this.settings.theme = themeSelect.value;
            this.saveSettings();
            this.applyTheme();
        });
        
        // Font size range
        const fontSizeRange = document.getElementById('fontSizeRange');
        const fontSizeValue = document.getElementById('fontSizeValue');
        fontSizeRange.value = this.settings.fontSize;
        fontSizeValue.textContent = `${this.settings.fontSize}px`;
        
        fontSizeRange.addEventListener('input', () => {
            this.settings.fontSize = parseInt(fontSizeRange.value);
            fontSizeValue.textContent = `${this.settings.fontSize}px`;
            this.saveSettings();
        });
        
        // Tab size select
        const tabSizeSelect = document.getElementById('tabSizeSelect');
        tabSizeSelect.value = this.settings.tabSize;
        tabSizeSelect.addEventListener('change', () => {
            this.settings.tabSize = parseInt(tabSizeSelect.value);
            this.saveSettings();
        });
        
        // Word wrap toggle
        const wordWrapToggle = document.getElementById('wordWrapToggle');
        wordWrapToggle.checked = this.settings.wordWrap;
        wordWrapToggle.addEventListener('change', () => {
            this.settings.wordWrap = wordWrapToggle.checked;
            this.saveSettings();
        });
        
        // Line numbers toggle
        const lineNumbersToggle = document.getElementById('lineNumbersToggle');
        lineNumbersToggle.checked = this.settings.lineNumbers;
        lineNumbersToggle.addEventListener('change', () => {
            this.settings.lineNumbers = lineNumbersToggle.checked;
            this.saveSettings();
        });
        
        // Back button
        document.getElementById('backButton').addEventListener('click', () => {
            window.location.href = 'index.html';
        });
    }
    
    /**
     * Apply settings to the UI
     */
    applySettings() {
        this.applyTheme();
    }
    
    /**
     * Apply theme setting
     */
    applyTheme() {
        document.body.className = this.settings.theme === 'light' ? 'light-theme' : '';
    }
}

// Initialize settings manager when the DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new SettingsManager();
}); 