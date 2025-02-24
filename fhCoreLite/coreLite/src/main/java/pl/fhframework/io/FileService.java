package pl.fhframework.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.fhframework.UserSessionSharedData;
import pl.fhframework.core.logging.FhLogger;
import pl.fhframework.UserSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public final class FileService {
    private final static String TEMP_DIR_PREFIX = "FH";

    private static Path tempDirectory;

    public String save(MultipartFile file, UserSessionSharedData userSessionSharedData) throws IOException {
        String fileName = cutFileName(file.getOriginalFilename());
        Pair<String, TemporaryResource> temporaryResource = createNewTemporaryResource(fileName, userSessionSharedData);
        temporaryResource.getSecond().setContentType(file.getContentType());
        File temporaryFile = temporaryResource.getSecond().getFile();
        file.transferTo(temporaryFile.toPath());
        FhLogger.info(this.getClass(), "File {} ({} bytes) uploaded as {}", file.getOriginalFilename(), file.getSize(), temporaryFile.toString());
        return temporaryResource.getFirst();
    }

    public Path generateHolder(String fileName, UserSessionSharedData userSessionSharedData) throws IOException {
        String uuid = UUID.randomUUID().toString();
        Path tempFilePath = Files.createTempFile(getTempDirectoryInstance(), fileName + TemporaryResource.TEMP_FILE_PREFIX + uuid, "");
        File temporaryFile = tempFilePath.toFile();
        userSessionSharedData.getUploadFileIndexes().put(uuid, new TemporaryResource(temporaryFile));
        return tempFilePath;
    }

    private String cutFileName(String originalFilename) {
        return FilenameUtils.getBaseName(originalFilename) + "." + FilenameUtils.getExtension(originalFilename);
    }

    public TemporaryResource getResource(String fileId, UserSessionSharedData userSessionSharedData) {
        return userSessionSharedData.getUploadFileIndexes().get(fileId);
    }

    public Path getTempDirectoryInstance() throws IOException {
        if (tempDirectory == null || Files.notExists(tempDirectory)) {
            tempDirectory = Files.createTempDirectory(TEMP_DIR_PREFIX).toAbsolutePath();
        }
        return tempDirectory;
    }

    public void deleteAllTemporaryFiles() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        final File[] files = new File(tempDir).listFiles(f -> f.isDirectory() && f.getName().startsWith(TEMP_DIR_PREFIX));
        for (File directory : files) {
            FileUtils.deleteDirectory(directory);
        }
    }

    public void deleteUserTemporaryFiles(UserSession userSession) {
        userSession.getSharedData().getUploadFileIndexes().values().forEach(r -> r.getFile().delete());
    }

    public Pair<String, TemporaryResource> createNewTemporaryResource(String fileName, UserSessionSharedData userSessionSharedData) throws IOException {
        Path tempFilePath = Files.createTempFile(getTempDirectoryInstance(), fileName + TemporaryResource.TEMP_FILE_PREFIX, "");
        File temporaryFile = tempFilePath.toFile();
        String uuid = UUID.randomUUID().toString();
        TemporaryResource temporaryResource = new TemporaryResource(temporaryFile);
        userSessionSharedData.getUploadFileIndexes().put(uuid, temporaryResource);
        return Pair.of(uuid, temporaryResource);
    }
}
