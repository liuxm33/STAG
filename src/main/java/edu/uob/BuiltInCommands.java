package edu.uob;

import java.util.HashMap;
import java.util.Objects;

public class BuiltInCommands {
    static class InventoryCommand extends BuiltInCommands{
        public static String execute(GameServer server){
            //Player currentPlayer, HashMap<String, Location> gameLocations
            return "You are carrying: \n" + server.getGameLocations().get(server.getCurrentPlayer()).getGameObjectsList();
        }
    }
    static class LookCommand  extends BuiltInCommands{
        public static String execute(GameServer server){
            //Player currentPlayer, HashMap<String, Location> gameLocations, HashMap<String, GameObject> gameObjects
            Player currentPlayer = server.getPlayers().get(server.getCurrentPlayer());
            HashMap<String, Location> gameLocations = server.getGameLocations();
            HashMap<String, GameObject> gameObjects = server.getGameObjects();

            String narration = "You are in " + gameLocations.get(currentPlayer.getCurrentLocation()).getDescription() +" . you can see:\n";
            Location currentLocation = gameLocations.get(currentPlayer.getCurrentLocation());
            for(String object : currentLocation.getGameObjectsList()){
                if(!Objects.equals(object, currentPlayer.getName())){
                    narration += gameObjects.get(object).getName() + " : " + gameObjects.get(object).getDescription() + "\n";
                }
            }
            narration += "you can go to:\n";
            for(String path : currentLocation.getPathsList()){
                narration += path + "\n";
            }
            return narration;
        }
    }
    static class GetCommand  extends BuiltInCommands{
        public static String executeWithObject(String objectName, GameServer server){
            // Player currentPlayer, HashMap<String, Location> gameLocations, HashMap<String, GameObject> gameObjects
            Player currentPlayer = server.getPlayers().get(server.getCurrentPlayer());
            HashMap<String, Location> gameLocations = server.getGameLocations();
            HashMap<String, GameObject> gameObjects = server.getGameObjects();

            Location currentLocation = gameLocations.get(currentPlayer.getCurrentLocation());
            if(!currentLocation.getGameObjectsList().contains(objectName) || gameObjects.get(objectName).getObjectType() != ObjectType.ARTEFACTS){
                return objectName + " is not available for this action, it is not in this location or you cannot pick it up";
            }
            currentLocation.removeGameObject(objectName);
            gameObjects.get(objectName).setCurrentLocation(currentPlayer.getName());
            gameLocations.get(currentPlayer.getName()).addGameObject(objectName);
            return "You picked up a " + objectName;
        }
    }
    static class DropCommand extends BuiltInCommands{
        public static String executeWithObject(String objectName, GameServer server){
            Player currentPlayer = server.getPlayers().get(server.getCurrentPlayer());
            HashMap<String, Location> gameLocations = server.getGameLocations();
            HashMap<String, GameObject> gameObjects = server.getGameObjects();

            Location currentLocation = gameLocations.get(currentPlayer.getCurrentLocation());
            Location inventory = gameLocations.get(currentPlayer.getName());
            if(!inventory.getGameObjectsList().contains(objectName)){
                return objectName + " is not available for this action, you haven't have it";
            }
            inventory.removeGameObject(objectName);
            gameObjects.get(objectName).setCurrentLocation(currentLocation.getName());
            currentLocation.addGameObject(objectName);
            return "You dropped a " + objectName;
        }
    }
    static class GotoCommand  extends BuiltInCommands {
        public static String executeWithObject(String destinationName, GameServer server){
            Player currentPlayer = server.getPlayers().get(server.getCurrentPlayer());
            HashMap<String, Location> gameLocations = server.getGameLocations();

            Location currentLocation = gameLocations.get(currentPlayer.getCurrentLocation());
            Location destination = gameLocations.get(destinationName);
            if(!currentLocation.getPathsList().contains(destinationName)){
                return destinationName + " is not available for this action, there is no path to it";
            }
            currentLocation.removeGameObject(currentPlayer.getName());
            currentPlayer.setCurrentLocation(destinationName);
            destination.addGameObject(currentPlayer.getName());
            return LookCommand.execute(server);
        }
    }
    static class HealthCommand  extends BuiltInCommands{
        public static String execute(GameServer server){
            Player currentPlayer = server.getPlayers().get(server.getCurrentPlayer());
            return "Your health is:" + currentPlayer.getHealthLevel();
        }
    }
}
