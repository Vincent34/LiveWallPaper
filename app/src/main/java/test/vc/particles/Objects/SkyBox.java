package test.vc.particles.Objects;

import java.nio.ByteBuffer;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glDrawElements;

import test.vc.particles.data.VertexArray;
import test.vc.particles.programs.SkyboxShaderProgram;

/**
 * Created by HaoZhe Chen on 2015/5/27.
 */
public class SkyBox {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private final VertexArray vertexArray;
    private final ByteBuffer indexArray;

    public SkyBox() {
        vertexArray = new VertexArray(new float[] {
                -1, 1, 1,
                1, 1, 1,
                -1, -1, 1,
                1, -1, 1,
                -1, 1, -1,
                1, 1, -1,
                -1, -1, -1,
                1, -1, -1
        });
        indexArray = ByteBuffer.allocateDirect(6 * 6)
                .put(new byte[] {
                        // front
                        1, 3, 0,
                        0, 3, 2,
                        // back
                        4, 6, 5,
                        5, 6, 7,
                        // left
                        0, 2, 4,
                        4, 2, 6,
                        // right
                        5, 7, 1,
                        1, 7, 3,
                        // top
                        5, 1, 4,
                        4, 1, 0,
                        // bottom
                        6, 2, 7,
                        7, 2, 3
                });
        indexArray.position(0);
    }

    public void bindData(SkyboxShaderProgram skyboxShaderProgram) {
        vertexArray.setVertexAttribPointer(0,
                skyboxShaderProgram.getaPositionLocation(),
                POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_BYTE, indexArray);
    }
}
