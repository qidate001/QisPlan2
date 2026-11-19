package com.qidate.qisplan2.ghost.module.influence;

import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** 只从实际参与的宿主收集声明；此处不引用任何具体厉鬼定义。 */
public final class GhostAttackResponses {
    /** 已绑定上下文的响应，统一排序，不依赖宿主种类。 */
    public record Response(int order, Consumer<GhostAttackContext> effect) {}

    private GhostAttackResponses() {}

    /** 收集当前承载有效的模块声明；状态和媒介仍由声明的能力入口检查。 */
    public static List<Response> collect(List<? extends GhostModuleHost> hosts) {
        var responses = new ArrayList<Response>();
        for (var host : hosts) {
            for (var entry : List.copyOf(host.getModules())) {
                if (!host.carriesModule(entry.id())) continue;
                var definition = GhostModuleRegistry.get(entry.id());
                if (definition == null) continue;
                for (var response : definition.getAttackResponses()) {
                    responses.add(new Response(response.order(), attack -> {
                        if (host.carriesModule(entry.id())
                                && host.suppression(entry.id()).canUseAbility(host.controlsIntrinsicModules()))
                            response.respond(new GhostModuleContext(host, entry.id()), attack);
                    }));
                }
            }
        }
        return responses;
    }

    /** 每层只接收前层留下的强度；完全抵挡后不调用后续响应，也不再支付代价。 */
    public static void apply(List<Response> responses, GhostAttackContext attack) {
        var ordered = new ArrayList<>(responses);
        ordered.sort(Comparator.comparingInt(Response::order));
        for (var response : ordered) {
            if (attack.event().isBlocked()) break;
            response.effect().accept(attack);
        }
    }
}
