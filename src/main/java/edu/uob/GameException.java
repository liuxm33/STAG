package edu.uob;

import java.io.Serial;
public class GameException extends Exception{
    @Serial
    private static final long serialVersionUID = 1;
    public GameException(String message){
        super(message);
    }

    public static class InvalidUserNameException extends GameException {
        @Serial private static final long serialVersionUID = 1;
        public InvalidUserNameException(String name) {
            super(name + " is an invalid user name! Name can only consist of letters, spaces, apostrophes and hyphens.");
        }
    }

    public static class PlayerHealthLevelException extends GameException {
        @Serial private static final long serialVersionUID = 1;
        public PlayerHealthLevelException() {
            super("you died and lost all of your items, you must return to the start of the game");
        }
    }

    public static class InvalidCommandException extends GameException {
        @Serial private static final long serialVersionUID = 1;
        public InvalidCommandException(String correctCommand) {
            super("Invalid command! Do you mean: " + correctCommand);
        }
    }
}

