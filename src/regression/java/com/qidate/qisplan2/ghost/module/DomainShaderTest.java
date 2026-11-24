package com.qidate.qisplan2.ghost.module;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.neoforged.neoforge.client.ClientHooks;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import java.nio.file.Files;
import java.nio.file.Path;

/** 使用隐藏 OpenGL 上下文编译并链接真实资源；不启动游戏或显示窗口。 */
public final class DomainShaderTest {
    private DomainShaderTest() {}
    public static void main(String[] args) throws Exception {
        if(!GLFW.glfwInit())throw new IllegalStateException("GLFW unavailable");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window=GLFW.glfwCreateWindow(16,16,"Domain shader validation",0,0);
        if(window==0)throw new IllegalStateException("OpenGL context unavailable");
        try {
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
            Path core=Path.of(args[0]);
            // 旧写法必须失败，保证测试不会再次绕开资源命名空间错误。
            boolean rejected=false;
            try { preprocess(core,"#version 150\n#moj_import \"domain_conflict.glsl\"\n"); }
            catch(UncheckedIOException expected) { rejected=true; }
            if(!rejected)throw new AssertionError("Missing namespace must not resolve to mod resources");
            for(String name:new String[]{"ghost_eye","ghost_mist","ghost_umbrella"}) {
                int vertex=compile(GL20.GL_VERTEX_SHADER,Files.readString(core.resolve(name+".vsh")),name);
                int fragment=compile(GL20.GL_FRAGMENT_SHADER,preprocess(core,Files.readString(core.resolve(name+".fsh"))),name);
                int program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);
                GL20.glLinkProgram(program);
                if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(name+": "+GL20.glGetProgramInfoLog(program));
                GL20.glDeleteProgram(program);GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
                System.out.println(name+" GLSL compile and link passed");
            }
        } finally {GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
    /** 使用游戏预处理器与 NeoForge 的实际路径解析，再按资源命名空间读取文件。 */
    private static String preprocess(Path core,String source) {
        Path assets=core.toAbsolutePath().getParent().getParent().getParent();
        return String.join("",new GlslPreprocessor() {
            private final Set<String> imported=new HashSet<>();
            @Override public String applyImport(boolean relative,String path) {
                var location=ClientHooks.getShaderImportLocation("shaders/core/",relative,path);
                if(!imported.add(location.toString()))return null;
                Path resource=assets.resolve(location.getNamespace()).resolve(location.getPath());
                try { return Files.readString(resource); }
                catch(IOException failure) { throw new UncheckedIOException("GLSL import: "+location,failure); }
            }
        }.process(source));
    }
    /** 编译失败立即输出驱动报告，避免 Java 编译掩盖 GLSL 错误。 */
    private static int compile(int type,String source,String name) {
        int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);
        if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0)throw new AssertionError(name+": "+GL20.glGetShaderInfoLog(shader));
        return shader;
    }
}
