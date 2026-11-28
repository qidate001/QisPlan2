package com.qidate.qisplan2.ghost.module;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.neoforged.neoforge.client.ClientHooks;
import net.minecraft.ResourceLocationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import com.qidate.qisplan2.ghost.domain.client.renderer.GhostTextureUpload;
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
            boolean duplicateRejected=false;
            try { preprocess(core,"#version 150\n#moj_import \"qisplan2:isolation_visibility.glsl\"\n",true); }
            catch(ResourceLocationException expected) { duplicateRejected=true; }
            if(!duplicateRejected)throw new AssertionError("Legacy relative import must reproduce duplicate namespace failure");
            for(String name:new String[]{"ghost_eye","ghost_mist","ghost_umbrella","ghost_isolation_region","ghost_isolation_stencil"}) {
                int vertex=compile(GL20.GL_VERTEX_SHADER,Files.readString(core.resolve((name.equals("ghost_isolation_region")?"ghost_isolation_stencil":name)+".vsh")),name);
                String fragmentSource=Files.readString(core.resolve(name+".fsh"));
                String normal=preprocess(core,fragmentSource);
                // 兼容层会给相对导入补上当前命名空间；绝对导入不应被再次改写。
                String compatible=preprocess(core,fragmentSource,true);
                if(!normal.equals(compatible))throw new AssertionError(name+": namespace completion changed shader source");
                int fragment=compile(GL20.GL_FRAGMENT_SHADER,compatible,name);
                int program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);
                GL20.glLinkProgram(program);
                if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new AssertionError(name+": "+GL20.glGetProgramInfoLog(program));
                GL20.glDeleteProgram(program);GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
                System.out.println(name+" GLSL compile and link passed");
            }
            verifyIsolationVisibility(core);
            verifyTextureUpload();
        } finally {GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
    /** 模拟图集遗留的解包状态，验证分配、增量上传和重建纹理都不会读错地址。 */
    private static void verifyTextureUpload() {
        int pbo=GL15.glGenBuffers();
        int[] names={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,
                GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SKIP_PIXELS,GL11.GL_UNPACK_SWAP_BYTES};
        int[] dirty={8,1024,17,23,1};
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);
            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER,16L,GL15.GL_STATIC_DRAW);
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],dirty[i]);
            for(int cycle=0;cycle<3;cycle++) {
                int texture=GL11.glGenTextures();
                try {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
                    for(int update=0;update<2;update++) {
                        var data=BufferUtils.createFloatBuffer(16);
                        for(int i=0;i<16;i++)data.put(i+cycle*100+update*20+0.25f);
                        data.flip();
                        GhostTextureUpload.rgba32f(2,2,data,update==0);
                        for(int i=0;i<names.length;i++)if(GL11.glGetInteger(names[i])!=dirty[i])
                            throw new AssertionError("Unpack state not restored: "+names[i]);
                        if(GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING)!=pbo)
                            throw new AssertionError("Unpack buffer binding not restored");
                        var actual=BufferUtils.createFloatBuffer(16);
                        GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_FLOAT,actual);
                        for(int i=0;i<16;i++)if(actual.get(i)!=data.get(i))
                            throw new AssertionError("Texture upload corrupted at "+i);
                        if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new AssertionError("Texture upload GL error");
                    }
                } finally {GL11.glDeleteTextures(texture);}
            }
            System.out.println("RGBA32F upload passed: dirty unpack state, PBO, allocation, subimage and recreation");
        } finally {
            for(int i=0;i<names.length;i++)GL11.glPixelStorei(names[i],i==0?4:0);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);GL15.glDeleteBuffers(pbo);
        }
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
                #moj_import <qisplan2:isolation_visibility.glsl>
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
        return preprocess(core,source,false);
    }
    /** 模拟相对导入的命名空间补全，重现日志中的重复前缀并验证公共 include 可避开它。 */
    private static String preprocess(Path core,String source,boolean completeRelativeNamespace) {
        Path assets=core.toAbsolutePath().getParent().getParent().getParent();
        return String.join("",new GlslPreprocessor() {
            private final Set<String> imported=new HashSet<>();
            @Override public String applyImport(boolean relative,String path) {
                if(completeRelativeNamespace && relative)path="qisplan2:"+path;
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
