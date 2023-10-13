package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class ActionExecuteTests {
    private GameServer extendedServer;
    // Create a new server _before_ every @Test
    @BeforeEach
    void setup() {
        File extendedEntitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File extendedActionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        extendedServer = new GameServer(extendedEntitiesFile, extendedActionsFile);
    }
    // Test to make sure that the action can be executed
    @Test
    void testActionCanExecute() throws GameException {
       extendedServer.loadPlayer("simon");
       extendedServer.setCurrentPlayer("simon");
       Player currentPlayer = extendedServer.getPlayers().get(extendedServer.getCurrentPlayer());
       //test built-in action "GET"
       String response = BuiltInCommands.GetCommand.executeWithObject("coin", extendedServer);
       assertTrue(response.contains("You picked up a coin"), "Did not return a correct response to get");
        assertEquals(1, extendedServer.getGameLocations().get(currentPlayer.getName()).getGameObjectsList().size(), "there should be only one object in inventory after getting it");
       assertTrue(extendedServer.getGameLocations().get(currentPlayer.getName()).getGameObjectsList().contains("coin"),"coin is not in the player's inventory after getting it");
       //consumed health action
       try{
           HashSet<String> subjects = new HashSet<>();
           subjects.add("elf");
           currentPlayer.setCurrentLocation("cellar");
           GameAction targetAction = extendedServer.findTargetAction("attack", subjects);
           response = targetAction.execute(extendedServer);
           assertTrue(response.contains("You attack the elf, but he fights back and you lose some health"), "Did not return a correct response to attack");
           assertEquals(2, currentPlayer.getHealthLevel(), "Health level did not decrease after attack");
           response = targetAction.execute(extendedServer);
           assertTrue(response.contains("You attack the elf, but he fights back and you lose some health"), "Did not return a correct response to attack");
           assertEquals(1, currentPlayer.getHealthLevel(), "Health level did not decrease after attack");
           response = targetAction.execute(extendedServer);
           assertTrue(response.contains("you died and lost all of your items, you must return to the start of the game"), "Did not return a correct response to attack");
           assertEquals(3, currentPlayer.getHealthLevel(), "the player's health level is not restored to full");
           assertEquals(currentPlayer.getCurrentLocation(), extendedServer.getStartingPoint(), "player should be transported to the start location of the game");
           assertTrue(extendedServer.getGameLocations().get(currentPlayer.getName()).getGameObjectsList().isEmpty(), "When a player's health runs out, they should lose all of the items in their inventory");
       }catch(GameException ge){
           fail("Do not find the attack action");
       }
        //test built-in action "GOTO"
        currentPlayer.setCurrentLocation("cabin");
        response = BuiltInCommands.GotoCommand.executeWithObject("forest", extendedServer);
        assertTrue(response.contains("forest"), "Did not return a correct response to goto");
        assertEquals("forest", currentPlayer.getCurrentLocation(), "player should be transported to the forest location");
        response = BuiltInCommands.GotoCommand.executeWithObject("clearing", extendedServer);
        assertTrue(response.contains("there is no path to it"), "Did not return a correct response to goto");
        assertEquals("forest", currentPlayer.getCurrentLocation(), "player should not be transported to the clearing location because there is no path to it");
    }




}
