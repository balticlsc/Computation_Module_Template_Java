package lv.lumii.balticlsc.module.pins;

public class Pin {
    private String PinName;
    private String PinType; // input, output
    private String AccessType; // (NoSQL_DB, RelationalDB, MySQL, FTP) -> AccessCredential : Host,Port,User,Password   AccessPath: ResourcePath
                               // (MongDB) ->  AccessCredential : Host,Port,User,Password   AccessPath: Database, Collection, ObjectId
                               // (AzuerLake) -> AccessCredential = {AccountName, ClientId, ClientSecret, TenantId, FileSystemName}, AccessPath = {ResourcePath}
                               // (AWS3) -> AccessCredential = {AccessKey, SecretKey, BucketRegion, BucketName}, AccessPath = {ResourcePath}
                               // (FileUpload) -> AccessCredential empty, AccessPath = {LocalPath}
                               // (Direct) -> AccessCredential empty, AccessPath empty
    private String DataMultiplicity; // single, multiple
    private String TokenMultiplicity; // single, multiple

    private IAccessCredential AccessCredential;
    private IAccessPath AccessPath;

    Pin() {}

    public String getPinName() {
        return PinName;
    }

    public void setPinName(String pinName) {
        PinName = pinName;
    }

    public String getPinType() {
        return PinType;
    }

    public void setPinType(String pinType) {
        PinType = pinType;
    }

    public String getAccessType() {
        return AccessType;
    }

    public void setAccessType(String accessType) {
        AccessType = accessType;
    }

    public String getDataMultiplicity() {
        return DataMultiplicity;
    }

    public void setDataMultiplicity(String dataMultiplicity) {
        DataMultiplicity = dataMultiplicity;
    }

    public String getTokenMultiplicity() {
        return TokenMultiplicity;
    }

    public void setTokenMultiplicity(String tokenMultiplicity) {
        TokenMultiplicity = tokenMultiplicity;
    }

    public IAccessCredential getAccessCredential() {
        return AccessCredential;
    }

    public void setAccessCredential(IAccessCredential accessCredential) {
        AccessCredential = accessCredential;
    }

    public IAccessPath getAccessPath() {
        return AccessPath;
    }

    public void setAccessPath(IAccessPath accessPath) {
        AccessPath = accessPath;
    }
}
