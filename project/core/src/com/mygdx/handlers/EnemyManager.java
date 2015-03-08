package com.mygdx.handlers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygdx.entities.Enemy;
import com.mygdx.entities.Tower;
import com.mygdx.game.MyGame;
import com.mygdx.triggers.WayPoint;

import java.util.LinkedList;

/**
 * Created by James on 2/18/2015.
 */
public class EnemyManager
{
    protected static final int centerOffset = 16;
    protected static final int rangeOffset = 32;
    protected static final int waveInfoNorm = 20;
    protected static final int waveInfoFast = 2;
    protected static final int waveInfoHeavy = 1;

    public int currentWave = 1;
    public int numEnemies = 0;
    private int numDeadEnemies = 0;
    public int waveToBeSpawnedNorm;
    public int waveToBeSpawnedFast;
    public int waveToBeSpawnedHeavy;
    public float timeSinceLastNorm;
    public float timeSinceLastFast;
    public float timeSinceLastHeavy;
    private Texture EnemyImg = new Texture("EnemyDev.png");
    private Texture NullLayer = new Texture("nulllayer.png");
    private Texture FastEnemy = new Texture("FastEnemy.png");
    private Texture TigerBase = new Texture("tigerbase.png");
    private Texture TigerTurret = new Texture("tigerturret.png");
    public LinkedList<Enemy> enemies;
    public LinkedList<Tower> towers;
    private LinkedList<WayPoint> path;

    public float accumulator =0;


    public EnemyManager(LinkedList<WayPoint> path)
    {
        enemies = new LinkedList<Enemy>();
        towers = new LinkedList<Tower>();
        timeSinceLastNorm = 0;
        timeSinceLastFast = 0;
        timeSinceLastHeavy = 0;
        waveToBeSpawnedNorm = waveInfoNorm;
        waveToBeSpawnedFast = waveInfoFast;
        waveToBeSpawnedHeavy = waveInfoHeavy;
        this.path = path;

    }

    public void AddEnemy(Texture img, Texture img2, float velocity, float armor, LinkedList<WayPoint> path)
    {
        Enemy New = new Enemy(img, img2, velocity, armor, path, Enemy.Type.NORMAL);
        enemies.addLast(New);
        numEnemies++;
    }

    public void AddFastEnemy(Texture img3, Texture img4, float velocity, float armor, LinkedList<WayPoint> path)
    {
        Enemy nEw = new Enemy(img3, img4, velocity, armor, path, Enemy.Type.FAST);
        enemies.addLast(nEw);
        numEnemies++;
    }

    public void AddHeavyEnemy(Texture img5, Texture img6, float velocity, float armor, LinkedList<WayPoint> path)
    {
        Enemy neW = new Enemy(img5, img6, velocity, armor, path, Enemy.Type.HEAVY);
        enemies.addLast(neW);
        numEnemies++;
    }

    public void RemoveEnemy(int toBeDeleted)
    {

        if (toBeDeleted == 0)
        {
            enemies.removeFirst();
        }

        else
        {
            enemies.remove(toBeDeleted);
        }


    }

    public void NextWaveCalculator()
    {

    }

    public void Update(float fps, LinkedList<Tower> towers)
    {
        //accumulator +=deltaTime;


        this.towers = towers;

        if (currentWave == 1)
        {
            timeSinceLastNorm++;

            if (timeSinceLastNorm > MyGame.fpsretrieve/2 && waveToBeSpawnedNorm > 0) {
                AddEnemy(EnemyImg, NullLayer, 3, 1, path);
                timeSinceLastNorm = 0;
                waveToBeSpawnedNorm--;
            }

        }

        else if (currentWave == 2)
        {
            timeSinceLastNorm++;
            timeSinceLastFast++;

            if (timeSinceLastNorm > MyGame.fpsretrieve/2 && waveToBeSpawnedNorm > 0)
            {
                AddEnemy(EnemyImg, NullLayer, 3, 1, path);
                timeSinceLastNorm = 0;
                waveToBeSpawnedNorm--;
            }

            if (timeSinceLastFast > MyGame.fpsretrieve/3 && waveToBeSpawnedFast > 0)
            {
                AddEnemy(FastEnemy, NullLayer, 6, 1, path);
                timeSinceLastFast = 0;
                waveToBeSpawnedFast--;
            }
        }

        else if (currentWave > 2)
        {
            timeSinceLastNorm++;
            timeSinceLastFast++;
            timeSinceLastHeavy++;

            if (timeSinceLastNorm > MyGame.fpsretrieve/2 && waveToBeSpawnedNorm > 0)
            {
                AddEnemy(EnemyImg, NullLayer, 3, 1, path);
                timeSinceLastNorm = 0;
                waveToBeSpawnedNorm--;
            }

            if (timeSinceLastFast > MyGame.fpsretrieve/3 && waveToBeSpawnedFast > 0)
            {
                AddEnemy(FastEnemy, NullLayer, 6, 1, path);
                timeSinceLastFast = 0;
                waveToBeSpawnedFast--;
            }

            if (timeSinceLastHeavy > MyGame.fpsretrieve * 3 && waveToBeSpawnedHeavy > 0)
            {
                AddHeavyEnemy(TigerBase, TigerTurret, .5f, 15, path);
                timeSinceLastFast = 0;
                waveToBeSpawnedHeavy--;
            }
        }


        //Enemy health decrementer, very crude atm.
        for (int i = 0; i < enemies.size(); i++)
        {
            if (enemies.get(i) == null)
            {
                continue;
            }
            for (int j = 0; j < towers.size(); j++)
            {
                if (InRange(enemies.get(i).sprite.getX(),
                            enemies.get(i).sprite.getY(),
                            towers.get(j).sprite.getX(),
                            towers.get(j).sprite.getY(),
                            towers.get(j).range))
                {
                    if (enemies.get(i).health > 0)
                    {
                        enemies.get(i).health = enemies.get(i).health - (towers.get(j).damages / enemies.get(i).armor);
                    }


                }
            }
        }


        for (int i = 0; i < enemies.size(); i++)
        {
            if (enemies.get(i).health <= 0)
            {
                numDeadEnemies++;
                RemoveEnemy(i);
                numEnemies--;
            }

        }



        for (int i = 0; i < enemies.size(); i++)
        {
            if (!enemies.get(i).Check())
            {
                enemies.get(i).Move();
            }

        }


    }

    public int GetDeadEnemies(){
        int temp = numDeadEnemies;
        numDeadEnemies = 0;
        return temp;
    }

    public int CheckEnemiesAtEnd(){
        int enemyAtEnd = 0;
        for (int i = 0; i < enemies.size(); i++)
        {
            if (enemies.get(i).atEnd)
            {
                RemoveEnemy(i);
                numEnemies--;
                enemyAtEnd++;
            }

        }
        return enemyAtEnd;
    }

    public boolean InRange(float enemyX, float enemyY, float towerX, float towerY, float towerRange)
    {
        if (Math.abs(enemyX + centerOffset - towerX + centerOffset)
                < towerRange * rangeOffset
                && Math.abs(enemyY + centerOffset - towerY + centerOffset)
                < towerRange * rangeOffset)
        {
            if (Math.sqrt(Math.pow((enemyX + centerOffset - towerX + centerOffset), 2)
                                  + Math.pow((enemyY + centerOffset - towerY + centerOffset), 2))
                    < towerRange * rangeOffset)
            {
                return true;
            }
        }
        return false;
    }

    public void RenderAll(SpriteBatch sb)
    {

        for (int i = 0; i < numEnemies; i++)
        {
            enemies.get(i).render(sb);
        }

    }

}

