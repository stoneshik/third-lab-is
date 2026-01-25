package lab.is.services.importfile;

import java.io.InputStream;

public interface ImportFileStorage {
    String prepare(InputStream data, String filename);
    void commit(String objectKey);
    void rollback(String objectKey);
}
