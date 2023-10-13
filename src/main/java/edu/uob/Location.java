package edu.uob;

import java.util.HashSet;

public class Location extends GameEntity{
    private HashSet<String> paths; //other location's name
    private HashSet<String> gameObjects; //object's name
    public Location(String name, String description) {
        super(name, description);
        paths = new HashSet<>();
        gameObjects = new HashSet<>();
    }
    public HashSet<String> getPathsList() {
        return paths;
    }
    public HashSet<String> getGameObjectsList() {
        return gameObjects;
    }

    public void addPath(String path) {
        paths.add(path);
    }
    public void removePath(String path) {
        paths.remove(path);
    }
    public void addGameObject(String gameObject) {
        gameObjects.add(gameObject);
    }
    public void removeGameObject(String gameObject) {
        gameObjects.remove(gameObject);
    }
}
