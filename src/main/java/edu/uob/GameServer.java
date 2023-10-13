package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** This class implements the STAG server. */
public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.GameServer(File, File)}) otherwise we won't be able to mark
    * your submission correctly.
    *
    * <p>You MUST use the supplied {@code entitiesFile} and {@code actionsFile}
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
    *
    */
    private HashMap<String, HashSet<GameAction>> gameActions = new HashMap<>(); // only for those game-specific commands
    private HashMap<GameAction, HashSet<String>> actionTrigger = new HashMap<>();// only for those game-specific commands
    private HashMap<String, BuiltInCommands> builtInCommands = new HashMap<>();
    private HashMap<String, Location> gameLocations = new HashMap<>();
    private HashMap<String, GameObject> gameObjects = new HashMap<>();
    private HashMap<String, Player> players = new HashMap<>();
    private String startingPoint; //store the starting location's [name]
    private String currentPlayer;

    public HashMap<String, Location> getGameLocations(){
        return gameLocations;
    }

    public HashMap<String, GameObject> getGameObjects(){
        return gameObjects;
    }

    public HashMap<String, Player> getPlayers(){
        return players;
    }

    public String getCurrentPlayer(){
        return currentPlayer;
    }

    public void setCurrentPlayer(String name){
        currentPlayer = name;
    }

    public String getStartingPoint(){
        return startingPoint;
    }

    public GameServer(File entitiesFile, File actionsFile) {
        // TODO implement your server logic here
        //file to class
        try{
            //parse entitiesFile
            Parser parser = new Parser();
            FileReader reader = new FileReader(entitiesFile);
            parser.parse(reader);
            Graph wholeDocument = parser.getGraphs().get(0);
            //load gameLocations and gameObjects and paths
            ArrayList<Graph> sections = wholeDocument.getSubgraphs();
            loadLocationsAndObjects(sections);
            if(!gameLocations.containsKey("storeroom")){
                Location newLocation = new Location("storeroom", "Storage for any entities not placed in the game");
                gameLocations.put("storeroom", newLocation);
            }
            ArrayList<Edge> paths = sections.get(1).getEdges();
            loadPaths(paths);

            //parse actionsFile
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(actionsFile);
            Element root = document.getDocumentElement();
            NodeList actions = root.getChildNodes();
            for(int i = 1; i < actions.getLength(); i += 2) {
                loadActions((Element)actions.item(i));
            }
            //load built-in actions
            loadBuiltInActions();
        }catch (FileNotFoundException fnfe) {
            System.out.println("FileNotFoundException was thrown when attempting to read basic entities file");
        } catch (ParseException pe) {
            System.out.println("ParseException was thrown when attempting to read basic entities file");
        } catch(ParserConfigurationException pce) {
            System.out.println("ParserConfigurationException was thrown when attempting to read basic actions file");
        } catch(SAXException saxe) {
            System.out.println("SAXException was thrown when attempting to read basic actions file");
        } catch(IOException ioe) {
            System.out.println("IOException was thrown when attempting to read basic actions file");
        }
    }

    private void loadBuiltInActions(){
        builtInCommands.put("look", new BuiltInCommands.LookCommand());
        builtInCommands.put("inventory", new BuiltInCommands.InventoryCommand());
        builtInCommands.put("inv", new BuiltInCommands.InventoryCommand());
        builtInCommands.put("get",new BuiltInCommands.GetCommand());
        builtInCommands.put("drop", new BuiltInCommands.DropCommand());
        builtInCommands.put("goto", new BuiltInCommands.GotoCommand());
        builtInCommands.put("health", new BuiltInCommands.HealthCommand());
//        System.out.println("Loaded built-in actions"); // for test
    }

    private HashSet<String> loadActionAttributes(Element action, String actionAttribute){
        Element attributeTag = (Element)action.getElementsByTagName(actionAttribute).item(0);
        NodeList Entities = attributeTag.getElementsByTagName("entity");
        HashSet<String> gameObjects = new HashSet<>();
        for(int i = 0; i< Entities.getLength(); i++){
            String subject = Entities.item(i).getTextContent();
            gameObjects.add(subject);
        }
//        System.out.println("Loaded "+ actionAttribute +": "+ gameObjects); // for test
        return gameObjects;
    }

    private void loadActions(Element action){
        //Get the first action (only the odd items are actually actions - 1, 3, 5 etc.)
        HashSet<String> subjects = loadActionAttributes(action, "subjects");
        HashSet<String> consumed = loadActionAttributes(action, "consumed");
        HashSet<String> produced = loadActionAttributes(action, "produced");
        String narration = action.getElementsByTagName("narration").item(0).getTextContent();
        GameAction newAction = new GameAction(subjects, consumed, produced, narration);//action
        actionTrigger.put(newAction, new HashSet<>());

        Element triggers = (Element)action.getElementsByTagName("triggers").item(0);
        NodeList triggerPhrases = triggers.getElementsByTagName("keyphrase");
        for(int i = 0; i<triggerPhrases.getLength(); i++){
            String keyphrase = triggerPhrases.item(i).getTextContent(); //keyphrase
            if(!gameActions.containsKey(keyphrase)){
                gameActions.put(keyphrase, new HashSet<>()); //new trigger for a list of actions
            }
//            System.out.println("Loaded keyphrase: "+ keyphrase); // for test
            gameActions.get(keyphrase).add(newAction);
            actionTrigger.get(newAction).add(keyphrase);
        }
    }

    private void loadPaths(ArrayList<Edge> paths){
        for(Edge path : paths){
            Node fromLocation = path.getSource().getNode();
            String fromLocationName = fromLocation.getId().getId();
            Node toLocation = path.getTarget().getNode();
            String toLocationName = toLocation.getId().getId();
            gameLocations.get(fromLocationName).addPath(toLocationName);
//            System.out.println("Path from: "+ fromLocationName +" to "+ toLocationName); // for test
        }
    }

    private void loadLocationsAndObjects(ArrayList<Graph> sections){
        ArrayList<Graph> locations = sections.get(0).getSubgraphs();
        //load gameLocations
        for(Graph location : locations){
            //get the location's name and description,and load them into the gameLocations
            Node locationDetails = location.getNodes(false).get(0);
            String locationName = locationDetails.getId().getId();
            String locationDescription = locationDetails.getAttribute("description");
            Location newLocation = new Location(locationName, locationDescription);
            gameLocations.put(locationName, newLocation);
            //get the objects in that location and load them into the gameObjects.
            //load the objects' locations
            //also add the objects' name into the location's objects list
            ArrayList<Graph> locationObjects = location.getSubgraphs();
            for(Graph objectType : locationObjects){
                String objectTypeName = objectType.getId().getId();
                ObjectType type = ObjectType.valueOf(objectTypeName.toUpperCase());
                objectType.getNodes(false).forEach(node -> {
                    String objectName = node.getId().getId();
                    String objectDescription = node.getAttribute("description");
                    GameObject newGameObject = new GameObject(objectName, objectDescription, type, locationName);
                    gameObjects.put(objectName, newGameObject);
                    newLocation.addGameObject(objectName);
//                    System.out.println("Object: "+objectName+" is loaded. description: "+objectDescription + "location:" + newGameObject.getCurrentLocation()); // for test
                });
            }
            //for test location information
//            System.out.println("in this location have Objects: "+gameLocations.get(locationName).getGameObjectsList()); // for test
//            System.out.println("Location: "+locationName+" is loaded. description: "+locationDescription); // for test
        }
        //store the starting point location's name
        startingPoint = locations.get(0).getNodes(false).get(0).getId().getId();
//       System.out.println("Starting point: "+startingPoint); // for test
    }

    private boolean isNewPlayer(String userName) throws GameException {
        //check if the player name is valid
        String pattern = "^[a-zA-Z\\s'-]+$";
        if (!userName.matches(pattern)) {
            throw new GameException.InvalidUserNameException(userName);
        }
        // the player name should be unique
        return !players.containsKey(userName);
    }
    void loadPlayer(String userName) throws GameException {
        if(isNewPlayer(userName)){
            String description = "A player/user named "+ userName;
            Player newPlayer = new Player(userName, description, startingPoint);
            gameObjects.put(userName, newPlayer);
            gameLocations.get(startingPoint).addGameObject(userName);//player is also needed to be added into the location's objects list

            players.put(userName, newPlayer);
            //special location for player's inventory named by player's name
            Location inventory = new Location(userName, userName + "'s inventory");
            gameLocations.put(userName, inventory);
//            System.out.println("add User: "+ userName +" in: " + newPlayer.getCurrentLocation()); // for test
        }
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.GameServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming game commands and carries out the corresponding actions.
    */
    //parse command -> get action instance- > action.execute() -> return result
    public String handleCommand(String command) {
        // TODO implement your server logic here
        int colonIndex = command.indexOf(":");
        if(colonIndex == -1){
            return "You need to create a player first.";
        }
        String userName = command.substring(0, colonIndex);
        String userCommand = command.substring(colonIndex+1).toLowerCase(); //for case-insensitive
        //create a new player if the player is new
        try{
            loadPlayer(userName);
        }catch (GameException ce){
            return ce.getMessage();
        }
        currentPlayer = players.get(userName).getName();
        /*.........................parse the command and find the action to execute.......................................*/
        HashSet<String> triggers = new HashSet<>();
        HashSet<String> builtInCmdTriggers = new HashSet<>();
        HashSet<String> subjects = new HashSet<>();
        HashSet<String> locations = new HashSet<>();
        String[] words = userCommand.split(" ");
        for (String word : words) {
            if(builtInCommands.containsKey(word)) {
                builtInCmdTriggers.add(word);
            }else if(gameActions.containsKey(word)){
                triggers.add(word);
            }else if(gameObjects.containsKey(word) ||gameLocations.containsKey(word)){
                subjects.add(word);
            }
            if(gameLocations.containsKey(word)) {
                locations.add(word);
            }
        }
//        System.out.println("..........................................................................Parsing");
//        System.out.println("parsing the command and get: triggers: "+ triggers +"\nbuiltIntriggers: " + builtInCmdTriggers); // for test
//        System.out.println("Subjects: "+ subjects +"\nlocations: " + locations); // for test
//        System.out.println(".......................................................................parsing over");
        if(triggers.size() != 0 && builtInCmdTriggers.size() != 0){
            return "Invalid command. Which one action do you want to execute in " + triggers + " and " + builtInCmdTriggers+ " ?";
        }

        if(triggers.size() != 0){
            if(subjects.size() == 0 ){
                return "Invalid command. Which things do you wanna " + triggers + " ?";
            }
            return executeAction(triggers, subjects);
        }else if(builtInCmdTriggers.size() != 0){
            if(builtInCmdTriggers.size() > 1){
                return "Invalid command. Which one action do you want to execute in " + builtInCmdTriggers + " ?";
            }
            String builtInCmdTrigger = (String)builtInCmdTriggers.toArray()[0];
            return executeBuiltInCmd(builtInCmdTrigger, subjects, locations);
        }
        return "Invalid command. Please check your spelling.";
    }

    String executeBuiltInCmd(String builtInCmdTrigger, HashSet<String> subjects, HashSet<String> locations){
        switch (builtInCmdTrigger) {
            case "inv", "inventory" -> {
                if (subjects.size() != 0) {
                    return "Invalid [inv] command. Just use inventory/inv.";
                }
                return BuiltInCommands.InventoryCommand.execute(this);
            }
            case "get" -> {
                if (subjects.size() != 1 || locations.size() != 0) {
                    return "Invalid [get] command. Which thing do you wanna get? you can only get one thing at a time.";
                }
                return BuiltInCommands.GetCommand.executeWithObject((String) subjects.toArray()[0], this);
            }
            case "drop" -> {
                if (subjects.size() != 1 || locations.size() != 0) {
                    return "Invalid [drop] command. Which thing do you wanna drop? you can only drop one thing at a time.";
                }
                return BuiltInCommands.DropCommand.executeWithObject((String) subjects.toArray()[0], this);
            }
            case "look" -> {
                if (subjects.size() != 0 ){
                    return "Invalid [look] command. only use look.";
                }
                return BuiltInCommands.LookCommand.execute(this);
            }
            case "goto" -> {
                if (subjects.size() != 1 || locations.size() != 1) {
                    return "Invalid [goto] command.Please make sure that you have input only one and a correct place.";
                }
                return BuiltInCommands.GotoCommand.executeWithObject((String) locations.toArray()[0], this);
            }
            case "health" -> {
                if (subjects.size() != 0) {
                    return "Invalid [health] command. only use health.";
                }
                return BuiltInCommands.HealthCommand.execute(this);
            }
        }
        return "Invalid command.";
    }
    //If we have found several triggers, then use the first one to find a basicAction which contains all subjects.
    //And check if the rest triggers are valid(all triggers should only for this action)
    String executeAction(HashSet<String> triggers, HashSet<String> subjects){
        String basicTrigger = (String)triggers.toArray()[0];
        GameAction basicAction;
        try{
            basicAction = findTargetAction(basicTrigger, subjects);
        }catch (GameException ce){
            return ce.getMessage();
        }
        //check if the rest triggers are valid(all triggers should only for this action)
        if(!actionTrigger.get(basicAction).containsAll(triggers)){
            return "Invalid command. Which one action do you want to execute in " + triggers + " ?";
        }
        //execute the action
        return basicAction.execute(this);
    }

    GameAction findTargetAction(String trigger, HashSet<String> subjects) throws GameException{
        for(GameAction action: gameActions.get(trigger)){
            if(action.getSubjects().containsAll(subjects)){
                return action;
            }
        }
        //Which means this trigger can trigger different actions. So give some tips for user to choose which action to execute.
        String possibleActions = "";
        for(String subject: subjects){
            possibleActions += trigger + " " + subject + "? ";
        }
        throw new GameException.InvalidCommandException(possibleActions);
    }

    //  === Methods below are there to facilitate server related operations. ===

    /**
    * Starts a *blocking* socket server listening for new connections. This method blocks until the
    * current thread is interrupted.
    *
    * <p>This method isn't used for marking. You shouldn't have to modify this method, but you can if
    * you want to.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Handles an incoming connection from the socket server.
    *
    * <p>This method isn't used for marking. You shouldn't have to modify this method, but you can if
    * * you want to.
    *
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
