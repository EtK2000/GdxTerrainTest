package com.etk2000.terrain;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btHeightfieldTerrainShape;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.etk2000.test.CameraInputControllerExt;
import com.etk2000.test.FollowMotionState;

public class Terra extends Game {
	private PerspectiveCamera camera;
	private CameraInputController camController;

	private TerrainChunk chunk;
	private Mesh mesh;

	private ShaderProgram shader;
	private Texture terrainTexture;

	private final Matrix3 normalMatrix = new Matrix3();

	private static final float[] lightPosition = { 5, 35, 5 };
	private static final float[] ambientColor = { 0.2f, 0.2f, 0.2f, 1.0f };
	private static final float[] diffuseColor = { 0.5f, 0.5f, 0.5f, 1.0f };
	private static final float[] specularColor = { 0.7f, 0.7f, 0.7f, 1.0f };

	private static final float[] fogColor = { 0.2f, 0.1f, 0.6f, 1.0f };

	private Matrix4 model = new Matrix4();
	private Matrix4 modelView = new Matrix4();

	private final String vertexShader = //
	"attribute vec4 a_position; \n" + //
			"attribute vec3 a_normal; \n" + //
			"attribute vec2 a_texCoord; \n" + //
			"attribute vec4 a_color; \n" + //
	//
	"uniform mat4 u_MVPMatrix; \n" + //
			"uniform mat3 u_normalMatrix; \n" + //
	//
	"uniform vec3 u_lightPosition; \n" + //
	//
	"varying float intensity; \n" + //
			"varying vec2 texCoords; \n" + //
			"varying vec4 v_color; \n" + //
	//
	"void main() { \n" + //
			"    vec3 normal = normalize(u_normalMatrix * a_normal); \n" + //
			"    vec3 light = normalize(u_lightPosition); \n" + //
			"    intensity = max( dot(normal, light) , 0.0); \n" + //
	//
	"    v_color = a_color; \n" + //
			"    texCoords = a_texCoord; \n" + //
	//
	"    gl_Position = u_MVPMatrix * a_position; \n" + //
			"}";//

	private final String fragmentShader = //
	"#ifdef GL_ES \n" + //
			"precision mediump float; \n" + //
			"#endif \n" + //
	//
	"uniform vec4 u_ambientColor; \n" + //
			"uniform vec4 u_diffuseColor; \n" + //
			"uniform vec4 u_specularColor; \n" + //
	//
	"uniform sampler2D u_texture; \n" + //
			"varying vec2 texCoords; \n" + //
			"varying vec4 v_color; \n" + //
	//
	"varying float intensity; \n" + //
	//
	"void main() { \n" + //
			"    gl_FragColor = v_color * intensity * texture2D(u_texture, texCoords); \n" + //
			"}";

	Model mod;
	ModelInstance ball;
	btCollisionShape ballShape;
	btRigidBody ballObject;
	btMotionState ballState;
	btCollisionShape terraShape;
	btRigidBody terraObject;
	btMotionState terraState;
	btDynamicsWorld world;
	ModelBatch modelBatch;
	Environment environment;
	btCollisionConfiguration collisionConfig;
	btDispatcher dispatcher;
	btBroadphaseInterface overlappingPairCache;
	btConstraintSolver solver;
	DebugDrawer debugDrawer;
	Matrix4 terraPos = new Matrix4(new Vector3(15.5f, 0.5f, 15.5f), new Quaternion(), new Vector3(1, 1, 1));

	@Override
	public void create() {
		// Terrain texture size is 128x128
		terrainTexture = new Texture(Gdx.files.internal("data/concrete2.png"));

		// position, normal, color, texture
		int vertexSize = 3 + 3 + 1 + 2;

		// Height map (black/white) texture size is 32x32
		chunk = new TerrainChunk(32, 32, vertexSize, Gdx.files.internal("data/heightmap.png"));

		mesh = new Mesh(true, chunk.vertices.length / 3, chunk.indices.length,
				new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
				new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
				new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE));

