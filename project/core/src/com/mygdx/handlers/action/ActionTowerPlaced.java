package com.mygdx.handlers.action;

import com.mygdx.entities.Tower;

/**
 * Created by rob on 3/25/15.
 */
public class ActionTowerPlaced extends ActionTowerBase
{
    public ActionTowerPlaced()
    {

    }

    public ActionTowerPlaced(Tower tower)
    {
        super(tower);

        actionClass = ActionClass.ACTION_TOWER_PLACED;
    }
}
