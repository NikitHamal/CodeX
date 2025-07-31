package com.codex.apk;

import android.content.Context;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.List;

/**
 * Test class for demonstrating Qwen function calling implementation.
 * This class provides examples of how to use the enhanced function calling features.
 */
public class QwenFunctionCallingTest {
    private static final String TAG = "QwenFunctionCallingTest";

    /**
     * Test the JSON response parsing functionality
     */
    public static void testJsonResponseParsing() {
        Log.d(TAG, "Testing JSON response parsing...");

        // Test 1: Valid file operation JSON
        String validJson = "{\n" +
            "  \"action\": \"file_operation\",\n" +
            "  \"operations\": [\n" +
            "    {\n" +
            "      \"type\": \"createFile\",\n" +
            "      \"path\": \"test.txt\",\n" +
            "      \"content\": \"Hello World\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"explanation\": \"Created a test file\",\n" +
            "  \"suggestions\": [\"Add more content\", \"Consider adding a header\"]\n" +
            "}";

        QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(validJson);
        if (parsed != null && parsed.isValid) {
            Log.d(TAG, "✓ Valid JSON parsed successfully");
            Log.d(TAG, "Action: " + parsed.action);
            Log.d(TAG, "Operations: " + parsed.operations.size());
            Log.d(TAG, "Explanation: " + parsed.explanation);
            Log.d(TAG, "Suggestions: " + parsed.suggestions.size());
        } else {
            Log.e(TAG, "✗ Failed to parse valid JSON");
        }

        // Test 2: Invalid JSON
        String invalidJson = "{invalid json}";
        QwenResponseParser.ParsedResponse invalidParsed = QwenResponseParser.parseResponse(invalidJson);
        if (invalidParsed == null) {
            Log.d(TAG, "✓ Invalid JSON correctly rejected");
        } else {
            Log.e(TAG, "✗ Invalid JSON should have been rejected");
        }

        // Test 3: JSON detection
        String jsonStart = "{";
        String nonJson = "Hello World";
        
        if (QwenResponseParser.looksLikeJson(jsonStart)) {
            Log.d(TAG, "✓ JSON detection working for JSON start");
        } else {
            Log.e(TAG, "✗ JSON detection failed for JSON start");
        }
        
        if (!QwenResponseParser.looksLikeJson(nonJson)) {
            Log.d(TAG, "✓ JSON detection correctly rejected non-JSON");
        } else {
            Log.e(TAG, "✗ JSON detection incorrectly accepted non-JSON");
        }
    }

    /**
     * Test the tool execution functionality
     */
    public static void testToolExecution(Context context, File projectDir) {
        Log.d(TAG, "Testing tool execution...");

        try {
            // Create a test AIAssistant instance
            AIAssistant aiAssistant = new AIAssistant(context);
            aiAssistant.setCurrentProjectPath(projectDir.getAbsolutePath());
            aiAssistant.setEnabledTools(ToolSpec.defaultFileTools());

            // Test file creation
            String createFileJson = "{\"path\": \"test_create.txt\", \"content\": \"Test content\"}";
            JsonObject createArgs = JsonParser.parseString(createFileJson).getAsJsonObject();
            String createResult = aiAssistant.executeToolCall("createFile", createArgs);
            Log.d(TAG, "Create file result: " + createResult);

            // Test file reading
            String readFileJson = "{\"path\": \"test_create.txt\"}";
            JsonObject readArgs = JsonParser.parseString(readFileJson).getAsJsonObject();
            String readResult = aiAssistant.executeToolCall("readFile", readArgs);
            Log.d(TAG, "Read file result: " + readResult);

            // Test file listing
            String listFilesJson = "{\"path\": \".\"}";
            JsonObject listArgs = JsonParser.parseString(listFilesJson).getAsJsonObject();
            String listResult = aiAssistant.executeToolCall("listFiles", listArgs);
            Log.d(TAG, "List files result: " + listResult);

            // Test file update
            String updateFileJson = "{\"path\": \"test_create.txt\", \"content\": \"Updated content\"}";
            JsonObject updateArgs = JsonParser.parseString(updateFileJson).getAsJsonObject();
            String updateResult = aiAssistant.executeToolCall("updateFile", updateArgs);
            Log.d(TAG, "Update file result: " + updateResult);

            // Test file rename
            String renameFileJson = "{\"oldPath\": \"test_create.txt\", \"newPath\": \"test_renamed.txt\"}";
            JsonObject renameArgs = JsonParser.parseString(renameFileJson).getAsJsonObject();
            String renameResult = aiAssistant.executeToolCall("renameFile", renameArgs);
            Log.d(TAG, "Rename file result: " + renameResult);

            // Test file deletion
            String deleteFileJson = "{\"path\": \"test_renamed.txt\"}";
            JsonObject deleteArgs = JsonParser.parseString(deleteFileJson).getAsJsonObject();
            String deleteResult = aiAssistant.executeToolCall("deleteFile", deleteArgs);
            Log.d(TAG, "Delete file result: " + deleteResult);

        } catch (Exception e) {
            Log.e(TAG, "Error during tool execution test", e);
        }
    }

