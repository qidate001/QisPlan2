package com.qidate.qisplan2.client.screen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.List;

/** 客户端个人技能布局持久化；服务端仍只接受已驾驭能力，不信任此文件。 */
public final class AbilityWheelPreferences {
    private static JsonObject profiles;

    /** 工具类无需实例化。 */
    private AbilityWheelPreferences() {}

    /** 配置属于当前客户端，按玩家 UUID 隔离。 */
    private static Path path() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config/qisplan2-ability-wheel.json"); }

    /** 首次读取配置；损坏文件记录错误并保留备份以便恢复。 */
    private static void load() {
        if (profiles != null) return;
        profiles = new JsonObject();
        if (!Files.exists(path())) return;
        try { profiles = JsonParser.parseString(Files.readString(path())).getAsJsonObject(); }
        catch (Exception error) {
            QisPlan2.LOGGER.warn("Cannot load ability wheel preferences", error);
            try { Files.copy(path(), path().resolveSibling("qisplan2-ability-wheel.json.broken"), StandardCopyOption.REPLACE_EXISTING); }
            catch (Exception backupError) { QisPlan2.LOGGER.warn("Cannot back up wheel preferences", backupError); }
        }
    }

    /** 尚未配置时使用目录前十项；保存过的空槽不自动补满。 */
    public static AbilityWheelSlots current() {
        load();
        var player = Minecraft.getInstance().player;
        if (player == null) return new AbilityWheelSlots(List.of());
        String id = player.getUUID().toString();
        if (!profiles.has(id)) return new AbilityWheelSlots(GhostAbilityCatalog.all(new PlayerGhostModuleHost(player))
                .stream().limit(AbilityWheelSlots.SIZE).map(AbilityWheelSlots.Key::of).toList());
        var keys = new ArrayList<AbilityWheelSlots.Key>();
        try {
            for (var element : profiles.getAsJsonArray(id)) {
                if (keys.size() == AbilityWheelSlots.SIZE) break;
                AbilityWheelSlots.Key key = null;
                try {
                    var object = element.getAsJsonObject();
                    key = new AbilityWheelSlots.Key(ResourceLocation.parse(object.get("module").getAsString()),
                            ResourceLocation.parse(object.get("ability").getAsString()));
                } catch (RuntimeException ignored) { /* 无效项只使当前槽失效，不移动其他技能。 */ }
                keys.add(key);
            }
        } catch (RuntimeException error) { QisPlan2.LOGGER.warn("Invalid wheel profile", error); }
        return new AbilityWheelSlots(keys);
    }

    /** 原子保存；磁盘写入失败时返回失败，界面明确提示。 */
    public static boolean save(AbilityWheelSlots layout) {
        load();
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        var array = new JsonArray();
        for (var key : layout.snapshot()) {
            if (key == null) { array.add(JsonNull.INSTANCE); continue; }
            var object = new JsonObject();
            object.addProperty("module", key.module().toString());
            object.addProperty("ability", key.ability().toString());
            array.add(object);
        }
        var next = profiles.deepCopy();
        next.add(player.getUUID().toString(), array);
        try {
            Files.createDirectories(path().getParent());
            Path temporary = path().resolveSibling(path().getFileName() + ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(next));
            try { Files.move(temporary, path(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path(), StandardCopyOption.REPLACE_EXISTING); }
            profiles = next;
            return true;
        } catch (Exception error) { QisPlan2.LOGGER.warn("Cannot save wheel preferences", error); return false; }
    }
}
