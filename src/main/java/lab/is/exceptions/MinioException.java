package lab.is.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class MinioException extends RuntimeException {
    public MinioException(String message) {
        super(message);
    }
}
