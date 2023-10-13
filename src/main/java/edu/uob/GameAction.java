package edu.uob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class  GameAction
{
    private HashSet<String> subjects;
    private HashSet<String> consumed;
    private HashSet<String> produced;
    private String narration;
    public GameAction(HashSet<String> subjects, HashSet<String> consumed, HashSet<String> produced, String narration) {
        this.subjects = subjects;
        this.consumed = consumed;
        this.produced = produced;
        this.narration = narration;
    }
    public HashSet<String> getSubjects(){
        return subjects;
    }
    private boolean isObjectIn(HashSet<String> objects, HashSet<String> objectsList){
        for(String object : objects){
            if(objectsList.contains(object)){
                return true;
            }
        }
        return false;
    }

    public String execute(GameServer server){
        Player currentPlayer = server.getPlayers().get(server.getCurrentPlayer());
        HashMap<String, GameObject> gameObjects = server.getGameObjects();
        HashMap<String,Location> gameLocations = server.getGameLocations();
        //check if subjects are in current location or player's inventory
        for(String subject:subjects){
            if(!gameLocations.get(currentPlayer.getCurrentLocation()).getGameObjectsList().contains(subject) &&
                    !gameLocations.get(currentPlayer.getName()).getGameObjectsList().contains(subject) &&
                    !gameLocations.containsKey(subject)){
                return "You don't have all the subjects for this action";
            }
        }
        //check if produced/consumed objects are in other player's inventory
        for(String player: server.getPlayers().keySet()){
            if(!player.equals(currentPlayer.getName())){
                if(isObjectIn(produced, gameLocations.get(player).getGameObjectsList()) ||
                        isObjectIn(consumed, gameLocations.get(player).getGameObjectsList())){
                    return "You can not produce/consume objects in other player's inventory";
                }
            }
        }
        //execute action, the object's type can be normal object, health and location.
        // although we can not consume/produce character, we don't need to check it here because all objects are from a valid file
        for(String consumedObject:consumed){
            if(gameObjects.containsKey(consumedObject)){
                String consumedObjectLocation = gameObjects.get(consumedObject).getCurrentLocation();
                gameLocations.get(consumedObjectLocation).removeGameObject(consumedObject);
                gameObjects.get(consumedObject).setCurrentLocation("storeroom");
                gameLocations.get("storeroom").addGameObject(consumedObject);
            } else if (consumedObject.equals("health")){
                try{
                    currentPlayer.reduceHealthLevel();
                    //System.out.println("health:" + currentPlayer.getHealthLevel() + " health level reduced?????");//for debug
                }catch (GameException ge) {
                    HashSet<String> playerObjectList = gameLocations.get(currentPlayer.getName()).getGameObjectsList();
                    for(String object: playerObjectList){
                        gameObjects.get(object).setCurrentLocation(currentPlayer.getCurrentLocation());
                        gameLocations.get(currentPlayer.getName()).removeGameObject(object);
                        gameLocations.get(currentPlayer.getCurrentLocation()).addGameObject(object);
                    }
                    currentPlayer.setCurrentLocation(server.getStartingPoint());
                    currentPlayer.setHealthLevel(3);    //reset health level
                    return ge.getMessage();
                }
            } else if (gameLocations.containsKey(consumedObject)){
                if(gameLocations.get(currentPlayer.getCurrentLocation()).getPathsList().contains(consumedObject)){
                    gameLocations.get(currentPlayer.getCurrentLocation()).removePath(consumedObject);
                }else{
                    return "There was no road to "+ consumedObject +".";
                }
            }
        }
        for(String producedObject:produced){
            if(gameObjects.containsKey(producedObject)) {
                String producedObjectLocation = gameObjects.get(producedObject).getCurrentLocation();
                gameLocations.get(producedObjectLocation).removeGameObject(producedObject);
                gameObjects.get(producedObject).setCurrentLocation(currentPlayer.getCurrentLocation());
                gameLocations.get(currentPlayer.getCurrentLocation()).addGameObject(producedObject);
            }else if (producedObject.equals("health")){
                    currentPlayer.addHealthLevel();
            } else if (gameLocations.containsKey(producedObject)){
                if(!gameLocations.get(currentPlayer.getCurrentLocation()).getPathsList().contains(producedObject)){
                    gameLocations.get(currentPlayer.getCurrentLocation()).addPath(producedObject);
                }else{
                    return "path to "+ producedObject +" already exists.";
                }
            }
        }
        return narration;
    }
}
