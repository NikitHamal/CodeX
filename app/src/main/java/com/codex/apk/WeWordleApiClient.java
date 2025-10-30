package com.codex.apk;

import android.content.Context;
import com.codex.apk.ai.AIModel;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WeWordleApiClient extends AnyProviderApiClient {
    private static final String API_ENDPOINT = "https://wewordle.org/gptapi/v1/web/turbo";

    public WeWordleApiClient(Context context, AIAssistant.AIActionListener actionListener) {
        super(context, actionListener);
    }
}
