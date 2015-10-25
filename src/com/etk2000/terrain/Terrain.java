package com.etk2000.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btHeightfieldTerrainShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;

public class Terrain implements Disposable {
	private static final int width = 32, height = 32;
	// the terrain
	private final TerrainChunk chunk;
	private final Mesh mesh;
	private final Texture tex;

	// the shader
	private final ShaderProgram shader;
	private float[] lightPosition = new float[] { 5, 35, 5 };
	private float[] ambientColor = new float[] { 0.2f, 0.2f, 0.2f, 1 };
	private float[] diffuseColor = new float[] { 0.5f, 0.5f, 0.5f, 1 };
	private float[] specularColor = new float[] { 0.7f, 0.7f, 0.7f, 1 };
	private final Matrix3 normalMatrix = new Matrix3();
	private final Matrix4 model = new Matrix4();
	private final Matrix4 modelView = new Matrix4();

	// bullet
	private final btHeightfieldTerrainShape shape;
	private final btMotionState state;
	private final btRigidBody body;
	private final Vector3 offset = new Vector3(15.5f, 0.5f, 15.5f);

	public Terrain(FileHandle texture, FileHandle heightMap) {
		tex = new Texture(texture);
		chunk = new TerrainChunk(width, height, 9, heightMap);
		mesh = new Mesh(true, chunk.vertices.length / 3, chunk.indices.length,
				new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
				new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
				new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));
		mesh.setVertices(chunk.vertices);
		mesh.setIndices(chunk.indices);

		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(Gdx.files.internal("com/etk2000/terrain/terrain.vert"),
				Gdx.files.internal("com/etk2000/terrain/terrain.frag"));
		shape = new btHeightfieldTerrainShape(chunk.getWidth(), chunk.getHeight(), chunk.getHeightfieldData(),
				1, 0, 1, 1, true);
		state = new btDefaultMotionState();
		body = new btRigidBody(0, state, shape);

		//body.translate(new Vector3(TerrainChunk.scale * (chunk.getWidth() - 1) * 0.5f, TerrainChunk.heightScale * 0.5f,
		//		TerrainChunk.scale * (chunk.getHeight() - 1) * 0.5f));
		body.translate(offset);
	}

	@Override
	public void dispose() {
		body.dispose();
		mesh.dispose();
		shader.dispose();
		shape.dispose();
		state.dispose();
		tex.dispose();
	}

	public btRigidBody getBody() {
		return body;
	}

	public btCollisionShape getShape() {
		return shape;
	}

	public Matrix4 getWorldTransform() {
		return body.getWorldTransform();
	}

	public void render(Camera cam) {
		model.setToRotation(new Vector3(0, 1, 0), 45f);
		modelView.set(cam.view).mul(model);

		tex.bind();

		shader.begin();

		shader.setUniformMatrix("u_MVPMatrix", cam.combined);
		shader.setUniformMatrix("u_normalMatrix", normalMatrix.set(modelView).inv().transpose());

		shader.setUniform3fv("u_lightPosition", lightPosition, 0, 3);
		shader.setUniform4fv("u_ambientColor", ambientColor, 0, 4);
		shader.setUniform4fv("u_diffuseColor", diffuseColor, 0, 4);
		shader.setUniform4fv("u_specularColor", specularColor, 0, 4);

		shader.setUniformi("u_texture", 0);

		mesh.render(shader, GL20.GL_TRIANGLES);

		shader.end();
	}

	public void setAmbiantColor(Color c) {
		ambientColor = new float[] { c.r, c.g, c.b, c.a };
	}

	public void setDiffuseColor(Color c) {
		diffuseColor = new float[] { c.r, c.g, c.b, c.a };
	}

	public void setLightPosition(Vector3 pos) {
		lightPosition = new float[] { pos.x, pos.y, pos.z };
	}

	public void setSpecularColor(Color c) {
		lightPosition = new float[] { c.r, c.g, c.b, c.a };
	}

	public void translate(Vector3 by) {
		mesh.transform(new Matrix4(by, new Quaternion(), new Vector3(1, 1, 1)));
		body.translate(by);
		Vector3 pos = new Vector3();
		body.getWorldTransform().getTranslation(pos);
		// lightPosition = new float[] { pos.x, pos.y - 10, pos.z };
	}
}