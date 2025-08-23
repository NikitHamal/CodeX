package com.codex.apk.core.tools;

import com.codex.apk.ToolSpec;
import com.codex.apk.core.model.ExecutionContext;
import com.codex.apk.core.model.ValidationResult;
import com.codex.apk.core.model.ToolCall;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Universal tool registry that manages tool definitions and their executors.
 * Provides a centralized system for registering, discovering, and executing tools
 * across different AI providers and contexts.
 */
public class ToolRegistry {
    
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolExecutor> executors = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final SecurityManager securityManager;
    
    public ToolRegistry() {
        this(Executors.newCachedThreadPool(), new DefaultSecurityManager());
    }
    
    public ToolRegistry(ExecutorService executorService, SecurityManager securityManager) {
        this.executorService = executorService;
        this.securityManager = securityManager;
        registerDefaultTools();
    }
    
    /**
     * Registers a tool with its executor.
     * 
     * @param definition Tool definition
     * @param executor Tool executor implementation
     * @throws IllegalArgumentException if tool is already registered
     */
    public void registerTool(ToolDefinition definition, ToolExecutor executor) {
        String name = definition.getName();
        
        if (tools.containsKey(name)) {
            throw new IllegalArgumentException("Tool already registered: " + name);
        }
        
        ValidationResult validation = definition.validate();
        if (validation.hasErrors()) {
            throw new IllegalArgumentException("Tool definition invalid: " + validation.getErrors());
        }
        
        tools.put(name, definition);
        executors.put(name, executor);
    }
    
    /**
     * Unregisters a tool and its executor.
     * 
     * @param name Tool name
     * @return true if tool was removed
     */
    public boolean unregisterTool(String name) {
        ToolDefinition removed = tools.remove(name);
        executors.remove(name);
        return removed != null;
    }
    
    /**
     * Gets all available tool definitions.
     * 
     * @return List of tool definitions
     */
    public List<ToolDefinition> getAvailableTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * Gets available tools that are compatible with the given context.
     * 
     * @param context Execution context
     * @return List of compatible tools
     */
    public List<ToolDefinition> getAvailableTools(ExecutionContext context) {
        List<ToolDefinition> compatibleTools = new ArrayList<>();
        
        for (ToolDefinition tool : tools.values()) {
            ToolExecutor executor = executors.get(tool.getName());
            if (executor != null && executor.isCompatibleWith(context)) {
                compatibleTools.add(tool);
            }
        }
        
        return compatibleTools;
    }
    
