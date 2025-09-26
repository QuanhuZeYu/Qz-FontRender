package club.heiqi.qz_fontrender.fontSystem;

import club.heiqi.qz_fontrender.Config;
import club.heiqi.qz_fontrender.fontSystem.shader.ShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class RenderTool {
    public ShaderManager shaderManager;
    public int vao, vbo, tbo, ebo;

    public RenderTool() {
        init();
    }

    public void init() {
        if (vao == 0 || vbo == 0 || ebo == 0) {
            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);

            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            // GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) null, GL15.GL_DYNAMIC_DRAW);
            GL20.glVertexAttribPointer(0,3,GL11.GL_FLOAT,false,0,0);

            tbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,tbo);
            // GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (FloatBuffer) null,GL15.GL_DYNAMIC_DRAW);
            GL20.glVertexAttribPointer(1,2,GL11.GL_FLOAT,false,0,0);

            ebo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            // GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (IntBuffer) null, GL15.GL_DYNAMIC_DRAW);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,0);
        }
        if (shaderManager == null) {
            shaderManager = new ShaderManager().loadFromJar(
                    "assets/shader/v.vert",
                    "assets/shader/f.frag",
                    null);
        }
    }

    public void render(float[] vertex, float[] uv, int[] index, int color, Vector2f textureSize, Vector4f uvInfo) {
        GL11.glDisable(GL11.GL_BLEND);
        // shaderManager.bind();
        setUniform(color, textureSize, uvInfo);

        GL30.glBindVertexArray(vao);

        // vertex
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer bufferV = BufferUtils.createFloatBuffer(vertex.length);
        bufferV.put(vertex).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,bufferV,GL15.GL_DYNAMIC_DRAW);

        // uv
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tbo);
        FloatBuffer bufferT = BufferUtils.createFloatBuffer(uv.length);
        bufferT.put(uv).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER,bufferT,GL15.GL_DYNAMIC_DRAW);

        // index
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,ebo);
        IntBuffer bufferI = BufferUtils.createIntBuffer(index.length);
        bufferI.put(index).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER,bufferI,GL15.GL_DYNAMIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL11.glDrawElements(GL11.GL_TRIANGLES, index.length, GL11.GL_UNSIGNED_INT, 0);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        // shaderManager.unbind();
        GL11.glEnable(GL11.GL_BLEND);
    }

    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    public void setUniform(int color, Vector2f textureSize, Vector4f uvInfo) {
        modelView.clear(); projection.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        modelView.flip(); projection.flip();

        float alpha = ((color >> 24) & 255) / 255f;
        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        shaderManager.setUniformM4f("modelview", new Matrix4f(modelView));
        shaderManager.setUniformM4f("projection", new Matrix4f(projection));
        shaderManager.setUniformVec4("color", new Vector4f(red, green, blue, alpha));

        // shaderManager.setUniformVec2("textureSize", textureSize);

        shaderManager.setUniformF("blurRadius", Config.blurRadius);
        shaderManager.setUniformVec2("smoothRange", new Vector2f(Config.smoothRangeMin, Config.smoothRangeMax));
        shaderManager.setUniformI("smoothSwitcher", Config.smoothSwitcher ? 1 : 0);
        shaderManager.setUniformI("sampleR", Config.sampleR);
    }
}
