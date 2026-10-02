package ar.axt.animar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import ar.axt.database.AdministrarDatos;
import ar.axt.nopeby.MainActivity;
import ar.axt.nopeby.MyRenderer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TreeMap;

public class ExportarVideo {
    private static final String TAG = "ExportarVideo";
    private final Context context;
    private final MyRenderer renderer;
    private final String nombreProyecto;
    private final int width;
    private final int height;
    private final int fps;
    
    private int fboId = -1;
    private int textureId = -1;
    private int depthId = -1;
    
    private boolean exportando = false;
    private int frameActual = 0;
    private float tiempoActual = 0f;
    private float tiempoFinal = 0f;
    private final float frameStep;
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExportListener listener;

    public interface ExportListener {
        void onProgress(int currentFrame, int totalFrames, String phase);
        void onFinished(String videoPath);
        void onError(String error);
    }

    public ExportarVideo(Context context, MyRenderer renderer, String nombreProyecto, int width, int height, int fps) {
        this.context = context;
        this.renderer = renderer;
        this.nombreProyecto = nombreProyecto;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.frameStep = 1.0f / fps;
    }

    public void setListener(ExportListener listener) {
        this.listener = listener;
    }

    public void iniciarExportacion() {
        if (exportando) return;
        exportando = true;
        File projectDir = getProjectDir();
        if (!projectDir.exists()) projectDir.mkdirs();
        // Calcular tiempo final
        CrearAnimaciones ca = new CrearAnimaciones(context);
        TreeMap<Integer, CrearAnimaciones.Keyframe> kfs = ca.cargarKeyframes(nombreProyecto);
        tiempoFinal = (float) ca.getUltimoSegundo(kfs);
        renderer.activity.glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {setupFBO(); procesarSiguienteFrame(kfs);} }); }

    private void setupFBO() {
        int[] buffers = new int[1];
        GLES20.glGenFramebuffers(1, buffers, 0);
        fboId = buffers[0];
        GLES20.glGenTextures(1, buffers, 0);
        textureId = buffers[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glGenRenderbuffers(1, buffers, 0);
        depthId = buffers[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthId);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthId);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Error al crear FBO: " + status);
        } GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void procesarSiguienteFrame(final TreeMap<Integer, CrearAnimaciones.Keyframe> kfs) {
        if (tiempoActual > tiempoFinal + 0.001f) {
            renderer.targetFBO = 0;
            renderer.isExporting = false;
            finalizarFase1(); return; }
        frameActual++;
        renderer.targetFBO = fboId;
        renderer.isExporting = true;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glViewport(0, 0, width, height);
        CrearAnimaciones ca = new CrearAnimaciones(context);
        ca.interpolarEscena(tiempoActual, renderer, kfs);
        renderer.onDrawFrame(null);
        saveFrameToPNG(frameActual);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        final int totalFrames = (int)(tiempoFinal / frameStep);
        if (listener != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {listener.onProgress(frameActual, totalFrames, "Capturando PNGs...");}});}
        tiempoActual += frameStep;
        renderer.activity.glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                procesarSiguienteFrame(kfs);
            }});
    }

    private void saveFrameToPNG(int frameNumber) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        buffer.asIntBuffer().get(pixels);
        int[] correctedPixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[(y * width) + x];
                int r = (pixel & 0xFF);
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;
                int correctedPixel = (0xFF << 24) | (r << 16) | (g << 8) | b;
                correctedPixels[((height - y - 1) * width) + x] = correctedPixel;
            }
        }
        bitmap.setPixels(correctedPixels, 0, width, 0, 0, width, height);
        File file = new File(getProjectDir(), "f" + frameNumber + ".png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            Log.e(TAG, "Error guardando PNG", e);
        } finally { bitmap.recycle(); }
    }

    private void finalizarFase1() {
        int[] buffers = {fboId, textureId, depthId};
        GLES20.glDeleteFramebuffers(1, buffers, 0);
        GLES20.glDeleteTextures(1, buffers, 1);
        GLES20.glDeleteRenderbuffers(1, buffers, 2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                convertirPNGaMP4();
            } }).start(); }

    private void convertirPNGaMP4() {
        File videoFile = getUniqueVideoFile(getProjectDir(), nombreProyecto);
        int totalFrames = frameActual;
        try {
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            int bitrate;
            if (width >= 3840) bitrate = 40000000;
            else if (width >= 2560) bitrate = 20000000;
            else if (width >= 1920) bitrate = 10000000;
            else if (width >= 1280) bitrate = 5000000;
            else if (width >= 854) bitrate = 2500000;
            else bitrate = 1000000;
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            MediaCodec codec = MediaCodec.createEncoderByType("video/avc");
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = codec.createInputSurface();
            codec.start();
            MediaMuxer muxer = new MediaMuxer(videoFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int trackIndex = -1;
            boolean muxerStarted = false;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            for (int i = 1; i <= totalFrames; i++) {
                if (listener != null) {
                    final int cur = i;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onProgress(cur, totalFrames, "Generando MP4...");}});}
                File pngFile = new File(getProjectDir(), "f" + i + ".png");
                if (!pngFile.exists()) continue;
                Bitmap bitmap = BitmapFactory.decodeFile(pngFile.getAbsolutePath());
                Canvas canvas = surface.lockCanvas(null);
                canvas.drawBitmap(bitmap, 0, 0, null);
                surface.unlockCanvasAndPost(canvas);
                bitmap.recycle();
                pngFile.delete();
                // Drain codec
                int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(codec.getOutputFormat());
                        muxer.start();
                        muxerStarted = true;
                    }
                    bufferInfo.presentationTimeUs = (i * 1000000L) / fps;
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                    codec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
            codec.signalEndOfInputStream();
            int lastOutputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000);
            while (lastOutputBufferIndex >= 0) {
                muxer.writeSampleData(trackIndex, outputBuffers[lastOutputBufferIndex], bufferInfo);
                codec.releaseOutputBuffer(lastOutputBufferIndex, false);
                lastOutputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
            }
            codec.stop();
            codec.release();
            muxer.stop();
            muxer.release();
            limpiarTemporales(totalFrames);
            if (listener != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFinished(videoFile.getAbsolutePath());
                    }});}
        } catch (Exception e) {
            Log.e(TAG, "Error en Fase 2", e);
            limpiarTemporales(totalFrames);
            if (listener != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(e.getMessage());
                    }
                });
            }
        } exportando = false;
    }

    private File getUniqueVideoFile(File projectDir, String baseName) {
        File file = new File(projectDir, baseName + ".mp4");
        if (!file.exists()) return file;
        int count = 1;
        while (true) {
            file = new File(projectDir, baseName + "_" + count + ".mp4");
            if (!file.exists()) return file;
            count++;
        }
    }

    private void limpiarTemporales(int total) {
        for (int i = 1; i <= total; i++) {
            File f = new File(getProjectDir(), "f" + i + ".png");
            if (f.exists()) f.delete();
        }
    }

    private File getProjectDir() {
        if (nombreProyecto == null) return new File(AdministrarDatos.getAppBaseDir(), "default/animaciones");
        return AdministrarDatos.getAnimacionesDir(nombreProyecto);
    }
}