    /**
     * Test the complete function calling workflow
     */
    public static void testCompleteWorkflow(Context context, File projectDir) {
        Log.d(TAG, "Testing complete function calling workflow...");

        try {
            AIAssistant aiAssistant = new AIAssistant(context);
            aiAssistant.setCurrentProjectPath(projectDir.getAbsolutePath());
            aiAssistant.setEnabledTools(ToolSpec.defaultFileTools());

            // Set up response listener
            aiAssistant.setResponseListener(new AIAssistant.AIResponseListener() {
                @Override
                public void onResponse(String response, boolean isThinking, boolean isWebSearch, List<AIAssistant.WebSource> webSources) {
                    Log.d(TAG, "Response received: " + response);
                    Log.d(TAG, "Is thinking: " + isThinking);
                    Log.d(TAG, "Is web search: " + isWebSearch);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error: " + error);
                }

                @Override
                public void onStreamUpdate(String partialResponse, boolean isThinking) {
                    Log.d(TAG, "Stream update: " + partialResponse + " (thinking: " + isThinking + ")");
                }
            });

            // Set up action listener
            aiAssistant.setActionListener(new AIAssistant.AIActionListener() {
                @Override
                public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, 
                                               List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName) {
                    Log.d(TAG, "Actions processed:");
                    Log.d(TAG, "Raw JSON: " + rawAiResponseJson);
                    Log.d(TAG, "Explanation: " + explanation);
                    Log.d(TAG, "Suggestions: " + suggestions);
                    Log.d(TAG, "File changes: " + proposedFileChanges.size());
                    Log.d(TAG, "Model: " + aiModelDisplayName);
                }

                @Override
                public void onAiError(String errorMessage) {
                    Log.e(TAG, "AI Error: " + errorMessage);
                }

                @Override
                public void onAiRequestStarted() {
                    Log.d(TAG, "AI request started");
                }
            });

            // Test with a simple prompt
            String testPrompt = "Create a file called 'workflow_test.txt' with the content 'This is a test of the complete workflow'";
            aiAssistant.sendMessage(testPrompt, null);

        } catch (Exception e) {
            Log.e(TAG, "Error during complete workflow test", e);
        }
    }

    /**
     * Test JSON response format validation
     */
    public static void testJsonFormatValidation() {
        Log.d(TAG, "Testing JSON format validation...");

        // Test various JSON formats
        String[] testCases = {
            // Valid file operation
            "{\"action\":\"file_operation\",\"operations\":[{\"type\":\"createFile\",\"path\":\"test.txt\",\"content\":\"test\"}],\"explanation\":\"test\",\"suggestions\":[]}",
            
            // Valid with multiple operations
            "{\"action\":\"file_operation\",\"operations\":[{\"type\":\"createFile\",\"path\":\"file1.txt\",\"content\":\"content1\"},{\"type\":\"updateFile\",\"path\":\"file2.txt\",\"content\":\"content2\"}],\"explanation\":\"Multiple operations\",\"suggestions\":[\"suggestion1\",\"suggestion2\"]}",
            
            // Invalid - missing action
            "{\"operations\":[{\"type\":\"createFile\",\"path\":\"test.txt\"}]}",
            
            // Invalid - wrong action type
            "{\"action\":\"wrong_action\",\"operations\":[]}",
            
            // Invalid - malformed JSON
            "{invalid json}",
            
            // Valid - regular JSON (non-file operation)
            "{\"message\":\"This is a regular response\",\"data\":{\"key\":\"value\"}}"
        };

        for (int i = 0; i < testCases.length; i++) {
            String testCase = testCases[i];
            QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(testCase);
            
            if (parsed != null && parsed.isValid) {
                Log.d(TAG, "✓ Test case " + (i + 1) + " parsed successfully");
            } else {
                Log.d(TAG, "✗ Test case " + (i + 1) + " failed to parse (expected for invalid cases)");
            }
        }
    }

    /**
     * Run all tests
     */
    public static void runAllTests(Context context, File projectDir) {
        Log.d(TAG, "=== Starting Qwen Function Calling Tests ===");
        
        testJsonResponseParsing();
        testToolExecution(context, projectDir);
        testJsonFormatValidation();
        // Note: testCompleteWorkflow requires actual API calls and should be run separately
        
        Log.d(TAG, "=== Qwen Function Calling Tests Completed ===");
    }
}