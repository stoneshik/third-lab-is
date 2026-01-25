package lab.is.services.insertion.history;

import java.io.InputStream;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadFile {
    private String name;
    private InputStream inputStream;
}
