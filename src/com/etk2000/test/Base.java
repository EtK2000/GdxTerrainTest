package com.etk2000.test;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.etk2000.terrain.TerrTest;
import com.etk2000.terrain.Terra;

public class Base/* implements ApplicationListener*/ {
	public static void main(String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Terra";
		config.width = 800;
		config.height = 480;
		new LwjglApplication(new TerrTest(), config);
		//new LwjglApplication(new Terra(), config);
	}

/*	Terrain t;

	@Override
	public void create() {
		t = new Terrain(10, 10);
		t.setTileset("outside");
		t.setFoliageSet("outside");
		t.buildSectors();
	}

	@Override
	public void dispose() {
		t.dispose();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}*/
}