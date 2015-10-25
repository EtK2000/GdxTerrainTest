package com.etk2000.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;

public class CameraInputControllerExt extends CameraInputController {
	public int leftKey = Keys.A;
	protected boolean leftPressed;
	public int rightKey = Keys.D;
	protected boolean rightPressed;

	private final Vector3 tmpV1 = new Vector3();

	public CameraInputControllerExt(Camera camera) {
		super(camera);
		rotateRightKey = Keys.Q;
		rotateLeftKey = Keys.E;
	}

	@Override
	protected boolean process(float deltaX, float deltaY, int button) {
		return super.process(-1 * deltaX, 0, button) && super.process(0, deltaY, button);
	}

	@Override
	public void update() {
		float delta = Gdx.graphics.getRawDeltaTime();
		if (leftPressed) {
			camera.translate(tmpV1.set(camera.direction).crs(Vector3.Y).scl(delta * translateUnits));
			if (forwardTarget)
				target.add(tmpV1);
		}
		if (rightPressed) {
			camera.translate(tmpV1.set(camera.direction).crs(Vector3.Y).scl(-delta * translateUnits));
			if (forwardTarget)
				target.add(tmpV1);
		}
		super.update();
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == leftKey)
			leftPressed = true;
		else if (keycode == rightKey)
			rightPressed = true;
		else
			super.keyDown(keycode);
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (keycode == leftKey)
			leftPressed = false;
		else if (keycode == rightKey)
			rightPressed = false;
		else
			super.keyUp(keycode);
		return false;
	}
}