    /**
     * Gets tool definitions filtered by category.
     * 
     * @param category Tool category
     * @return List of tools in the category
     */
    public List<ToolDefinition> getToolsByCategory(ToolCategory category) {
        return tools.values().stream()
            .filter(tool -> tool.getCategories().contains(category))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Executes a tool call asynchronously.
     * 
     * @param call Tool call to execute
     * @param context Execution context
     * @return CompletableFuture with tool result
     */
    public CompletableFuture<ToolResult> executeTool(ToolCall call, ExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeToolSync(call, context);
            } catch (Exception e) {
                return ToolResult.error(call.getId(), "Execution failed: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    /**
     * Executes a tool call synchronously.
     * 
     * @param call Tool call to execute
     * @param context Execution context
     * @return Tool result
     * @throws ToolExecutionException if execution fails
     */
    public ToolResult executeToolSync(ToolCall call, ExecutionContext context) throws ToolExecutionException {
        String toolName = call.getName();
        
        ToolDefinition definition = tools.get(toolName);
        if (definition == null) {
            throw new ToolExecutionException("Tool not found: " + toolName);
        }
        
        ToolExecutor executor = executors.get(toolName);
        if (executor == null) {
            throw new ToolExecutionException("Executor not found for tool: " + toolName);
        }
        
        // Validate call against definition
        ValidationResult callValidation = definition.validateCall(call);
        if (callValidation.hasErrors()) {
            throw new ToolExecutionException("Tool call validation failed: " + callValidation.getErrors());
        }
        
        // Check permissions
        if (!securityManager.hasPermissions(context, executor.getRequiredPermissions())) {
            throw new ToolExecutionException("Insufficient permissions for tool: " + toolName);
        }
        
        // Execute the tool
        try {
            return executor.execute(call, context);
        } catch (Exception e) {
            throw new ToolExecutionException("Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a tool is registered.
     * 
     * @param name Tool name
     * @return true if tool is registered
     */
    public boolean isToolRegistered(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * Gets a tool definition by name.
     * 
     * @param name Tool name
     * @return Tool definition or null if not found
     */
    public ToolDefinition getToolDefinition(String name) {
        return tools.get(name);
    }
    
    /**
     * Converts tool definitions to ToolSpec format for AI requests.
     * 
     * @param toolNames List of tool names to include
     * @return List of ToolSpec objects
     */
    public List<ToolSpec> toToolSpecs(List<String> toolNames) {
        List<ToolSpec> specs = new ArrayList<>();
        
        for (String name : toolNames) {
            ToolDefinition definition = tools.get(name);
            if (definition != null) {
                specs.add(definition.toToolSpec());
            }
        }
        
        return specs;
    }
    
    /**
     * Converts all available tools to ToolSpec format.
     * 
     * @return List of all ToolSpec objects
     */
    public List<ToolSpec> getAllToolSpecs() {
        return tools.values().stream()
            .map(ToolDefinition::toToolSpec)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Shuts down the tool registry and its executor service.
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    private void registerDefaultTools() {
        // Register file system tools
        FileSystemToolExecutor fileExecutor = new FileSystemToolExecutor();
        
        registerTool(ToolDefinition.createFile(), fileExecutor);
        registerTool(ToolDefinition.readFile(), fileExecutor);
        registerTool(ToolDefinition.updateFile(), fileExecutor);
        registerTool(ToolDefinition.deleteFile(), fileExecutor);
        registerTool(ToolDefinition.listFiles(), fileExecutor);
        registerTool(ToolDefinition.searchFiles(), fileExecutor);
        
        // Register web search tool if available
        try {
            WebSearchToolExecutor webExecutor = new WebSearchToolExecutor();
            registerTool(ToolDefinition.webSearch(), webExecutor);
        } catch (Exception e) {
            // Web search not available
        }
    }
    
    /**
     * Exception thrown when tool execution fails.
     */
    public static class ToolExecutionException extends Exception {
        public ToolExecutionException(String message) {
            super(message);
        }
        
        public ToolExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

/**
 * Tool definition with metadata and validation.
 */
class ToolDefinition {
    private final String name;
    private final String description;
    private final com.google.gson.JsonObject parametersSchema;
    private final Set<Permission> permissions;
    private final Set<ToolCategory> categories;
    
    public ToolDefinition(String name, String description, com.google.gson.JsonObject parametersSchema,
                         Set<Permission> permissions, Set<ToolCategory> categories) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
        this.permissions = permissions != null ? permissions : new HashSet<>();
        this.categories = categories != null ? categories : new HashSet<>();
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return permissions; }
    public Set<ToolCategory> getCategories() { return categories; }
    
    public ValidationResult validateCall(ToolCall call) {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (!name.equals(call.getName())) {
            result.addError("Tool name mismatch");
        }
        
        // Could add JSON schema validation here
        
        return result.build();
    }
    
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (name == null || name.trim().isEmpty()) {
            result.addError("Tool name is required");
        }
        
        if (description == null || description.trim().isEmpty()) {
            result.addError("Tool description is required");
        }
        
        return result.build();
    }
    
    public ToolSpec toToolSpec() {
        return new ToolSpec(name, description, parametersSchema);
    }
    
    // Factory methods for common tools
    public static ToolDefinition createFile() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        // Add schema definition for createFile parameters
        
        return new ToolDefinition(
            "createFile",
            "Create a new file with the provided content",
            schema,
            Set.of(Permission.FILE_WRITE),
            Set.of(ToolCategory.FILE_SYSTEM)
        );
    }
    
    public static ToolDefinition readFile() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        // Add schema definition
        
        return new ToolDefinition(
            "readFile",
            "Read the contents of a file",
            schema,
            Set.of(Permission.FILE_READ),
            Set.of(ToolCategory.FILE_SYSTEM)
        );
    }
    
    public static ToolDefinition updateFile() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        
        return new ToolDefinition(
            "updateFile",
            "Update an existing file with new content",
            schema,
            Set.of(Permission.FILE_WRITE),
            Set.of(ToolCategory.FILE_SYSTEM)
        );
    }
    
    public static ToolDefinition deleteFile() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        
        return new ToolDefinition(
            "deleteFile",
            "Delete a file or directory",
            schema,
            Set.of(Permission.FILE_DELETE),
            Set.of(ToolCategory.FILE_SYSTEM)
        );
    }
    
    public static ToolDefinition listFiles() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        
        return new ToolDefinition(
            "listFiles",
            "List files in a directory",
            schema,
            Set.of(Permission.FILE_READ),
            Set.of(ToolCategory.FILE_SYSTEM)
        );
    }
    
    public static ToolDefinition searchFiles() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        
        return new ToolDefinition(
            "searchFiles",
            "Search for files matching criteria",
            schema,
            Set.of(Permission.FILE_READ),
            Set.of(ToolCategory.FILE_SYSTEM)
        );
    }
    
    public static ToolDefinition webSearch() {
        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
        
        return new ToolDefinition(
            "webSearch",
            "Search the web for information",
            schema,
            Set.of(Permission.NETWORK_ACCESS),
            Set.of(ToolCategory.WEB)
        );
    }
}

/**
 * Tool executor interface.
 */
interface ToolExecutor {
    ToolResult execute(ToolCall call, ExecutionContext context) throws Exception;
    Set<Permission> getRequiredPermissions();
    boolean isCompatibleWith(ExecutionContext context);
}

/**
 * Tool execution result.
 */
class ToolResult {
    private final String toolCallId;
    private final String content;
    private final boolean success;
    private final String errorMessage;
    private final Throwable exception;
    
    private ToolResult(String toolCallId, String content, boolean success, String errorMessage, Throwable exception) {
        this.toolCallId = toolCallId;
        this.content = content;
        this.success = success;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
    
    public String getToolCallId() { return toolCallId; }
    public String getContent() { return content; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public Throwable getException() { return exception; }
    
    public static ToolResult success(String toolCallId, String content) {
        return new ToolResult(toolCallId, content, true, null, null);
    }
    
    public static ToolResult error(String toolCallId, String errorMessage, Throwable exception) {
        return new ToolResult(toolCallId, null, false, errorMessage, exception);
    }
}

/**
 * Tool categories for organization.
 */
enum ToolCategory {
    FILE_SYSTEM,
    WEB,
    DATABASE,
    COMMUNICATION,
    ANALYSIS,
    UTILITY
}

/**
 * Permissions for tool execution.
 */
enum Permission {
    FILE_READ,
    FILE_WRITE,
    FILE_DELETE,
    NETWORK_ACCESS,
    DATABASE_ACCESS
}

/**
 * Security manager for tool permissions.
 */
interface SecurityManager {
    boolean hasPermissions(ExecutionContext context, Set<Permission> requiredPermissions);
}

/**
 * Default security manager implementation.
 */
class DefaultSecurityManager implements SecurityManager {
    @Override
    public boolean hasPermissions(ExecutionContext context, Set<Permission> requiredPermissions) {
        // For now, allow all permissions - could be enhanced with actual permission checking
        return true;
    }
}