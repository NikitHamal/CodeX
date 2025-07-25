package com.codex.apk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CodeCompletionHelper provides basic code completion suggestions for various languages.
 */
public class CodeCompletionHelper {
    
    // HTML tags and attributes
    private static final String[] HTML_TAGS = {
        "html", "head", "body", "title", "meta", "link", "script", "style",
        "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6",
        "a", "img", "ul", "ol", "li", "table", "tr", "td", "th",
        "form", "input", "button", "select", "option", "textarea",
        "nav", "header", "footer", "section", "article", "aside",
        "canvas", "svg", "video", "audio"
    };
    
    private static final String[] HTML_ATTRIBUTES = {
        "id", "class", "style", "src", "href", "alt", "title", "type",
        "name", "value", "placeholder", "required", "disabled",
        "width", "height", "onclick", "onload", "onchange"
    };
    
    // CSS properties
    private static final String[] CSS_PROPERTIES = {
        "color", "background", "background-color", "font-size", "font-family",
        "margin", "padding", "border", "width", "height", "display",
        "position", "top", "left", "right", "bottom", "float", "clear",
        "text-align", "line-height", "opacity", "z-index", "overflow",
        "cursor", "transition", "transform", "box-shadow", "border-radius",
        "flex", "grid", "justify-content", "align-items", "flex-direction"
    };
    
    // JavaScript keywords and methods
    private static final String[] JS_KEYWORDS = {
        "var", "let", "const", "function", "return", "if", "else", "for",
        "while", "do", "switch", "case", "break", "continue", "try", "catch",
        "finally", "throw", "new", "this", "typeof", "instanceof",
        "document", "window", "console", "alert", "confirm", "prompt"
    };
    
    private static final String[] JS_METHODS = {
        "getElementById", "getElementsByClassName", "querySelector", "querySelectorAll",
        "addEventListener", "removeEventListener", "appendChild", "removeChild",
        "createElement", "setAttribute", "getAttribute", "classList",
        "innerHTML", "textContent", "style", "value", "length", "push", "pop",
        "slice", "splice", "indexOf", "includes", "map", "filter", "reduce"
    };
    
    /**
     * Gets code completion suggestions based on the current text and cursor position.
     * @param text The full text content
     * @param cursorPosition The current cursor position
     * @param fileExtension The file extension to determine language
     * @return List of completion suggestions
     */
    public static List<String> getCompletionSuggestions(String text, int cursorPosition, String fileExtension) {
        List<String> suggestions = new ArrayList<>();
        
        if (text == null || cursorPosition < 0 || cursorPosition > text.length()) {
            return suggestions;
        }
        
        // Get the word being typed
        String currentWord = getCurrentWord(text, cursorPosition);
        if (currentWord.isEmpty()) {
            return suggestions;
        }
        
        // Get suggestions based on file type
        switch (fileExtension.toLowerCase()) {
            case "html":
            case "htm":
                suggestions.addAll(getHtmlSuggestions(text, cursorPosition, currentWord));
                break;
            case "css":
                suggestions.addAll(getCssSuggestions(text, cursorPosition, currentWord));
                break;
            case "js":
            case "javascript":
                suggestions.addAll(getJavaScriptSuggestions(text, cursorPosition, currentWord));
                break;
        }
        
        return suggestions;
    }
    
    /**
     * Gets the current word being typed at the cursor position.
     */
    private static String getCurrentWord(String text, int cursorPosition) {
        if (cursorPosition == 0) return "";
        
        int start = cursorPosition - 1;
        while (start >= 0 && Character.isLetterOrDigit(text.charAt(start))) {
            start--;
        }
        start++;
        
        int end = cursorPosition;
        while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) {
            end++;
        }
        
        return text.substring(start, end);
    }
    
    /**
     * Gets HTML completion suggestions.
     */
    private static List<String> getHtmlSuggestions(String text, int cursorPosition, String currentWord) {
        List<String> suggestions = new ArrayList<>();
        
        // Check if we're inside a tag
        boolean insideTag = isInsideTag(text, cursorPosition);
        
        if (insideTag) {
            // Suggest attributes
            for (String attr : HTML_ATTRIBUTES) {
                if (attr.startsWith(currentWord.toLowerCase())) {
                    suggestions.add(attr + "=\"\"");
                }
            }
        } else {
            // Suggest tags
            for (String tag : HTML_TAGS) {
                if (tag.startsWith(currentWord.toLowerCase())) {
                    suggestions.add("<" + tag + ">");
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Gets CSS completion suggestions.
     */
    private static List<String> getCssSuggestions(String text, int cursorPosition, String currentWord) {
        List<String> suggestions = new ArrayList<>();
        
        for (String property : CSS_PROPERTIES) {
            if (property.startsWith(currentWord.toLowerCase())) {
                suggestions.add(property + ": ");
            }
        }
        
        return suggestions;
    }
    
    /**
     * Gets JavaScript completion suggestions.
     */
    private static List<String> getJavaScriptSuggestions(String text, int cursorPosition, String currentWord) {
        List<String> suggestions = new ArrayList<>();
        
        // Add keywords
        for (String keyword : JS_KEYWORDS) {
            if (keyword.startsWith(currentWord.toLowerCase())) {
                suggestions.add(keyword);
            }
        }
        
        // Add methods
        for (String method : JS_METHODS) {
            if (method.startsWith(currentWord)) {
                suggestions.add(method + "()");
            }
        }
        
        return suggestions;
    }
    
    /**
     * Checks if the cursor is inside an HTML tag.
     */
    private static boolean isInsideTag(String text, int cursorPosition) {
        int lastOpenTag = text.lastIndexOf('<', cursorPosition - 1);
        int lastCloseTag = text.lastIndexOf('>', cursorPosition - 1);
        
        return lastOpenTag > lastCloseTag;
    }
}