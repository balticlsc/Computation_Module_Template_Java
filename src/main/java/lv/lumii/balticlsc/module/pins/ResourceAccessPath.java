package lv.lumii.balticlsc.module.pins;

public class ResourceAccessPath implements IAccessPath {
    private String ResourcePath;

    ResourceAccessPath(String path) {
        this.ResourcePath = path;
    }
    @Override
    public String getPath() {
        return ResourcePath;
    }
}
