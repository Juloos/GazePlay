package net.gazeplay.games.TowerDefense.maps;

public class MapOne extends Map {
    public MapOne() {
        super();
    }

    @Override
    public void setStructure() {
        this.map = new int[][] {
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8,0,0,},
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,},
            {0,0,0,0,1,1,1,1,1,1,0,0,0,0,0,0,0,1,0,0,},
            {0,0,0,0,1,2,0,2,0,1,0,0,0,2,0,0,0,1,2,0,},
            {0,0,0,0,1,0,0,0,0,1,0,0,1,1,1,0,0,1,1,1,},
            {0,0,0,2,1,0,0,0,0,1,0,0,1,0,1,0,0,0,0,1,},
            {9,1,1,1,1,0,0,0,0,1,0,0,1,0,1,0,2,1,1,1,},
            {0,0,2,0,0,0,0,0,0,1,0,2,1,0,1,0,0,1,0,0,},
            {0,0,0,0,0,0,0,0,0,1,1,1,1,0,1,1,1,1,0,0,},
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,},
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,},
        };
    }
}