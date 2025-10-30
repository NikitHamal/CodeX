package com.codex.apk;

import java.util.List;

import java.io.File;
import com.codex.apk.ai.AIModel;

public interface ApiClient {
    List<AIModel> fetchModels();
}
