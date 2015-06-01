package test.vc.particles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import test.vc.particles.Objects.HeightMap;
import test.vc.particles.Objects.ParticleShooter;
import test.vc.particles.Objects.ParticleSystem;
import test.vc.particles.Objects.SkyBox;
import test.vc.particles.programs.HeightmapShaderProgram;
import test.vc.particles.programs.ParticleShaderProgram;
import test.vc.particles.programs.SkyboxShaderProgram;
import test.vc.particles.util.Geometry.Point;
import test.vc.particles.util.Geometry.Vector;
import test.vc.particles.util.LoggerConfig;
import test.vc.particles.util.TextureHelper;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_LESS;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;

/**
 * Created by HaoZhe Chen on 2015/4/4.
 * The renderer for the game.
 */
public class ParticlesRenderer implements GLSurfaceView.Renderer {

    private final static String TAG = "ParticlesRenderer";

    private final Context context;

    private int particletexture;

    private long frameStartTimeMs;
    private long startTimeMs;
    private int frameCount;

    private final float[] modelMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrixForSkybox = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] it_modelViewMatrix = new float[16];

    private float xRotation, yRotation;
    private float xOffset, yOffset;

    private ParticleSystem particleSystem;
    private ParticleShaderProgram particleShaderProgram;
    private ParticleShooter redParticleShooter;
    private ParticleShooter greenParticleShooter;
    private ParticleShooter blueParticleShooter;
    private long globalStartTime;
    private SkyboxShaderProgram skyboxShaderProgram;
    private SkyBox skyBox;
    private int skyboxTexture;
    private HeightmapShaderProgram heightmapShaderProgram;
    private HeightMap heightMap;

    final float[] vectorToLight = {0.30f, 0.35f, -0.89f, 0f};
//    private final Vector vectorToLight = new Vector(0.61f, 0.64f, -0.47f).normalize();
    private final float[] pointLightPositions = new float[]{
            -1f, 1f, 0f, 1f,
            0f, 1f, 0f, 1f,
            1f, 1f, 0f, 1f};
    private final float[] getPointLightColors = new float[]{
            1.00f, 0.20f, 0.02f,
            0.02f, 0.25f, 0.02f,
            0.02f, 0.20f, 1.00f
    };

    public ParticlesRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        particleShaderProgram = new ParticleShaderProgram(context);
        particleSystem = new ParticleSystem(10000);
        globalStartTime = System.nanoTime();

        final Vector particleDirection = new Vector(0f, 0.5f, 0f);
        final float angleVarianceInDegrees = 5f;
        final float speedVariance = 1f;

        redParticleShooter = new ParticleShooter(
                new Point(-1f, 0f, 0f),
                particleDirection,
                Color.rgb(255, 50, 5),
                angleVarianceInDegrees,
                speedVariance);
        greenParticleShooter = new ParticleShooter(
                new Point(0f, 0f, 0f),
                particleDirection,
                Color.rgb(25, 255, 25),
                angleVarianceInDegrees,
                speedVariance);
        blueParticleShooter = new ParticleShooter(
                new Point(1f, 0f, 0f),
                particleDirection,
                Color.rgb(5, 50, 255),
                angleVarianceInDegrees,
                speedVariance);
        particletexture = TextureHelper.loadTexture(context, R.drawable.particle_texture);

        skyboxShaderProgram = new SkyboxShaderProgram(context);
        skyBox = new SkyBox();
        skyboxTexture = TextureHelper.loadCubeMap(context,
                new int[] { R.drawable.night_left, R.drawable.night_right,
                        R.drawable.night_back, R.drawable.night_top,
                        R.drawable.night_back, R.drawable.night_back});

        heightmapShaderProgram = new HeightmapShaderProgram(context);
        // 设置非缩放
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        // 解码纹理文件
        final Bitmap bitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.heightmap00, options);
        heightMap = new HeightMap(bitmap);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        glViewport(0, 0, width, height);

        Matrix.perspectiveM(projectionMatrix, 0, 45, (float) width / (float) height, 1f, 100f);
        updateViewMatrices();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        limitFrameRate(24);
        logFrameRate();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;

