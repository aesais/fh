package pl.fhframework;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import pl.fhframework.io.TemporaryResource;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for data shared between user sessions in the same http session
 */
@Getter
@RequiredArgsConstructor
public class UserSessionSharedData {
    private final Map<String, TemporaryResource> uploadFileIndexes = new HashMap<>();
    private final Map<String, Resource> downloadFileIndexes = new HashMap<>();

    private final String httpSessionId;
}
