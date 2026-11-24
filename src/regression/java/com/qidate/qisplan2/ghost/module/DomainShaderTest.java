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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.BufferUtils;
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
            for(String name:new String[]{"ghost_eye","ghost_mist","ghost_umbrella","ghost_isolation_region","ghost_isolation_stencil"}) {
                int vertex=compile(GL20.GL_VERTEX_SHADER,Files.readString(core.resolve((name.equals("ghost_isolation_region")?"ghost_isolation_stencil":name)+".vsh")),name);
                int fragment=compile(GL20.GL_FRAGMENT_SHADER,preprocess(core,Files.readString(core.resolve(name+".fsh"))),name);
                int program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);
                GL20.glLinkProgram(program);
                if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(name+": "+GL20.glGetProgramInfoLog(program));
                GL20.glDeleteProgram(program);GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
                System.out.println(name+" GLSL compile and link passed");
            }
            verifyIsolationVisibility(core);
        } finally {GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
    /** 实际绘制六个像素并回读，验证天空、远景、区域表面和内部相机的遮罩语义。 */
    private static void verifyIsolationVisibility(Path core) {
        String vertexSource="""
                #version 150
                void main() {
                    vec2 p=vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                    gl_Position=vec4(p*2.0-1.0,0.0,1.0);
                }
                """;
        String fragmentSource="""
                #version 150
                #moj_import "qisplan2:isolation_visibility.glsl"
                out vec4 fragColor;
                void main() {
                    int test=int(gl_FragCoord.x);
                    vec3 camera=vec3(0.0),surface=vec3(20.0,0.0,0.0);
                    float depth=0.5;
                    if(test==0)depth=1.0;
                    if(test==2)surface=vec3(11.0,0.0,0.0);
                    if(test==3){camera=vec3(11.0,0.0,0.0);depth=1.0;}
                    if(test==4)surface=vec3(9.99,0.0,0.0);
                    if(test==5)surface=vec3(5.0,0.0,0.0);
                    bool visible=isolationHasVisibleSurface(camera,surface,depth,vec3(10.0,-1.0,-1.0),vec3(12.0,1.0,1.0));
                    fragColor=vec4(visible?1.0:0.0,0.0,0.0,1.0);
                }
                """;
        int vertex=compile(GL20.GL_VERTEX_SHADER,vertexSource,"visibility vertex");
        int fragment=compile(GL20.GL_FRAGMENT_SHADER,preprocess(core,fragmentSource),"visibility fragment");
        int program=GL20.glCreateProgram();int vao=GL30.glGenVertexArrays();
        try {
            GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(GL20.glGetProgramInfoLog(program));
            GL30.glBindVertexArray(vao);GL20.glUseProgram(program);GL11.glViewport(0,0,6,1);
            GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);
            var pixels=BufferUtils.createByteBuffer(24);GL11.glReadPixels(0,0,6,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
            int[] expected={0,0,255,255,255,0};
            for(int i=0;i<expected.length;i++)if(Byte.toUnsignedInt(pixels.get(i*4))!=expected[i])
                throw new AssertionError("Isolation visibility case "+i);
            System.out.println("Isolation visibility GPU pixels passed: sky, distant terrain, visible interior, inside camera, boundary and foreground");
        } finally {
            GL20.glUseProgram(0);GL30.glBindVertexArray(0);GL30.glDeleteVertexArrays(vao);
            GL20.glDeleteProgram(program);GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
        }
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
