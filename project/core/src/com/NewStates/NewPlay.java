package com.NewStates;


import com.NewEntities.MyActor;
import com.NewEntities.NewEnemy;
import com.NewEntities.NewTower;
import com.NewHandlers.NewEnemyManager;
import com.NewHandlers.NewGameStateManager;
import com.NewHandlers.NewTowerManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.Debug.Debugger;
import com.mygdx.game.MyGame;
import com.mygdx.handlers.MyInput;
import com.mygdx.handlers.NetworkManager;
import com.mygdx.handlers.WayPointManager;


import java.util.LinkedList;


/**
 * Created by James on 3/18/2015.
 */
public class NewPlay extends  NewGameState {

    private int gold = 500;
    private int health = 10;
    private int towerPlacement = 0;
    private int Zooka = 0;
    private int Rifle = 0;

    private boolean clicked = false;

    private LinkedList<NewTower> towers;
    public NewEnemyManager enemyManager;
    public NewTowerManager towerManager;
    public WayPointManager wayPointManager;

    private TextButton rifleButton;
    private TextButton bazookaButton;

    private Stage stage;
    private Texture mapImg;
    private Debugger debugger;

    Sprite towerToBePlaced;
    Sprite towerToBePlacedS;
    Texture RifleTower = new Texture("RifleTower.png");
    Texture BazookaTower = new Texture("BazookaTower.png");
    Texture TowerShadow = new Texture("shadowtower.png");
    private BitmapFont font;
    private Batch batch;


    public NewPlay(NewGameStateManager gameStateManager,NetworkManager networkManager, boolean inAndroid){
        super(gameStateManager,networkManager);
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        Skin skin = new Skin(Gdx.files.internal("UiData/uiskin.json"));
        mapImg = new Texture("MapEasy.png");
        MyActor map = new MyActor(mapImg,0,0);

        towers = new LinkedList<NewTower>();
        wayPointManager = new WayPointManager(inAndroid);
        enemyManager = new NewEnemyManager(wayPointManager.wayPoints);
        towerManager = new NewTowerManager(towers);
        rifleButton = new TextButton("rifle",skin);
        rifleButton.setSize(64,64);
        rifleButton.setPosition(game.V_WIDTH-rifleButton.getWidth(), game.V_HEIGHT-rifleButton.getHeight());
        rifleButton.addListener(new ClickListener());
        bazookaButton = new TextButton("bazooka",skin);
        bazookaButton.setSize(64,64);
        bazookaButton.setPosition(rifleButton.getX(),rifleButton.getY()-64);

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.scale(.01f);


        stage.addActor(map);
        stage.addActor(enemyManager);
        stage.addActor(towerManager);
        stage.addActor(rifleButton);
        stage.addActor(bazookaButton);

    }

    @Override
    public void update() {
        //((OrthographicCamera)stage.getCamera()).zoom += .01;
        health = health - enemyManager.CheckEnemiesAtEnd();
        gold = gold + (enemyManager.GetDeadEnemies() * 15);

        if(rifleButton.isPressed() && towerPlacement==0 && gold >= towerManager.rifleBasePrice){
            System.out.println("test");
            towerToBePlaced = new Sprite(RifleTower);
            towerToBePlacedS = new Sprite(TowerShadow);
            towerToBePlaced.setPosition(Gdx.input.getX(), MyGame.V_HEIGHT - Gdx.input.getY());
            towerToBePlacedS.setPosition(Gdx.input.getX()+ 9,MyGame.V_HEIGHT - Gdx.input.getY() - 23);
            towerToBePlacedS.rotate(-45);
            towerPlacement = 1;
            Rifle = 1;
        }

        if(bazookaButton.isPressed() && towerPlacement==0 && gold >= towerManager.bazookaBasePrice){
            System.out.println("test");
            towerToBePlaced = new Sprite(BazookaTower);
            towerToBePlacedS = new Sprite(TowerShadow);
            towerToBePlaced.setPosition(Gdx.input.getX(), MyGame.V_HEIGHT - Gdx.input.getY());
            towerToBePlacedS.setPosition(Gdx.input.getX()+ 9,MyGame.V_HEIGHT - Gdx.input.getY() - 23);
            towerToBePlacedS.rotate(-45);
            towerPlacement = 1;
            Zooka = 1;
        }


        else if (!Gdx.input.isTouched() && towerPlacement == 1 && !wayPointManager.WithinAny(Gdx.input.getX(), Gdx.input.getY()))
        {
            if(Rifle == 1)
            {
                towerManager.addRifleTower(Gdx.input.getX(), MyGame.V_HEIGHT - Gdx.input.getY());
                towerPlacement--;
                Rifle--;
                gold = gold - towerManager.rifleBasePrice;
            }

            else if(Zooka == 1)
            {
                towerManager.addBazookaTower(Gdx.input.getX(), MyGame.V_HEIGHT -Gdx.input.getY());
                towerPlacement--;
                Zooka--;
                gold = gold - towerManager.bazookaBasePrice;
            }
        }
        else if (towerPlacement == 1)
        {
            System.out.println("towerPlacement == 1");
            towerToBePlaced.setPosition(Gdx.input.getX(), MyGame.V_HEIGHT - Gdx.input.getY());
            towerToBePlacedS.setPosition(Gdx.input.getX()+ 9,MyGame.V_HEIGHT - Gdx.input.getY() - 23);
        }

        else if(Gdx.input.isTouched())
        {
            switch (towerPlacement){
                case (1):
                    towerPlacement =0;
                    break;
            }
        }


    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 2);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        enemyManager.SetTowers(towers);
        stage.act(delta);
        stage.draw();
        batch = stage.getBatch();
        batch.begin();
        if(towerPlacement == 1)
        {
            towerToBePlacedS.draw(batch);
            towerToBePlaced.draw(batch);
        }
        font.draw(batch, "Health: " + health, 0, MyGame.V_HEIGHT - 10);
        font.draw(batch, "Gold: " + gold, 96, MyGame.V_HEIGHT - 10);
        font.draw(batch, "Wave: " + enemyManager.currentWave, 192, MyGame.V_HEIGHT - 10);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
