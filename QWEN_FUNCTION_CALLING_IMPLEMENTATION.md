# Qwen Function Calling Implementation for CodeX

## Overview

This document describes the enhanced function calling implementation for Qwen models in the CodeX AI Code Editor. The implementation supports both native function calling and JSON response-based file operations.

## Key Features

### 1. Dual Approach Function Calling

The implementation supports two approaches for function calling:

#### A. Native Function Calling
- Uses Qwen's native function calling API
- Supports tools like `createFile`, `updateFile`, `deleteFile`, `renameFile`, `readFile`, `listFiles`
- Real-time tool execution and response streaming

#### B. JSON Response-Based File Operations
- Models respond with structured JSON containing file operations
- Supports complex multi-file operations in a single response
- Better handling of batch operations

### 2. Enhanced Tool Specifications

The `ToolSpec` class now includes:

```java
// File manipulation tools
- createFile: Create new files with content
- updateFile: Overwrite existing files
- deleteFile: Delete files or directories
- renameFile: Move or rename files/directories
- readFile: Read file contents
- listFiles: List directory contents
```

### 3. JSON Response Format

For JSON-based responses, the expected format is:

```json
{
  "action": "file_operation",
  "operations": [
    {
      "type": "createFile|updateFile|deleteFile|renameFile",
      "path": "file/path.txt",
      "content": "file content",
      "oldPath": "old/path.txt",
      "newPath": "new/path.txt"
    }
  ],
  "explanation": "Brief explanation of what was done",
  "suggestions": ["suggestion1", "suggestion2"]
}
```

## Implementation Details

### 1. AIAssistant.java Enhancements

#### System Prompt Selection
```java
// Enhanced system instruction for function calling
if (currentModel.supportsFunctionCalling() && enabledTools != null && !enabledTools.isEmpty()) {
    // Function calling enabled - use JSON response format
    systemMsg.addProperty("content", 
        "You are CodexAgent, an AI assistant inside a code editor.\n\n" +
        "IMPORTANT: When the user requests file operations (create, update, delete, rename files/folders), " +
        "you MUST respond with a JSON object containing the action details.\n\n" +
        "JSON Response Format:\n" +
        "{\n" +
        "  \"action\": \"file_operation\",\n" +
        "  \"operations\": [\n" +
        "    {\n" +
        "      \"type\": \"createFile|updateFile|deleteFile|renameFile\",\n" +
        "      \"path\": \"file/path.txt\",\n" +
        "      \"content\": \"file content\",\n" +
        "      \"oldPath\": \"old/path.txt\",\n" +
        "      \"newPath\": \"new/path.txt\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"explanation\": \"Brief explanation of what was done\",\n" +
        "  \"suggestions\": [\"suggestion1\", \"suggestion2\"]\n" +
        "}\n\n" +
        "For non-file operations, respond normally in plain text.\n" +
        "Always think step by step but output only the final JSON or text response.");
} else {
    // Standard system prompt for non-function calling
    systemMsg.addProperty("content", 
        "You are CodexAgent, an AI assistant inside a code editor.\n\n" +
        "- If the user's request requires changing the workspace (create, update, delete, rename files/folders) " +
        "respond with detailed instructions on what files to create or modify.\n" +
        "- Provide clear explanations and suggestions for improvements.\n" +
        "- Think step by step internally, but output only the final answer.");
}
```

#### Enhanced Stream Processing
```java
// Check if this might be a JSON response
if (QwenResponseParser.looksLikeJson(content)) {
    isJsonResponse = true;
    jsonResponseBuilder.append(content);
} else if (isJsonResponse) {
    // Continue accumulating JSON
    jsonResponseBuilder.append(content);
} else {
    // Regular text response
    if ("think".equals(phase)) {
        thinkingContent.append(content);
        if (responseListener != null) {
            responseListener.onStreamUpdate(thinkingContent.toString(), true);
        }
    } else if ("answer".equals(phase)) {
        answerContent.append(content);
        if (responseListener != null) {
            responseListener.onStreamUpdate(answerContent.toString(), false);
        }
    }
}
```

### 2. QwenResponseParser.java

New class for parsing and validating JSON responses:

```java
public class QwenResponseParser {
    // Parses JSON responses into structured objects
    public static ParsedResponse parseResponse(String responseText)
    
    // Validates if a response looks like JSON
    public static boolean looksLikeJson(String response)
    
    // Converts parsed responses to file action details
    public static List<ChatMessage.FileActionDetail> toFileActionDetails(ParsedResponse response)
}
```

### 3. Enhanced Tool Execution

The `executeToolCall` method now supports:

```java
case "readFile": {
    String path = args.get("path").getAsString();
    java.io.File file = new java.io.File(projectDir, path);
    if (!file.exists()) {
        result.addProperty("ok", false);
        result.addProperty("error", "File not found: " + path);
    } else {
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), 
                                  java.nio.charset.StandardCharsets.UTF_8);
        result.addProperty("ok", true);
        result.addProperty("content", content);
        result.addProperty("message", "File read: " + path);
    }
    break;
}

case "listFiles": {
    String path = args.get("path").getAsString();
    java.io.File dir = new java.io.File(projectDir, path);
    if (!dir.exists() || !dir.isDirectory()) {
        result.addProperty("ok", false);
        result.addProperty("error", "Directory not found: " + path);
    } else {
        JsonArray files = new JsonArray();
        java.io.File[] fileList = dir.listFiles();
        if (fileList != null) {
            for (java.io.File f : fileList) {
                JsonObject fileInfo = new JsonObject();
                fileInfo.addProperty("name", f.getName());
                fileInfo.addProperty("type", f.isDirectory() ? "directory" : "file");
                fileInfo.addProperty("size", f.length());
                files.add(fileInfo);
            }
        }
        result.addProperty("ok", true);
        result.add("files", files);
        result.addProperty("message", "Directory listed: " + path);
    }
    break;
}
```

