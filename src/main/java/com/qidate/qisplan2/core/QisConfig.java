package com.qidate.qisplan2.core;

import net.neoforged.neoforge.common.ModConfigSpec;

public class QisConfig {

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {

        ModConfigSpec.Builder builder =
                new ModConfigSpec.Builder();

        CLIENT =
                new ClientConfig(builder);

        CLIENT_SPEC =
                builder.build();
    }

    public static class ClientConfig {

        public final ModConfigSpec.ConfigValue<String> API_KEY;
        public final ModConfigSpec.ConfigValue<String> MODEL_NAME;

        /**
         * 使用纯白人体图
         */
        public final ModConfigSpec.BooleanValue WHITE_BODY_TEXTURE;

        ClientConfig(
                ModConfigSpec.Builder builder
        ) {

            builder.push("client");

            API_KEY =
                    builder
                            .comment("你的大模型API密钥")
                            .define("apiKey", "");

            MODEL_NAME =
                    builder
                            .comment("选择要使用的大模型")
                            .define("modelName", "deepseek-v4-flash");

            WHITE_BODY_TEXTURE =
                    builder
                            .comment(
                                    "使用纯白人体图（适合不希望看到器官细节的玩家）"
                            )
                            .define(
                                    "whiteBodyTexture",
                                    false
                            );

            builder.pop();
        }
    }
}