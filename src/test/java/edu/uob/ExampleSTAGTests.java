package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

class ExampleSTAGTests {

  private GameServer server;
  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will time out if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testMultiPlayer() {
      //test that we can not add a player with an invalid name
      sendCommandToServer("simon123: look");
      String response2 = sendCommandToServer("lxm: look");
      assertFalse(response2.contains("simon123"), "Can not add a player with invalid name");
      //test that we can add a player with a valid name and other player can see it
      String response1 = sendCommandToServer("simon: look");
      assertTrue(response1.contains("lxm"), "Did not see the name of the another player");
      response2 = sendCommandToServer("lxm: look");
      assertTrue(response2.contains("simon"), "Did not see the name of the another player");
      //test that we can not use things in other player's inventory
      sendCommandToServer("simon: get potion");
      response2 = sendCommandToServer("lxm: drink potion");
      assertTrue(response2.contains("You don't have all the subjects for this action"), "Can not consume things in other player's inventory");
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: get key");
      response2 = sendCommandToServer("lxm: open trapdoor");
      sendCommandToServer("lxm: look");
      assertFalse(response2.contains("cellar"), "key is in another player's inventory, so you can not open the door");
      sendCommandToServer("simon: drop key");
  }
  @Test
  void testLook() {
    String response = sendCommandToServer("simon: look");
    response = response.toLowerCase();
    assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
    assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
    assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
    assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
    assertTrue(response.contains("forest"), "Did not see available paths in response to look");
  }

  // Test that we can pick something up and that it appears in our inventory.
  // and then we drop it, and it is no longer in our inventory. it is in the current place.
    // it can also test inventory/inv command
  @Test
  void testGetDrop()
  {
      String response;
      sendCommandToServer("simon: get potion");
      response = sendCommandToServer("simon: inv");
      response = response.toLowerCase();
      assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: inventory");
      assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
      //you can not get a thing that is not in the current place
      sendCommandToServer("simon: get key");
      response = sendCommandToServer("simon: inv");
      assertFalse(response.contains("key"), "You can not get key because key is not here");
      //you can not get a thing that is furniture
      sendCommandToServer("simon: get trapdoor");
      response = sendCommandToServer("simon: inv");
      assertFalse(response.contains("trapdoor"), "You can not get trapdoor because trapdoor is furniture");

      sendCommandToServer("simon: drop potion");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("potion"), "Did not see the potion in current place after an attempt was made to drop it");
      response = sendCommandToServer("simon: inv");
      assertFalse(response.contains("potion"), "Potion is still present in the inventory after an attempt was made to drop it");
      //you can not drop a thing that is not in your inventory
      response = sendCommandToServer("simon: drop potion");
      assertTrue(response.contains("you haven't have it"), "you can not drop a thing you don't have");
  }

  // Test that we can goto a different location (we won't get very far if we can't move around the game !)
  @Test
  void testGoto()
  {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
      //you can not go to a place that there is no path to it
      sendCommandToServer("simon: goto riverbank");
      sendCommandToServer("simon: goto clearing");
      response = sendCommandToServer("simon: look");
      assertFalse(response.contains("ground"), "You can not go to clearing because there is no path to it");
  }
  @Test
  void testProduced()
  {
      //produced location
      sendCommandToServer("simon: get axe");
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: cut tree");
      sendCommandToServer("simon: get log");
      sendCommandToServer("simon: goto riverbank");
      sendCommandToServer("simon: bridge river");
      String response = sendCommandToServer("simon: look");
      assertTrue(response.contains("clearing"), "Failed attempt to produced a path to clearing");
      // produced a location that is already exist
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: open trapdoor");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("cellar"), "Failed attempt to produced a path to cellar");
      response = sendCommandToServer("simon: open trapdoor");
      assertTrue(response.contains("already exists"), "You can not produce a path that is already exist");
  }
    @Test
    void testConsumed()
    {
        //consumed a place is not exist
        String response = sendCommandToServer("simon: lock trapdoor");
        assertTrue(response.contains("There was no road"), "You can not close trapdoor because it is not open");
        //produced location
        sendCommandToServer("lxm: open trapdoor");
        response = sendCommandToServer("lxm: look");
        assertTrue(response.contains("cellar"), "a way to cellar is not produced");
        //consumed location
        sendCommandToServer("simon: lock trapdoor");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("cellar"), "a way to cellar is not consumed");
    }
    @Test
    void testHealth()
    {
        sendCommandToServer("simon: open trapdoor");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: hit elf");
        String response = sendCommandToServer("simon: health");
        assertTrue(response.contains("2"), "health is not reduced");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: drink potion");
        response = sendCommandToServer("simon: health");
        assertTrue(response.contains("3"), "health is not increased");
        //test that a player is died
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: attack elf");
        sendCommandToServer("simon: attack elf");
        sendCommandToServer("simon: attack elf");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("cabin"), "you are not back to tha starting point after you died");
        assertFalse(response.contains("axe"), "axe is not dropped in the place you died.");
        sendCommandToServer("simon: goto cellar");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("axe"), "axe is not dropped in the place you died.");
        response = sendCommandToServer("simon: health");
        assertTrue(response.contains("3"), "health is not reset after you died");
    }
    @Test
    void testCommandFlexibility()
    {
        //Case Insensitivity
        //multiple actions(action + built-in command)
        sendCommandToServer("simon: drink get potion");
        String response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("potion"), "Ambiguous commands should not execute");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("potion"), "Ambiguous commands should not execute");
        //multiple actions(action + action)
        sendCommandToServer("simon: drink fight potion");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("potion"), "Ambiguous commands should not execute");
        //invalid [look]
        response = sendCommandToServer("simon: look cabin");
        assertFalse(response.contains("potion"), "Ambiguous commands should not execute");
        //invalid [goto]
        response = sendCommandToServer("simon: goto cabin forest");
        assertTrue(response.contains("Invalid"), "A command containing extraneous entities should not execute");
        //multiple built-in command
        sendCommandToServer("simon: goto look forest");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("key"), "Ambiguous commands should not execute");
        //incorrect command
        response = sendCommandToServer("simon: looking");
        assertFalse(response.contains("trapdoor"), "Invalid command should not execute");
        //invalid [inventory]
        sendCommandToServer("simon: get axe");
        response = sendCommandToServer("simon: inv axe");
        assertFalse(response.contains("axe"), "Invalid command should not execute");
        //invalid [drop]
        sendCommandToServer("simon: get coin");
        sendCommandToServer("simon: drop coin and axe");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("coin"), "A command containing extraneous entities should not execute");
        //invalid [drop], drop a location, incorrect type
        response = sendCommandToServer("simon: drop forest");
        assertTrue(response.contains("Invalid"), "can not drop a location");
        // Extraneous Entities for built-in commands
        response = sendCommandToServer("simon: get axe coin");
        assertTrue(response.contains("Invalid"), "A command containing extraneous entities should not execute");
        //incorrect subject's name
        sendCommandToServer("simon: open a door");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("cellar"), "door is not an entity in this location, command should not execute");
        // Extraneous Entities for an action
        sendCommandToServer("simon: open trapdoor and potion");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("cellar"), "A command containing extraneous entities should not execute");
        assertTrue(response.contains("potion"), "A command containing extraneous entities should not execute");
        //incorrect [health]
        response = sendCommandToServer("simon: health simon");
        assertFalse(response.contains("3"), "A command containing extraneous entities should not execute");
    }

}