//        float angle = (currentTime * 15) / 360 * 2f * (float)Math.PI;
//
//        Matrix.setLookAtM(viewMatrix, 0, 1f * FloatMath.cos(angle), 0.5f, 1f * FloatMath.sin(angle),
//                0f, 0f, 0f, 0f, 1f, 0f);
//        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        drawHeightmap();
        drawSkybox();
        drawParticles(currentTime);
    }

    private void limitFrameRate(int framesPerSecond) {
        long elapsedFrameTimeMs = SystemClock.elapsedRealtime() - frameStartTimeMs;
        long expectedFrameTimeMs = 1000 / framesPerSecond;
        long timeToSleepMs = expectedFrameTimeMs - elapsedFrameTimeMs;
        if (timeToSleepMs > 0) {
            SystemClock.sleep(timeToSleepMs);
        }
        frameStartTimeMs = SystemClock.elapsedRealtime();
    }

    private void logFrameRate() {
        if (LoggerConfig.ON) {
            long elapsedRealTimeMs = SystemClock.elapsedRealtime();
            double elapsedSeconds = (elapsedRealTimeMs - startTimeMs) / 1000.0;
            if (elapsedSeconds >= 1.0) {
                Log.v(TAG, frameCount / elapsedSeconds + "fps");
                startTimeMs = SystemClock.elapsedRealtime();
                frameCount = 0;
            }
            frameCount++;
        }
    }

    public void updateViewMatrices() {
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        Matrix.rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.length);
        Matrix.translateM(viewMatrix, 0, 0 - xOffset, -1.5f - yOffset, -5f);
    }

    private void updateMvpMatrix() {
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.invertM(tempMatrix, 0, modelViewMatrix, 0);
        Matrix.transposeM(it_modelViewMatrix, 0, tempMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
    }

    private void updateMvpMatrixForSkybox() {
        Matrix.multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }

    public void drawSkybox() {
        Matrix.setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();

        glDepthFunc(GL_LEQUAL);
        skyboxShaderProgram.useProgram();
        skyboxShaderProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture);
        skyBox.bindData(skyboxShaderProgram);
        skyBox.draw();
        glDepthFunc(GL_LESS);
    }

    public void drawParticles(float currentTime) {
        Matrix.setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        redParticleShooter.addParticles(particleSystem, currentTime, 3);
        greenParticleShooter.addParticles(particleSystem, currentTime, 3);
        blueParticleShooter.addParticles(particleSystem, currentTime, 3);

        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);

        particleShaderProgram.useProgram();
        particleShaderProgram.setUniforms(modelViewProjectionMatrix, currentTime, particletexture);
        particleSystem.bindData(particleShaderProgram);
        particleSystem.draw();

        glDisable(GL_BLEND);
        glDepthMask(true);
    }

    public void drawHeightmap() {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.scaleM(modelMatrix, 0, 100f, 10f, 100f);
        updateMvpMatrix();
        heightmapShaderProgram.useProgram();
        final float[] vectorToLightInEyeSpace = new float[4];
        final float[] pointPositionsInEyeSpace = new float[12];
        Matrix.multiplyMV(vectorToLightInEyeSpace, 0, viewMatrix, 0, vectorToLight, 0);
        Matrix.multiplyMV(pointPositionsInEyeSpace, 0, viewMatrix, 0, pointLightPositions, 0);
        Matrix.multiplyMV(pointPositionsInEyeSpace, 4, viewMatrix, 0, pointLightPositions, 4);
        Matrix.multiplyMV(pointPositionsInEyeSpace, 8, viewMatrix, 0, pointLightPositions, 8);
        heightmapShaderProgram.setUniforms(modelViewMatrix, it_modelViewMatrix, modelViewProjectionMatrix,
                vectorToLightInEyeSpace, pointPositionsInEyeSpace, getPointLightColors);
//        heightmapShaderProgram.setUniforms(modelViewProjectionMatrix, vectorToLight);
        heightMap.bindData(heightmapShaderProgram);
        heightMap.draw();
    }

    public void handleTouchDrag(float deltaX, float deltaY) {
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;

        if (yRotation < -90) {
            yRotation = -90;
        } else if (yRotation > 90) {
            yRotation = 90;
        }
        updateViewMatrices();
    }

    public void handleOffsetsChanged(float xOffset, float yOffset) {
        this.xOffset = (xOffset - 0.5f) * 2.5f;
        this.yOffset = (yOffset - 0.5f) * 2.5f;
        updateViewMatrices();
    }
}