## Usage Examples

### 1. Creating a File
User: "Create a new file called `hello.txt` with the content 'Hello World'"

Expected JSON Response:
```json
{
  "action": "file_operation",
  "operations": [
    {
      "type": "createFile",
      "path": "hello.txt",
      "content": "Hello World"
    }
  ],
  "explanation": "Created a new file hello.txt with the content 'Hello World'",
  "suggestions": ["Consider adding more descriptive content", "You might want to add a file extension"]
}
```

### 2. Updating Multiple Files
User: "Update the README.md file and create a new config.json file"

Expected JSON Response:
```json
{
  "action": "file_operation",
  "operations": [
    {
      "type": "updateFile",
      "path": "README.md",
      "content": "# Updated README\n\nThis is the updated content."
    },
    {
      "type": "createFile",
      "path": "config.json",
      "content": "{\n  \"version\": \"1.0.0\",\n  \"settings\": {}\n}"
    }
  ],
  "explanation": "Updated README.md with new content and created config.json with basic configuration",
  "suggestions": ["Consider adding more configuration options", "You might want to add documentation"]
}
```

## API Integration

### Qwen API Endpoints Used

1. **Chat Completions**: `POST /api/v2/chat/completions`
   - Supports streaming responses
   - Handles function calling
   - Supports thinking mode

2. **Models**: `GET /api/models`
   - Retrieves available models
   - Includes model capabilities

### Headers and Authentication

```java
Request.Builder requestBuilder = new Request.Builder()
    .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + conversationId)
    .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
    .addHeader("authorization", "Bearer " + qwenToken)
    .addHeader("content-type", "application/json")
    .addHeader("accept", "*/*")
    .addHeader("Cookie", QWEN_COOKIE)
    .addHeader("bx-ua", QWEN_BX_UA)
    .addHeader("bx-umidtoken", QWEN_BX_UMIDTOKEN)
    .addHeader("bx-v", QWEN_BX_V)
    // ... additional headers
```

## Error Handling

### 1. JSON Parsing Errors
- Invalid JSON responses are treated as regular text
- Logs warnings for parsing failures
- Graceful fallback to text response

### 2. File Operation Errors
- Individual file operations can fail without affecting others
- Detailed error messages for debugging
- Rollback mechanisms for critical operations

### 3. Network Errors
- Retry mechanisms for transient failures
- Timeout handling
- Connection error recovery

## Configuration

### Model Capabilities
```java
public boolean supportsFunctionCalling() {
    // For now, assume all models support function calling
    // This could be made more granular based on actual model capabilities
    return true;
}
```

### Tool Configuration
```java
// Enable tools for function calling
aiAssistant.setEnabledTools(ToolSpec.defaultFileTools());
```

## Testing

### 1. Function Calling Test
```java
// Test basic file creation
String prompt = "Create a test.txt file with 'Hello World' content";
aiAssistant.sendMessage(prompt, null);
```

### 2. JSON Response Test
```java
// Test JSON response parsing
String jsonResponse = "{\"action\":\"file_operation\",\"operations\":[...]}";
QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(jsonResponse);
```

### 3. Error Handling Test
```java
// Test invalid JSON handling
String invalidJson = "{invalid json}";
QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(invalidJson);
// Should return null
```

## Future Enhancements

### 1. Advanced Tool Support
- Git operations (commit, push, pull)
- Search and replace operations
- Code formatting tools
- Linting and validation tools

### 2. Enhanced JSON Schema
- Support for more complex operations
- Batch operation optimization
- Transaction-like operations

### 3. Model-Specific Optimizations
- Different prompts for different models
- Model-specific tool sets
- Performance optimizations

## Troubleshooting

### Common Issues

1. **Function Calling Not Working**
   - Check if tools are enabled: `aiAssistant.setEnabledTools(ToolSpec.defaultFileTools())`
   - Verify model supports function calling: `currentModel.supportsFunctionCalling()`
   - Check API token validity

2. **JSON Response Not Parsed**
   - Verify response format matches expected schema
   - Check for malformed JSON
   - Review system prompt for JSON format instructions

3. **File Operations Failing**
   - Check file permissions
   - Verify project directory is set
   - Review file paths for validity

### Debug Logging

Enable debug logging to troubleshoot issues:

```java
Log.d("AIAssistant", "Sending message: " + message);
Log.d("AIAssistant", "Response received: " + response);
Log.d("AIAssistant", "Parsed operations: " + operations.size());
```

## Conclusion

This implementation provides a robust foundation for function calling with Qwen models in CodeX. It supports both native function calling and JSON response-based operations, with comprehensive error handling and extensible architecture for future enhancements.