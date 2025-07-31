package com.codex.apk;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Represents an OpenAI / Qwen compatible function tool specification that can
 * be transmitted via the "tools" array in the request body.  Only the minimal
 * properties required by Qwen are included.
 */
public class ToolSpec {

    private final String name;
    private final String description;
    private final JsonObject parametersSchema;

    public ToolSpec(String name, String description, JsonObject parametersSchema) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
    }

    public String getName() {
        return name;
    }

    /**
     * Serialises this ToolSpec into the expected JSON structure used by Qwen / OpenAI.
     */
    public JsonObject toJson() {
        JsonObject fn = new JsonObject();
        fn.addProperty("name", name);
        fn.addProperty("description", description);
        fn.add("parameters", parametersSchema);

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", "function");
        wrapper.add("function", fn);
        return wrapper;
    }

    /**
     * Converts a list of ToolSpec to a JsonArray ready for inclusion in the
     * request body.
     */
    public static JsonArray toJsonArray(java.util.List<ToolSpec> specs) {
        JsonArray arr = new JsonArray();
        for (ToolSpec spec : specs) {
            arr.add(spec.toJson());
        }
        return arr;
    }

    /* ------------------------------------------------------------
     * Convenience helpers: common file-system manipulation tools
     * ------------------------------------------------------------ */
    public static java.util.List<ToolSpec> defaultFileTools() {
        java.util.List<ToolSpec> tools = new java.util.ArrayList<>();

        // createFile
        tools.add(new ToolSpec(
                "createFile",
                "Create a new file with the provided content (UTF-8). The file will be created in the project workspace.",
                buildSchema(
                        new String[]{"path", "content"},
                        new String[]{"string", "string"},
                        new String[]{"Relative path to the file to create", "Content to write to the file"}
                )));

        // updateFile
        tools.add(new ToolSpec(
                "updateFile",
                "Overwrite an existing file with new content. The file must exist in the project workspace.",
                buildSchema(
                        new String[]{"path", "content"},
                        new String[]{"string", "string"},
                        new String[]{"Relative path to the file to update", "New content to write to the file"}
                )));

        // deleteFile
        tools.add(new ToolSpec(
                "deleteFile",
                "Delete a file or an empty directory from the project workspace.",
                buildSchema(
                        new String[]{"path"},
                        new String[]{"string"},
                        new String[]{"Relative path to the file or directory to delete"}
                )));

        // renameFile
        tools.add(new ToolSpec(
                "renameFile",
                "Rename or move a file or directory within the project workspace.",
                buildSchema(
                        new String[]{"oldPath", "newPath"},
                        new String[]{"string", "string"},
                        new String[]{"Current path of the file or directory", "New path for the file or directory"}
                )));

        // readFile
        tools.add(new ToolSpec(
                "readFile",
                "Read the contents of a file from the project workspace.",
                buildSchema(
                        new String[]{"path"},
                        new String[]{"string"},
                        new String[]{"Relative path to the file to read"}
                )));

        // listFiles
        tools.add(new ToolSpec(
                "listFiles",
                "List files and directories in a directory within the project workspace.",
                buildSchema(
                        new String[]{"path"},
                        new String[]{"string"},
                        new String[]{"Relative path to the directory to list (use '.' for root)"}
                )));

        return tools;
    }

    /**
     * Enhanced schema builder with descriptions
     */
    private static JsonObject buildSchema(String[] keys, String[] types, String[] descriptions) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject props = new JsonObject();
        for (int i = 0; i < keys.length; i++) {
            JsonObject field = new JsonObject();
            field.addProperty("type", types[i]);
            if (descriptions != null && i < descriptions.length) {
                field.addProperty("description", descriptions[i]);
            }
            props.add(keys[i], field);
        }
        schema.add("properties", props);

        // required
        JsonArray req = new JsonArray();
        for (String k : keys) req.add(k);
        schema.add("required", req);
        return schema;
    }

    /**
     * Legacy schema builder for backward compatibility
     */
    private static JsonObject buildSchema(String[] keys, String[] types) {
        return buildSchema(keys, types, null);
    }
}