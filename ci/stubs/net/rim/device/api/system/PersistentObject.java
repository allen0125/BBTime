package net.rim.device.api.system;

public interface PersistentObject {
    Object getContents();
    void setContents(Object object);
    void commit();
}