		mesh.setVertices(chunk.vertices);
		mesh.setIndices(chunk.indices);

		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(5, 50, 5);
		camera.direction.set(3, 0, 0).sub(camera.position).nor();
		camera.near = 0.005f;
		camera.far = 300;
		camera.update();

		camController = new CameraInputControllerExt(camera);
		camController.translateUnits *= 20;
		Gdx.input.setInputProcessor(camController);

		ShaderProgram.pedantic = false;

		shader = new ShaderProgram(vertexShader, fragmentShader);

		Bullet.init();
		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		overlappingPairCache = new btDbvtBroadphase();
		solver = new btSequentialImpulseConstraintSolver();
		world = new btDiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver, collisionConfig);
		world.setGravity(new Vector3(0, 9.8f, 0));
		debugDrawer = new DebugDrawer();
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
		world.setDebugDrawer(debugDrawer);

		ModelBuilder mb = new ModelBuilder();
		mb.begin();
		mb.node().id = "ball";
		mb.part("sphere", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
				new Material(ColorAttribute.createDiffuse(Color.GREEN))).sphere(1f, 1f, 1f, 10, 10);
		mod = mb.end();

		ball = new ModelInstance(mod, "ball");
		ball.transform.setToTranslation(5, -20, 5);
		ballShape = new btSphereShape(0.5f);
		ballState = new FollowMotionState(ball);
		ballObject = new btRigidBody(1, ballState, ballShape);
		ballObject.setWorldTransform(ball.transform);

		terraShape = new btHeightfieldTerrainShape(chunk.getWidth(), chunk.getHeight(), chunk.getHeightfieldData(), 1,
				0, 1, 1, true);
		terraState = new btDefaultMotionState(
				new Matrix4(new Vector3(0, -1, 0), new Quaternion(0, 0, 0, 1), new Vector3(1, 1, 1)));
		terraObject = new btRigidBody(0, terraState, terraShape, new Vector3());
		terraObject.setWorldTransform(terraPos);

		world.addRigidBody(ballObject);
		world.addRigidBody(terraObject);

		modelBatch = new ModelBatch();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
	}

	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		logic(Gdx.graphics.getRawDeltaTime());

		camController.update();
		camera.update();

		debugDrawer.begin(camera);
		world.debugDrawObject(ballObject.getWorldTransform(), ballShape, new Vector3(0, 0, 1));
		world.debugDrawObject(terraObject.getWorldTransform(), terraShape, new Vector3(0, 1, 0));
		debugDrawer.end();

		// This is wrong?
		model.setToRotation(new Vector3(0, 1, 0), 45f);
		modelView.set(camera.view).mul(model);

		terrainTexture.bind();

		shader.begin();

		shader.setUniformMatrix("u_MVPMatrix", camera.combined);
		shader.setUniformMatrix("u_normalMatrix", normalMatrix.set(modelView).inv().transpose());

		shader.setUniform3fv("u_lightPosition", lightPosition, 0, 3);
		shader.setUniform4fv("u_ambientColor", ambientColor, 0, 4);
		shader.setUniform4fv("u_diffuseColor", diffuseColor, 0, 4);
		shader.setUniform4fv("u_specularColor", specularColor, 0, 4);

		shader.setUniformi("u_texture", 0);

		mesh.render(shader, GL20.GL_TRIANGLES);

		shader.end();

		modelBatch.begin(camera);
		modelBatch.render(ball, environment);
		modelBatch.end();
	}

	float t = 0;

	private void logic(float delta) {
		world.stepSimulation(delta);

		Matrix4 trans = new Matrix4();
		Vector3 pos = new Vector3();
		ballObject.getMotionState().getWorldTransform(trans);
		trans.getTranslation(pos);

		System.out.println(pos.y);
		ball.calculateTransforms();
	}

	@Override
	public void dispose() {
		terraObject.dispose();
		terraShape.dispose();
		terraState.dispose();
		ballObject.dispose();
		ballShape.dispose();
		ballState.dispose();
		world.dispose();
		dispatcher.dispose();
		collisionConfig.dispose();
		overlappingPairCache.dispose();
		solver.dispose();
		modelBatch.dispose();
		mod.dispose();
		debugDrawer.dispose();
	}
}