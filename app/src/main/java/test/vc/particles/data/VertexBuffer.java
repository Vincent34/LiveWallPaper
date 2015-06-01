package test.vc.particles.data;

import android.app.ActivityManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;

import static android.opengl.GLES20.glVertexAttrib1f;
import static android.opengl.GLES20.glVertexAttribPointer;
import static test.vc.particles.Constants.BYTES_PER_FLOAT;

/**
 * Created by HaoZhe Chen on 2015/5/28.
 */
public class VertexBuffer {
    private final int bufferId;

    public VertexBuffer(float[] vertexData) {
        // Allocate a buffer;
        final int buffers[] = new int[1];
        glGenBuffers(buffers.length, buffers, 0);
        if (buffers[0] == 0) {
            throw new RuntimeException("Could not create a new vertex buffer object.");
        }
        bufferId = buffers[0];

        // Bind to the buffer
        glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);

        // Transfer data to native memory
        FloatBuffer vertexArray = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexArray.position(0);

        // Transfer data from native memory to the GPU buffer.
        glBufferData(GL_ARRAY_BUFFER, vertexArray.capacity() * BYTES_PER_FLOAT, vertexArray, GL_STATIC_DRAW);

        // Unbind from the buffer when we are done.
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void setVertexAttribPointer(int dataOffset, int attributeLocation, int componentCount, int stride) {
        glBindBuffer(GL_ARRAY_BUFFER, bufferId);
        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT, false, stride, dataOffset);
        glEnableVertexAttribArray(attributeLocation);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
}
