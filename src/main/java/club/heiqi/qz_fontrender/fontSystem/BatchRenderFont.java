package club.heiqi.qz_fontrender.fontsystem;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BatchRenderFont {
    public ArrayList<Double> vertex = new ArrayList<>();
    public ArrayList<Float> texCoord = new ArrayList<>();
    public ArrayList<Float> color = new ArrayList<>();
    public ArrayList<Integer> index = new ArrayList<>();
    /**0=随时可渲染，1=进入世界渲染区间，2=进入GUI渲染区间*/
    public int phase = 0;


    /**键为 纹理ID 值为数据收集指令*/
    public HashMap<Integer, ArrayList<Runnable>> callRenders = new HashMap<>();

    public void collectRender(float x, float y, float charSize, CharPage page, CharInfo info, int inColor, boolean italic) {

        Runnable call = () -> {
            float u0 = (float)info.getU0(page.textureSize);
            float u1 = (float)info.getU1(page.textureSize);
            float v0 = (float)info.getV0(page.textureSize);
            float v1 = (float)info.getV1(page.textureSize);
            float alpha = (float) (inColor >> 24) / 255;
            float red = (float) ((inColor >> 16) & 255) / 255;
            float green = (float) ((inColor >> 8) & 255) / 255;
            float blue = (float) (inColor & 255) / 255;

            double[] vertex = new double[] {
                    // 左上
                    italic ? x+2 : x, y, 0.1,
                    // 左下
                    x, y+charSize, 0.1,
                    // 右下
                    x+charSize, y+charSize, 0.1,
                    // 右上
                    italic ? x+charSize+2 : x+charSize, y, 0.1
            };
            float[] texCoord = new float[] {
                    u0, v0,
                    u0, v1,
                    u1, v1,
                    u1, v0,
            };
            float[] color = new float[] {
                    red, green, blue, alpha,
                    red, green, blue, alpha,
                    red, green, blue, alpha,
                    red, green, blue, alpha,
            };
            int[] index = new int[] {
                    0,1,2, 2,3,0
            };

            int preVertexCount = this.vertex.isEmpty() ? 0 : this.vertex.size() / 3;
            for (double v : vertex) {
                this.vertex.add(v);
            }
            for (float tex : texCoord) {
                this.texCoord.add(tex);
            }
            for (float c : color) {
                this.color.add(c);
            }
            for (int i : index) {
                this.index.add(i+preVertexCount);
            }
            RenderTool.getInstance().shaderManager.setUniformVec2("textureSize", new Vector2f(page.textureSize));
        };
        callRenders.computeIfAbsent(page.textureID, k -> new ArrayList<>()).add(call);
    }

    public void flush() {
        RenderTool.getInstance().shaderManager.bind();
        clean();

        for (Map.Entry<Integer, ArrayList<Runnable>> entry : callRenders.entrySet()) {

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, entry.getKey());

            for (Runnable call : entry.getValue()) {
                call.run();
            }
            RenderTool.getInstance().render(
                    toArrayF(vertex),
                    toArrayF(texCoord),
                    toArrayF(color),
                    toArrayI(index));

            clean();
        }
        callRenders.clear();

        RenderTool.getInstance().shaderManager.unbind();
    }

    public void clean() {
        vertex.clear();
        texCoord.clear();
        color.clear();
        index.clear();
    }

    public <T extends Number>float[] toArrayF(ArrayList<T> in) {
        float[] result = new float[in.size()];
        for (int i = 0; i < in.size(); i++) {
            result[i] = in.get(i).floatValue();
        }
        return result;
    }

    public <T extends Number>int[] toArrayI(ArrayList<T> in) {
        int[] result = new int[in.size()];
        for (int i = 0; i < in.size(); i++) {
            result[i] = in.get(i).intValue();
        }
        return result;
    }
}
