package com.mygdx.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mygdx.Debug.Debuger;
import com.mygdx.entities.Enemy;
import com.mygdx.entities.Tower;
import com.mygdx.game.MyGame;
import com.mygdx.handlers.EnemyManager;
import com.mygdx.handlers.GameStateManager;
import com.mygdx.triggers.Path;
import com.mygdx.handlers.TowerManager;
import com.mygdx.triggers.WayPoint;

import java.util.LinkedList;

/**
 * Created by James on 2/1/2015.
 */
public class Play extends GameState
{

    private  boolean debugMode_ON = true;
    private Texture EnemyImg;
    private Texture TowerImg;
    private Enemy enemy;

    private Tower tower;
    public ShapeRenderer shapeRenderer;
    private OrthographicCamera cam;
    private Debuger debuger;
    private LinkedList<WayPoint> wayPoints;
    private LinkedList<Tower> towers;
    public EnemyManager enemyManager;
    public TowerManager towerManager;
    float TimeSinceLastSpawn = 0;
    int numNormEnemies = 0;
    private LinkedList<Path> paths;

    public Play(GameStateManager gsm)
    {
        super(gsm);

        enemyManager = new EnemyManager(0);
        paths = new LinkedList<Path>();
        wayPoints = new LinkedList<WayPoint>();
        towers = new LinkedList<Tower>();
        wayPoints.addLast(new WayPoint(0,0,"e"));
        wayPoints.addLast(new WayPoint(MyGame.V_WIDTH-32,0,"n"));
        wayPoints.addLast(new WayPoint(MyGame.V_WIDTH-32,MyGame.V_HEIGHT-32,"w"));
        wayPoints.addLast(new WayPoint(0,MyGame.V_HEIGHT-32,"s"));
        wayPoints.addLast(new WayPoint(0,0,"end"));




        cam = new OrthographicCamera();
        cam.setToOrtho(false,MyGame.V_WIDTH,MyGame.V_HEIGHT);
        shapeRenderer = new ShapeRenderer();
        EnemyImg = new Texture("EnemyDev.png");

        numNormEnemies = 3;
        enemyManager.AddEnemy(EnemyImg,3, 1,wayPoints);
        TowerImg = new Texture("DevText_Tower.png");
        tower = new Tower(TowerImg, 50, 50, 1, 2);
        towers.addLast(tower);
        towerManager = new TowerManager(towers);
        towerManager.addTower(TowerImg, 240, 50, 1, 2);
        debuger = new Debuger(wayPoints, towerManager.towers, enemyManager.enemies);

    }

    public void handleInput()
    {
    }

    public void update(float deltaTime)
    {
        TimeSinceLastSpawn = TimeSinceLastSpawn + deltaTime;

        if(TimeSinceLastSpawn > .5 && numNormEnemies > 0){
            enemyManager.AddEnemy(EnemyImg,3, 1,wayPoints);
            TimeSinceLastSpawn = 0;
            numNormEnemies--;
        }


        enemyManager.UpdateAll(deltaTime, towers);

    }

    public void render()
    {
        Gdx.gl.glClearColor(0, 0, 0, 2);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        cam.update();
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        enemyManager.RenderAll(spriteBatch);
        towerManager.RenderAll(spriteBatch);
        spriteBatch.end();

        if(debugMode_ON){
            debuger.render();
        }
    }

    public void dispose(){};

}
