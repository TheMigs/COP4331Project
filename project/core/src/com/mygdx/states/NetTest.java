package com.mygdx.states;

// LibGDX includes

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.entities.Actor;
import com.mygdx.handlers.EnemyManager;
import com.mygdx.handlers.GameStateManager;
import com.mygdx.UI.MyStage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.mygdx.game.MyGame;
import com.mygdx.handlers.NetworkManager;
import com.mygdx.handlers.WayPointManager;
import com.mygdx.handlers.action.Action;
import com.mygdx.handlers.action.ActionPlayersReady;
import com.mygdx.handlers.action.ActionWaitForReady;
import com.mygdx.net.ConnectionMode;

import java.util.List;

/**
 * Created by NeilMoore on 2/14/2015.
 */
public class NetTest extends GameState
{
    protected boolean connected = false;
    private MyStage stage;
    private TextButton serverButton;
    private TextButton clientButton;
    private TextButton toMenu;
    private TextButton connecting;
    private TextButton ready;

    protected boolean waitForLobby = false;
    protected boolean lobbyReady = false;

    private OrthographicCamera cam;
    private Texture Map = new Texture("Maps/SubMenuMap.png");
    private Skin skin = new Skin(Gdx.files.internal("UiData/uiskin.json"));

    public NetTest(GameStateManager gameStateManager, final NetworkManager networkManager)
    {
        super(gameStateManager, networkManager);

        stage = new MyStage();
        cam = (OrthographicCamera) stage.getCamera();
        cam.setToOrtho(false, MyGame.V_WIDTH, MyGame.V_HEIGHT);
        Gdx.input.setInputProcessor(stage);

        serverButton = new TextButton("Server", skin);
        serverButton.setSize(200, 60);
        serverButton.setPosition(game.V_WIDTH / 2 - serverButton.getWidth() / 2, MyGame.V_HEIGHT * 7 / 12);
        serverButton.addListener(new ClickListener());


        clientButton = new TextButton("Client", skin);
        clientButton.setSize(200, 60);
        clientButton.setPosition(game.V_WIDTH / 2 - serverButton.getWidth() / 2, serverButton.getY() - 65);
        clientButton.addListener(new ClickListener());


        toMenu = new TextButton("Menu", skin);
        toMenu.setSize(200, 60);
        toMenu.setPosition(game.V_WIDTH / 2 - toMenu.getWidth() / 2, clientButton.getY() - 65);
        toMenu.addListener(new ClickListener());

        Actor map = new Actor(Map, 0, 0);
        stage.addActor(map);
        stage.addActor(toMenu);
        stage.addActor(clientButton);
        stage.addActor(serverButton);

        connecting = new TextButton("Connecting please wait...", skin);
        connecting.setSize(200, 60);
        connecting.setPosition(game.V_WIDTH/2-serverButton.getWidth()/2, MyGame.V_HEIGHT * 7/12);

        ready = new TextButton("Ready!", skin);
        ready.setSize(200, 60);
        ready.setPosition(game.V_WIDTH/2-serverButton.getWidth()/2, MyGame.V_HEIGHT * 7/12);

    }

    @Override
    public void update(float delta)
    {
        stage.act(delta);
        if (serverButton.isPressed())
        {
            networkManager.prepInitialize(true,
                                          ConnectionMode.WIFI_LAN,
                                          ConnectionMode.NONE,
                                          true);
            serverButton.setDisabled(true);
            serverButton.remove();
            clientButton.remove();
        }

        if(clientButton.isPressed())
        {
            networkManager.prepInitialize(false,
                                          ConnectionMode.WIFI_LAN,
                                          ConnectionMode.NONE,
                                          true);
            clientButton.setDisabled(true);
            serverButton.remove();
            clientButton.remove();
        }

        if(toMenu.isPressed())
        {
            // reset state since network manager 'falsely initialize' (i.e., we aren't doing multiplayer)
            networkManager.reset();
            gameStateManager.setState(GameStateManager.MENU, 0);
        }

        List<Action> actions = networkManager.fetchChanges();
        for(Action action : actions)
        {
            if(action instanceof ActionWaitForReady)
            {
                waitForLobby = true;
            }

            if(action instanceof ActionPlayersReady)
            {
                lobbyReady = true;
            }
        }

        if(waitForLobby)
        {
            stage.addActor(connecting);
            waitForLobby = false;
        }

        if(lobbyReady)
        {
            connecting.setVisible(false);
            connecting.setDisabled(true);
            connecting.remove();
            ready.setVisible(true);
            ready.setDisabled(false);
            stage.addActor(ready);
            lobbyReady = false;
        }

        if(ready.isPressed())
        {
            gameStateManager.setState(GameStateManager.LEVELSELECT,0);
        }

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta)
    {
        // clear screen, then draw stage
        Gdx.gl.glClearColor(0, 0, 0, 2);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
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
    public void dispose()
    {

    }
}
