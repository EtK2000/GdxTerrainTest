package com.etk2000.terrain;

import java.nio.FloatBuffer;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.BufferUtils;

class TerrainChunk {
	public final float[] heightMap;
	public final short width;
	public final short height;
	public final float[] vertices;
	public final short[] indices;

	public final int vertexSize;
	private final int positionSize = 3;

	public TerrainChunk(int width, int height, int vertexSize, FileHandle heightMap) {

		if (width * height > Short.MAX_VALUE)
			throw new IllegalArgumentException("Chunk size too big, width*height must be <= 32767");

		this.heightMap = new float[width * height];
		this.width = (short) width;
		this.height = (short) height;
		this.vertices = new float[this.heightMap.length * vertexSize];
		this.indices = new short[width * height * 6];
		this.vertexSize = vertexSize;

		buildHeightmap(heightMap);

		buildIndices();
		buildVertices();

		calcNormals(indices, vertices);
	}

	public void buildHeightmap(FileHandle hMap) {
		Pixmap heightmapImage = new Pixmap(hMap);
		Color color = new Color();
		int idh = 0;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Color.rgba8888ToColor(color, heightmapImage.getPixel(x, y));
				heightMap[idh++] = color.r;
			}
		}
	}

	public static final int scale = 1;// default was 10
	private static final int heightScale = 10;// default was 10

	public void buildVertices() {
		int idx = 0;
		int hIdx = 0;

		for (int z = 0; z < height; z++) {
			for (int x = 0; x < width; x++) {
				// POSITION
				vertices[idx++] = scale * x;
				vertices[idx++] = heightMap[hIdx++] * heightScale * -1;
				vertices[idx++] = scale * z;

				// NORMAL, skip these for now
				idx += 3;

				// COLOR
				vertices[idx++] = Color.WHITE.toFloatBits();

				// TEXTURE
				vertices[idx++] = (x / (float) width);
				vertices[idx++] = (z / (float) height);
			}
		}
	}

	private void buildIndices() {
		int idx = 0;
		short pitch = (short) width;
		short i1 = 0;
		short i2 = 1;
		short i3 = (short) (1 + pitch);
		short i4 = pitch;

		short row = 0;

		for (int z = 0; z < (height - 1); z++) {
			for (int x = 0; x < (width - 1); x++) {
				indices[idx++] = i1;
				indices[idx++] = i2;
				indices[idx++] = i3;

				indices[idx++] = i3;
				indices[idx++] = i4;
				indices[idx++] = i1;

				i1++;
				i2++;
				i3++;
				i4++;
			}

			row += pitch;
			i1 = row;
			i2 = (short) (row + 1);
			i3 = (short) (i2 + pitch);
			i4 = (short) (row + pitch);
		}
	}

	// Gets the index of the first float of a normal for a specific vertex
	private int getNormalStart(int vertIndex) {
		return vertIndex * vertexSize + positionSize;
	}

	// Gets the index of the first float of a specific vertex
	private int getPositionStart(int vertIndex) {
		return vertIndex * vertexSize;
	}

	// Adds the provided value to the normal
	private void addNormal(int vertIndex, float[] verts, float x, float y, float z) {
		int i = getNormalStart(vertIndex);

		verts[i] += x;
		verts[i + 1] += y;
		verts[i + 2] += z;
	}

	/*
	 * Normalizes normals
	 */
	private void normalizeNormal(int vertIndex, float[] verts) {
		int i = getNormalStart(vertIndex);

		float x = verts[i];
		float y = verts[i + 1];
		float z = verts[i + 2];

		float num2 = ((x * x) + (y * y)) + (z * z);
		float num = 1f / (float) Math.sqrt(num2);
		x *= num;
		y *= num;
		z *= num;

		verts[i] = x;
		verts[i + 1] = y;
		verts[i + 2] = z;
	}

	/*
	 * Calculates the normals
	 */
	private void calcNormals(short[] indices, float[] verts) {
		for (int i = 0; i < indices.length; i += 3) {
			int i1 = getPositionStart(indices[i]);
			int i2 = getPositionStart(indices[i + 1]);
			int i3 = getPositionStart(indices[i + 2]);

			// p1
			float x1 = verts[i1];
			float y1 = verts[i1 + 1];
			float z1 = verts[i1 + 2];

			// p2
			float x2 = verts[i2];
			float y2 = verts[i2 + 1];
			float z2 = verts[i2 + 2];

			// p3
			float x3 = verts[i3];
			float y3 = verts[i3 + 1];
			float z3 = verts[i3 + 2];

			// u = p3 - p1
			float ux = x3 - x1;
			float uy = y3 - y1;
			float uz = z3 - z1;

			// v = p2 - p1
			float vx = x2 - x1;
			float vy = y2 - y1;
			float vz = z2 - z1;

			// n = cross(v, u)
			float nx = (vy * uz) - (vz * uy);
			float ny = (vz * ux) - (vx * uz);
			float nz = (vx * uy) - (vy * ux);

			// normalize(n)
			float num2 = ((nx * nx) + (ny * ny)) + (nz * nz);
			float num = 1f / (float) Math.sqrt(num2);
			nx *= num;
			ny *= num;
			nz *= num;

			addNormal(indices[i], verts, nx, ny, nz);
			addNormal(indices[i + 1], verts, nx, ny, nz);
			addNormal(indices[i + 2], verts, nx, ny, nz);
		}

		for (int i = 0; i < (verts.length / vertexSize); i++)
			normalizeNormal(i, verts);
	}

	public short getWidth() {
		return width;
	}

	public short getHeight() {
		return height;
	}

	public FloatBuffer getHeightfieldData() {
		FloatBuffer res = BufferUtils.newFloatBuffer(width * height);
		for (int i = 0; i < res.capacity(); i++)
			res.put(heightMap[i] * heightScale * -1);
		return res;
	}

}