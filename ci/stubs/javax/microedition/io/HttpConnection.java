package javax.microedition.io;

import java.io.IOException;

public interface HttpConnection extends Connection {
    String HEAD = "HEAD";
    String GET = "GET";
    String POST = "POST";

    void setRequestMethod(String method) throws IOException;
    void setRequestProperty(String key, String value) throws IOException;
    int getResponseCode() throws IOException;
    long getDate() throws IOException;
    String getHeaderField(String name) throws IOException;
}
