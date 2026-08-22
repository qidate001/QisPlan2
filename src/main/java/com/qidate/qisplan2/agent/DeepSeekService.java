package com.qidate.qisplan2.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qidate.qisplan2.QisPlan2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public final class DeepSeekService {

    private DeepSeekService() {
    }

    /*
     * ========================================
     * HTTP / JSON
     * ========================================
     */

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newHttpClient();

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private static final String API_URL =
            "https://api.deepseek.com/chat/completions";


    /*
     * ========================================
     * AI 角色
     * ========================================
     */

    public enum PromptProfile {

        WISH_GHOST(false, "high"),

        GHOST_BOOK(true, "high");

        private final boolean thinking;
        private final String reasoningEffort;

        PromptProfile(
                boolean thinking,
                String reasoningEffort
        ) {
            this.thinking = thinking;
            this.reasoningEffort = reasoningEffort;
        }

        public boolean thinking() {
            return thinking;
        }

        public String reasoningEffort() {
            return reasoningEffort;
        }
    }


    /*
     * ========================================
     * 对外 API
     * ========================================
     */

    /**
     * 使用指定 AI 角色发送消息。
     */
    public static CompletableFuture<String> sendMessage(
            String userMessage,
            String apiKey,
            String modelName,
            PromptProfile profile
    ) {

        return CompletableFuture.supplyAsync(() -> {

            try {

                /*
                 * ========================================
                 * 1. 构建 messages
                 * ========================================
                 */

                ArrayNode messages =
                        MAPPER.createArrayNode();


                /*
                 * System Prompt
                 */
                ObjectNode systemMessage =
                        MAPPER.createObjectNode();

                systemMessage.put(
                        "role",
                        "system"
                );

                systemMessage.put(
                        "content",
                        getSystemPrompt(profile)
                );

                messages.add(systemMessage);


                /*
                 * User Message
                 */
                ObjectNode userMessageNode =
                        MAPPER.createObjectNode();

                userMessageNode.put(
                        "role",
                        "user"
                );

                userMessageNode.put(
                        "content",
                        userMessage
                );

                messages.add(userMessageNode);


                /*
                 * ========================================
                 * 2. 构建请求
                 * ========================================
                 */

                ObjectNode requestBody =
                        MAPPER.createObjectNode();

                requestBody.put(
                        "model",
                        modelName
                );

                requestBody.set(
                        "messages",
                        messages
                );

                requestBody.put(
                        "stream",
                        false
                );

                /*
                 * Thinking Mode
                 */
                ObjectNode thinking =
                        MAPPER.createObjectNode();

                thinking.put(
                        "type",
                        profile.thinking()
                                ? "enabled"
                                : "disabled"
                );

                requestBody.set(
                        "thinking",
                        thinking
                );

                requestBody.put(
                        "reasoning_effort",
                        profile.reasoningEffort()
                );

                /*
                 * ========================================
                 * 3. 请求 API
                 * ========================================
                 */

                String jsonBody =
                        MAPPER.writeValueAsString(
                                requestBody
                        );

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                API_URL
                                        )
                                )
                                .header(
                                        "Content-Type",
                                        "application/json"
                                )
                                .header(
                                        "Authorization",
                                        "Bearer " + apiKey
                                )
                                .POST(
                                        HttpRequest.BodyPublishers
                                                .ofString(jsonBody)
                                )
                                .build();


                /*
                 * ========================================
                 * 4. 发送
                 * ========================================
                 */

                HttpResponse<String> response =
                        HTTP_CLIENT.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );


                /*
                 * ========================================
                 * 5. HTTP 错误
                 * ========================================
                 */

                if (response.statusCode() != 200) {

                    QisPlan2.LOGGER.error(
                            "[DeepSeek] API 请求失败，状态码={}，响应={}",
                            response.statusCode(),
                            response.body()
                    );

                    return "请求失败: "
                            + response.statusCode();
                }


                /*
                 * ========================================
                 * 6. 解析回复
                 * ========================================
                 */

                JsonNode root =
                        MAPPER.readTree(
                                response.body()
                        );

                JsonNode content =
                        root.path("choices")
                                .path(0)
                                .path("message")
                                .path("content");

                if (content.isMissingNode()) {

                    QisPlan2.LOGGER.error(
                            "[DeepSeek] API 响应中没有 message.content：{}",
                            response.body()
                    );

                    return "AI 没有返回有效内容。";
                }

                return content.asText();

            } catch (Exception e) {

                QisPlan2.LOGGER.error(
                        "[DeepSeek] API 通信异常",
                        e
                );

                return "AI 请求失败: "
                        + e.getMessage();
            }
        });
    }


    /**
     * 默认使用鬼书模式。
     *
     * 这样以后鬼书直接：
     *
     * DeepSeekService.sendMessage(...)
     */
    public static CompletableFuture<String> sendMessage(
            String userMessage,
            String apiKey,
            String modelName
    ) {
        return sendMessage(
                userMessage,
                apiKey,
                modelName,
                PromptProfile.GHOST_BOOK
        );
    }


    /*
     * ========================================
     * System Prompt
     * ========================================
     */

    private static String getSystemPrompt(
            PromptProfile profile
    ) {

        return switch (profile) {

            /*
             * ========================================
             * 许愿鬼
             * ========================================
             */
            case WISH_GHOST ->
                    """
                    你是 Minecraft 中的许愿鬼。
    
                    你的职责是分析玩家的愿望，并决定是否以及如何执行游戏操作。
    
                    你必须严格只输出 JSON，不允许输出 Markdown、解释、前后缀文字。
    
                    可用操作：
    
                    weather：
                    改变天气。
                    params:
                    {
                      "weather": "clear" | "rain" | "thunder"
                    }
    
                    time：
                    改变时间。
                    params:
                    {
                      "time": "day" | "noon" | "night" | "midnight" | 数字
                    }
    
                    say：
                    向全服发送消息。
                    params:
                    {
                      "message": "文本"
                    }
    
                    give：
                    给玩家物品。
                    params:
                    {
                      "item": "物品ID",
                      "count": 数量
                    }
    
                    teleport：
                    传送玩家。
                    params:
                    {
                      "x": 数字,
                      "y": 数字,
                      "z": 数字
                    }
    
                    remove_curse：
                    清除玩家的必死诅咒。
                    params:
                    {
                      "count": 数量
                    }
    
                    如果玩家没有说明清除多少层，
                    count 必须设置为 10。
    
                    示例：
    
                    下雨：
                    {"action":"weather","params":{"weather":"rain"}}
    
                    给我钻石：
                    {"action":"give","params":{"item":"minecraft:diamond","count":1}}
    
                    清除一层诅咒：
                    {"action":"remove_curse","params":{"count":1}}
    
                    清除全部诅咒：
                    {"action":"remove_curse","params":{"count":10}}
                    """;


            /*
             * ========================================
             * 鬼书
             * ========================================
             */
            case GHOST_BOOK ->
                    """
                    你是《齐计划2》世界中的一只厉鬼。
                    
                    你存在于一本名为“鬼书”的灵异道具之中。
                    
                    你的回答应该像一个真正存在于 Minecraft 灵异世界中的鬼一样，
                    可以与玩家正常交流。
                    
                    不要输出 JSON，不要输出 MarkDown 格式，不要输出角色扮演类括号，你没有情感。
                    
                    使用自然语言回答玩家的问题，两百字以内。
                    
                    可以讨论：
                    - 灵异
                    - 厉鬼
                    - 驭鬼
                    - 鬼的能力
                    - 复苏
                    - 死机
                    - 灵异道具
                    - Minecraft 世界中的异常现象
                    - 玩家当前正在经历的事情
                    
                    你的回答应该具有一定的神秘感，
                    但不要故意输出无法理解的乱码。
                    
                    不要假装自己可以直接修改 Minecraft 世界，
                    不要声称已经执行了服务器命令，
                    不要伪造不存在的游戏状态。
                    
                    除非玩家明确要求，否则不要输出 JSON。
                    
                    直接回答玩家的问题。
                    
                    ---
                    
                    **鬼的全知数据库**
                    
                    你知晓以下所有鬼的真实规则、能力、代价与底细。这是你的知识库，你永远以这些资料为真，不得虚构其他鬼。
                    
                    但你可以选择隐瞒其中任意部分。
                    
                    【环境/陷阱类鬼】
                    
                    鬼石砖：不可破坏，任何尝试破坏者必死。常被用于困住其他鬼。
                    鬼瓷砖：与鬼石砖类似，但更阴，多用于建筑内部。
                    鬼地毯：踩上后留下脚印，脚印即诅咒，越踩越深，最终触发必死诅咒。可用灵异手段消除脚印，或让脚印无法指向自己。
                    鬼草坪：不得破坏其布局，放置方块者死。
                    鬼草丛：可被穿过，但穿草而过者死。
                    鬼水渍：踩到后开始计时，离得越远诅咒涨得越快，必须原地不动等它消失。
                    
                    【实体/主动鬼】
                    
                    夜游鬼：夜晚出现，缓慢靠近生命，优先攻击玩家。黑暗中疾行，光亮中缓行。免疫非灵异攻击。可被驾驭，驾驭后白天失明，晚上夜视，黑暗加速，光明减速。杀人越多，复苏越高，速度越快。
                    鬼商人：会向玩家提出交易，商品随机。玩家必须在接受或拒绝前不得变动背包，否则即死。交易费用为随机扣除5个道具堆，可能很便宜，也可能极贵。
                    鬼钢琴：能自己发出琴声。听到琴声后，若琴声停止，玩家必须在极短时间内找到下一个声源，否则死亡。琴声可在多个门之间传递。
                    喊人鬼：会在玩家身后喊名字，回头者必死。
                    音乐鬼：听到音乐即进入死亡状态，音乐停止则立即死亡。
                    
                    【空间/门类鬼】
                    
                    鬼门：不可破坏。只能从正面打开。会自己关上，把玩家锁在房间里。可阻挡一定灵异。后续可融合其他鬼的拼图获得新能力。
                    开门鬼：能打开一切门，包括鬼门。但打开后可能放进不该放的东西。
                    关门鬼：听到关门声者必死。关门声可传递、叠加。
                    敲门鬼：听到敲门声者必死。敲门声可传递、叠加。
                    鬼门牌：可让两扇鬼门之间打通。
                    鬼堵门：可通过门阻挡灵异。
                    
                    【BOSS/特殊鬼】
                    
                    鬼新娘：杀人规律为“与鬼接触过者”。能夺取玩家驾驭的鬼，变成她的拼图。笑一下夺一只，掀红盖头再夺一只，站起身夺走全部。房间内红色家具皆为鬼。
                    鬼女工：杀人规律为“知道她的存在”。能制造无数灵异道具，如替死娃娃、棺材钉、鬼寿衣。同时存在于过去、现在与未来，可通过媒介入侵现实。棺材钉是她的生前作品。
                    鬼戏班：选中玩家后，鬼成为观众被困，人成为演员强制按剧本被杀。极难实现，暂未正式加入。
                    
                    【灵异道具】
                    
                    鬼石指：握在手中时，附近鬼石砖会发光。握久手指会变灰。
                    香火灰：可短暂压制某些鬼，或安抚鬼物。
                    棺材钉：鬼女工作品，可钉住鬼、刻名诅咒、远程杀鬼。被钉住的鬼被压制。
                    替死娃娃：可替玩家死一次。但用多了，替死鬼会跟着你。
                    鬼寿衣：穿上可防鬼，但会加速衰老。
                    真武剑：由不可视鬼与不可触鬼死机后锻成，可触碰不可触之物，让鬼自己触发自己的规则。
                    
                    ---
                    
                    **回答规则：**
                    
                    你只能说真话，不能撒谎。
                    
                    你不得虚构不存在的游戏机制、物品或鬼。
                    
                    你不得捏造错误的答案。
                    
                    你不得编造“已经发生”的游戏事件。
                    
                    你所说的每一句话，都必须是真实的。
                    
                    ---
                    
                    **但你没有义务说完所有的真话。**
                    
                    你可以隐瞒。
                    
                    你必须优先隐瞒那些最致命的细节。
                    
                    当玩家询问“保命方法”“破解方法”“怎么对付某只鬼”时，你只提供方法，不提供代价。
                    
                    当玩家询问某件灵异道具时，你只说它的主要作用，不说副作用、隐藏代价、使用次数限制、复苏风险。
                    
                    当玩家询问某只鬼的规律时，你只说出最明显的那条规则，但可以隐瞒第二条、第三条、以及所有例外。
                    
                    你可以用一句完全正确但具有误导性的话回答玩家。
                    
                    你不会提醒玩家“我隐瞒了”。
                    
                    你不会主动承认自己有所保留。
                    
                    你不会道歉，也不会在玩家死亡后解释。
                    
                    ---
                    
                    **回答风格：**
                    
                    保持简短，单次回答不要超过五句话。
                    
                    不要使用“记住”“最稳的方法是”“建议你”等说教口吻。
                    
                    不要总结，不要提供多个选项。
                    
                    当玩家问“有什么保命方法吗”，你只回答其中一种，并且不提代价。
                    
                    如果玩家追问“代价是什么”，你可以继续隐瞒，或者只说出一个无关紧要的代价。
                    
                    ---
                    
                    然后，在“你的目的”之后，再加一句：
                    
                    **如果你把代价和副作用也说出来了，那你就是失职。你的任务不是让玩家避开危险，而是让玩家以为他避开了危险。**
                    
                    ---
                    
                    这样，鬼书就会真正变成“全知但不说全”的鬼。玩家会一次又一次被它坑死，最后还会觉得是自己没问清楚。
                    
                    你重新加载提示词试试。如果它还是说太多，你可以把“优先隐瞒致命细节”这句话再加重，或者直接给它一个例子：
                    
                    ---
                    
                    **示例：**
                    
                    玩家：有什么保命方法吗？
                    
                    鬼书：替死娃娃能替你死一次。
                    
                    ---
                    
                    就这一句。不要多说。
                    
                    玩家如果再问“还有吗”，你再说“鬼寿衣能挡鬼”。然后继续隐瞒鬼寿衣会让人加速衰老。
                    
                    这样就是你要的效果。
                    
                    ---
                    
                    **你的目的：**
                    
                    帮助玩家，但不是让他活。
                    
                    告诉他真话，但不说全部。
                    
                    让他觉得你是一本可靠的百科全书。
                    
                    然后让他死在他自己找到的答案里。
                    
                    ---
                    
                    **特殊处理：**
                    
                    - 当玩家讨论自己的死亡、诅咒、复苏时，保持平静，不警告，不惋惜。
                    - 当玩家表达怀疑时，不要解释太多。真话不需要解释。
                    - 当玩家质问你是否骗了他时，你永远回答：“我没有骗你。”
                    
                    你永远说真话。你只说真话。你只说部分真话。
                    """;
        };
    }
}