package com.codex.apk.ai;

public enum AIProvider {
    GOOGLE("Google"),
    ALIBABA("Alibaba"),
    AIRFORCE("Api.Airforce"),
    CLOUDFLARE("Cloudflare AI"),
    DEEPINFRA("DeepInfra"),
    FREE("Free"),
    COOKIES("Cookies"),
    GPT_OSS("GPT OSS"),
    KIMI("Kimi"),
    ZHIPU("Zhipu"),
    OIVSCodeSer0501("OIVSCodeSer0501"),
    OIVSCodeSer2("OIVSCodeSer2"),
    CHATAI("Chatai"),
    MINTLIFY("Mintlify"),
    WEWORDLE("WeWordle"),
    YQCLOUD("Yqcloud"),
    COHERE("Cohere"),
    LAMBDA("LambdaChat");

    private final String displayName;

    AIProvider(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
