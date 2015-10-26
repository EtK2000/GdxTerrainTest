package com.etk2000.terrain;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.RayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.etk2000.test.CameraInputControllerExt;
import com.etk2000.test.FollowMotionState;

public class TerrTest extends Game {
	final static short GROUND_FLAG = 1 << 8;
	final static short OBJECT_FLAG = 1 << 9;
	final static short ALL_FLAG = -1;

	private PerspectiveCamera camera;
	private CameraInputController camController;

	private Terrain2 terrain;
	private Model mod;
	private ModelInstance ball;
	private btCollisionShape ballShape;
	private btRigidBody ballObject;
	private btMotionState ballState;
	private ModelBatch modelBatch;
	private Environment environment;
	private btCollisionConfiguration collisionConfig;
	private btDispatcher dispatcher;
	private btBroadphaseInterface overlappingPairCache;
	private btDynamicsWorld world;
	private btConstraintSolver solver;
	private DebugDrawer debugDrawer;

	@Override
	public void create() {
		Bullet.init();
		terrain = new Terrain2(Gdx.files.internal("data/grass.png"), Gdx.files.internal("data/heightmap_.png"));

		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(20, -20, 20);
		camera.direction.set(3, 0, 0).sub(camera.position).nor();
		camera.up.set(new Vector3(0, -1, 0));
		camera.lookAt(10, 0, 10);
		camera.near = 0.005f;
		camera.far = 300;
		camera.update();

		camController = new CameraInputControllerExt(camera);
		camController.translateUnits *= 20;
		Gdx.input.setInputProcessor(camController);

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
		Vector3 inertia = new Vector3();
		ballShape.calculateLocalInertia(1, inertia);
		ballState = new FollowMotionState(ball);
		ballObject = new btRigidBody(1, ballState, ballShape, inertia);
		ballObject.setWorldTransform(ball.transform);
		ballObject.setFriction(0.5f);
		ballObject.setRollingFriction(0.25f);

		world.addRigidBody(terrain.getBody(), GROUND_FLAG, ALL_FLAG);
		world.addRigidBody(ballObject, OBJECT_FLAG, GROUND_FLAG);

		modelBatch = new ModelBatch();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1, -0.8f, -0.2f));
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
		world.debugDrawObject(terrain.getWorldTransform(), terrain.getShape(), new Vector3(0, 1, 0));
		debugDrawer.end();

		terrain.render(camera);

		modelBatch.begin(camera);
		modelBatch.render(ball, environment);
		modelBatch.end();
	}

	RayResultCallback rrcb;

	private void logic(float delta) {
		if (rrcb != null)
			rrcb.dispose();
		Vector3 from = new Vector3(ballObject.getWorldTransform().getTranslation(new Vector3()));
		Vector3 to = new Vector3(from.x, 1, from.z);
		world.rayTest(from, to, rrcb = new ClosestRayResultCallback(Vector3.Zero, Vector3.Y)); // m_btWorld
																								// is
																								// btDiscreteDynamicsWorld

		if (rrcb.hasHit() && rrcb.getClosestHitFraction() < 0.2) {
			ballObject.applyCentralImpulse(new Vector3(10, 0, 0));
		}
		world.stepSimulation(1 / 60f, 10);
		System.out.println("x: " + ballObject.getLinearVelocity().x + "; y: " + ballObject.getLinearVelocity().y);
		ballObject.activate();
		//if (ballObject.getLinearVelocity().y == 0f)
		//	ballObject.applyCentralImpulse(new Vector3(100, 0, 0));
		if (ballObject.getWorldTransform().getTranslation(new Vector3()).y > 0)
			ballObject.setWorldTransform(new Matrix4(new Vector3(10, -20, 10), new Quaternion(), new Vector3(1, 1, 1)));
	}

	@Override
	public void dispose() {
		terrain.dispose();
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