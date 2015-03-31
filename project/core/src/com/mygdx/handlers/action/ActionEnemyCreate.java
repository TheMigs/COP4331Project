package com.mygdx.handlers.action;

import com.mygdx.entities.Enemy;
import com.mygdx.handlers.NetworkManager;
import com.mygdx.states.GameState;

/**
 * Created by rob on 3/23/15.
 */
public class ActionEnemyCreate extends Action
{
    public int tempID;

    public ActionEnemyCreate(NetworkManager networkManager, Enemy newEnemy)
    {
        super(networkManager);

        actionClass = ActionClass.ACTION_ENEMY_CREATE;
        this.entity = newEnemy;

        newEnemy.entityID = tempEntityID.getAndIncrement();
        needsID = true;

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
