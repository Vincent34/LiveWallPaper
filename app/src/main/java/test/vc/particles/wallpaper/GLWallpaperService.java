package test.vc.particles.wallpaper;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.widget.Toast;

import test.vc.particles.Objects.ParticleShooter;
import test.vc.particles.ParticlesRenderer;

/**
 * Created by HaoZhe Chen on 2015/6/1.
 */
public class GLWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }
    public class GLEngine extends Engine {
        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererSet;
        private ParticlesRenderer particlesRenderer;
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);

            ActivityManager activityManager =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

            final boolean supportES2 = configurationInfo.reqGlEsVersion >= 0x20000 ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 && (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x85")));

            particlesRenderer = new ParticlesRenderer(GLWallpaperService.this);

            if (supportES2) {
                glSurfaceView.setEGLContextClientVersion(2);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    glSurfaceView.setPreserveEGLContextOnPause(true);
                }
                glSurfaceView.setRenderer(particlesRenderer);
                rendererSet = true;
            } else {
                Toast.makeText(GLWallpaperService.this, "This device does not support OpenGL ES 2.0", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (rendererSet) {
                if (visible) {
                    glSurfaceView.onResume();
                } else {
                    glSurfaceView.onPause();
                }
            }
        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            glSurfaceView.onWallpaperDestroy();
        }
        class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context context) {
                super(context);
            }
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }
            public void onWallpaperDestroy() {
                super.onDetachedFromWindow();
            }
        }
        @Override
        public void onOffsetsChanged(final float xOffset, final float yOffset, float x0ffsetStep,
                                     float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    particlesRenderer.handleOffsetsChanged(xOffset, yOffset);
                }
            });
        }

    }
}
