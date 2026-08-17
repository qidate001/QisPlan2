package com.qidate.qisplan2.core;

import net.neoforged.neoforge.common.ModConfigSpec;

public class QisConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class ClientConfig {
        public final ModConfigSpec.ConfigValue<String> API_KEY;
        public final ModConfigSpec.ConfigValue<String> MODEL_NAME;

        ClientConfig(ModConfigSpec.Builder builder) {
            API_KEY = builder
                    .comment("你的大模型API密钥")
                    .define("apiKey", "");

            MODEL_NAME = builder
                    .comment("选择要使用的大模型")
                    .define("modelName", "deepseek-v4-flash");
        }
    }
}