package com.mygdx.triggers;

/**
 * Created by James on 2/14/2015.
 */
public class WayPoint
{
    public float x;
    public float y;
    public enum Direction
    {
        NORTH(0,1),
        SOUTH(0,-1),
        EAST(1,0),
        WEST(-1,0),
        END(0,0), // NOTE: Discussion is needed
        NONE(0,0);

        public int x,y;
        Direction(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }

    public Direction direction;

    public WayPoint(float x, float y, Direction direction)
    {
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

}
