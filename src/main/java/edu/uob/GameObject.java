package edu.uob;

public class GameObject extends GameEntity{
    private String currentLocation;
    private ObjectType type;
    public GameObject(String name, String description, ObjectType type, String locationName) {
        super(name, description);
        this.type = type;
        this.currentLocation = locationName;
    }
    public String getCurrentLocation() {
        return currentLocation;
    }
    public void setCurrentLocation(String location) {
        this.currentLocation = location;
    }
    public ObjectType getObjectType() {
        return type;
    }
}
