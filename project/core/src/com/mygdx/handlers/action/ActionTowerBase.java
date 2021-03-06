package com.mygdx.handlers.action;

import com.mygdx.entities.Tower;

/**
 * Created by NeilMoore on 4/4/2015.
 */
public class ActionTowerBase extends ActionEntityBase
{
    public Class<? extends Tower> towerType;

    public int level;

    public ActionTowerBase()
    {

    }

    public ActionTowerBase(Tower tower)
    {
        super(tower);

        towerType = tower.getClass();
        level = tower.getTowerLevel();
    }
}
