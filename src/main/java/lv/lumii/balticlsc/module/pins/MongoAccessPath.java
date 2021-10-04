package lv.lumii.balticlsc.module.pins;

public class MongoAccessPath implements IAccessPath {
    private String Database;
    private String Collection;
    private String ObjectId;

    MongoAccessPath(String database, String collection, String objectId) {
        this.setDatabase(database);
        this.setCollection(collection);
        this.setObjectId(objectId);
    }

    @Override
    public String getPath() {
        //TODO
        return this.getDatabase() + "/" + this.getCollection() + "/" + getObjectId();
    }

    public String getDatabase() {
        return Database;
    }

    public void setDatabase(String database) {
        Database = database;
    }

    public String getCollection() {
        return Collection;
    }

    public void setCollection(String collection) {
        Collection = collection;
    }

    public String getObjectId() {
        return ObjectId;
    }

    public void setObjectId(String objectId) {
        ObjectId = objectId;
    }
}
