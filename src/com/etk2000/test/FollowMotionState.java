package com.etk2000.test;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class FollowMotionState extends btMotionState {
	private final Matrix4 transform;
	
	public FollowMotionState(ModelInstance instance) {
		transform = instance.transform;
	}

	@Override
	public void getWorldTransform(Matrix4 worldTrans) {
		worldTrans.set(transform);
	}

	@Override
	public void setWorldTransform(Matrix4 worldTrans) {
		transform.set(worldTrans);
	}
}