package edu.uob;


public class Player extends GameObject {
    private int healthLevel;
    public Player(String name, String description, String location) {
        super(name, description, ObjectType.CHARACTERS, location);
        healthLevel = 3;
    }
    public int getHealthLevel() {
        return healthLevel;
    }
    public void setHealthLevel(int healthLevel) {
        this.healthLevel = healthLevel;
    }
    public void addHealthLevel() {
        if(healthLevel < 3) {
            healthLevel++;
        }
    }
    public void reduceHealthLevel() throws GameException {
        if(healthLevel > 0) {
            healthLevel--;
        }
        if(healthLevel == 0) {
            throw new GameException.PlayerHealthLevelException();
        }
    }

}
