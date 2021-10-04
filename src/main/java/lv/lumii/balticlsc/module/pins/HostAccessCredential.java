package lv.lumii.balticlsc.module.pins;

public class HostAccessCredential implements IAccessCredential {
    private String Host;
    private String Port;
    private String User;
    private String Password;

    HostAccessCredential(String host, String port, String user, String password) {
        this.setHost(host);
        this.setPort(port);
        this.setUser(user);
        this.setPassword(password);
    }

    @Override
    public String getConnectionString() {
        //TODO Mongo
        return "mongodb://"+getUser() + ":" +
                getPassword() + "@" + getHost() + ":" +
                getPort();
    }

    public String getHost() {
        return Host;
    }

    public void setHost(String host) {
        Host = host;
    }

    public String getPort() {
        return Port;
    }

    public void setPort(String port) {
        Port = port;
    }

    public String getUser() {
        return User;
    }

    public void setUser(String user) {
        User = user;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }


}
