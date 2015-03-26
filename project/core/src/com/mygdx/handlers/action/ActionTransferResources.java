package com.mygdx.handlers.action;

import com.mygdx.handlers.NetworkManager;
import com.mygdx.states.GameState;

/**
 * Created by rob on 3/25/15.
 */
public class ActionTransferResources extends Action
{
    public int gold, playerFrom, playerTo;

    public ActionTransferResources(GameState gameState, NetworkManager networkManager, int gold, int playerFrom, int playerTo)
    {
        super(gameState, networkManager);

        this.networkManager = networkManager;
        this.gameState = gameState;

        actionClass = ActionClass.ACTION_TRANSFER_RESOURCES;

        this.gold = gold;
        this.playerFrom = playerFrom;
        this.playerTo = playerTo;

        updateGamestate();
        updateNetMan();
    }

    @Override
    public void updateGamestate()
    {
        /**
         * TODO -- add gameState changes
         */
    }

    @Override
    public void updateNetMan()
    {
        networkManager.addToSendQueue(this);
    }
}