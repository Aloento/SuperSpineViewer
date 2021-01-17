package com.esotericsoftware.spine36.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class TwoColorPolygonBatch {
    private final Mesh mesh;
    private final float[] vertices;
    private final short[] triangles;
    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();
    private final ShaderProgram defaultShader;
    private ShaderProgram shader;
    private int vertexIndex, triangleIndex;
    private Texture lastTexture;
    private boolean drawing;
    private int blendSrcFunc = GL20.GL_SRC_ALPHA;
    private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
    private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
    private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;
    private boolean premultipliedAlpha;

    public TwoColorPolygonBatch(int size) {
        this(size, size * 2);
    }

    public TwoColorPolygonBatch(int maxVertices, int maxTriangles) {
        if (maxVertices > 32767)
            throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxTriangles);

        VertexDataType vertexDataType = VertexDataType.VertexArray;
        if (Gdx.gl30 != null) vertexDataType = VertexDataType.VertexBufferObjectWithVAO;
        mesh = new Mesh(vertexDataType, false, maxVertices, maxTriangles * 3,
                new VertexAttribute(Usage.Position, 2, "a_position"),
                new VertexAttribute(Usage.ColorPacked, 4, "a_light"),
                new VertexAttribute(Usage.ColorPacked, 4, "a_dark"),
                new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord0"));

        vertices = new float[maxVertices * 6];
        triangles = new short[maxTriangles * 3];
        defaultShader = createDefaultShader();
        shader = defaultShader;
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void begin() {
        if (drawing) throw new IllegalStateException("end must be called before begin.");
        Gdx.gl.glDepthMask(false);
        shader.bind();
        setupMatrices();
        drawing = true;
    }

    public void end() {
        if (!drawing) throw new IllegalStateException("begin must be called before end.");
        if (vertexIndex > 0) flush();
        shader.end();
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        lastTexture = null;
        drawing = false;
    }

    public void draw(Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount, short[] polygonTriangles,
                     int trianglesOffset, int trianglesCount) {
        if (!drawing) throw new IllegalStateException("begin must be called before draw.");

        final short[] triangles = this.triangles;
        final float[] vertices = this.vertices;

        if (texture != lastTexture) {
            flush();
            lastTexture = texture;
        } else if (triangleIndex + trianglesCount > triangles.length || vertexIndex + verticesCount > vertices.length)
            flush();

        int triangleIndex = this.triangleIndex;
        final int vertexIndex = this.vertexIndex;
        final int startVertex = vertexIndex / 6;

        for (int i = trianglesOffset, n = i + trianglesCount; i < n; i++)
            triangles[triangleIndex++] = (short) (polygonTriangles[i] + startVertex);
        this.triangleIndex = triangleIndex;

        System.arraycopy(polygonVertices, verticesOffset, vertices, vertexIndex, verticesCount);
        this.vertexIndex += verticesCount;
    }

    public void flush() {
        if (vertexIndex == 0) return;

        lastTexture.bind();
        Mesh mesh = this.mesh;
        mesh.setVertices(vertices, 0, vertexIndex);
        mesh.setIndices(triangles, 0, triangleIndex);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        if (blendSrcFunc != -1)
            Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, triangleIndex);

        vertexIndex = 0;
        triangleIndex = 0;
    }

    public void dispose() {
        mesh.dispose();
        shader.dispose();
    }

    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    
    public void setProjectionMatrix(Matrix4 projection) {
        if (drawing) flush();
        projectionMatrix.set(projection);
        if (drawing) setupMatrices();
    }

    public Matrix4 getTransformMatrix() {
        return transformMatrix;
    }

    
    public void setTransformMatrix(Matrix4 transform) {
        if (drawing) flush();
        transformMatrix.set(transform);
        if (drawing) setupMatrices();
    }

    
    public void setPremultipliedAlpha(boolean premultipliedAlpha) {
        if (this.premultipliedAlpha == premultipliedAlpha) return;
        if (drawing) flush();
        this.premultipliedAlpha = premultipliedAlpha;
        if (drawing) setupMatrices();
    }

    private void setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        shader.setUniformf("u_pma", premultipliedAlpha ? 1 : 0);
        shader.setUniformMatrix("u_projTrans", combinedMatrix);
        shader.setUniformi("u_texture", 0);
    }

    
    public void setShader(ShaderProgram newShader) {
        if (shader == newShader) return;
        if (drawing) {
            flush();
            shader.end();
        }
        shader = newShader == null ? defaultShader : newShader;
        if (drawing) {
            shader.bind();
            setupMatrices();
        }
    }

    
    public void setBlendFunction(int srcFunc, int dstFunc) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
    }

    
    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
                && blendDstFuncAlpha == dstFuncAlpha) return;
        flush();
        blendSrcFunc = srcFuncColor;
        blendDstFunc = dstFuncColor;
        blendSrcFuncAlpha = srcFuncAlpha;
        blendDstFuncAlpha = dstFuncAlpha;
    }

    private ShaderProgram createDefaultShader() {
        String vertexShader = """
                attribute vec4 a_position;
                attribute vec4 a_light;
                attribute vec4 a_dark;
                attribute vec2 a_texCoord0;
                uniform mat4 u_projTrans;
                varying vec4 v_light;
                varying vec4 v_dark;
                varying vec2 v_texCoords;

                void main()
                {
                  v_light = a_light;
                  v_light.a = v_light.a * (255.0/254.0);
                  v_dark = a_dark;
                  v_texCoords = a_texCoord0;
                  gl_Position = u_projTrans * a_position;
                }
                """;

        String fragmentShader = """
                #ifdef GL_ES
                #define LOWP lowp
                precision mediump float;
                #else
                #define LOWP\s
                #endif
                varying LOWP vec4 v_light;
                varying LOWP vec4 v_dark;
                uniform float u_pma;
                varying vec2 v_texCoords;
                uniform sampler2D u_texture;
                void main()
                {
                  vec4 texColor = texture2D(u_texture, v_texCoords);
                  gl_FragColor.a = texColor.a * v_light.a;
                  gl_FragColor.rgb = ((texColor.a - 1.0) * u_pma + 1.0 - texColor.rgb) * v_dark.rgb + texColor.rgb * v_light.rgb;
                }""";

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled())
            throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }
}
