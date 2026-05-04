package xml.json.transformer.application.model;

import java.nio.file.Path;

public record ProcessAttachmentResult(
        Status status,
        Path outputDirectory,
        String xmlOutputPath,
        String jsonOutputPath
) {
    public enum Status {
        COMPLETED,
        CANCELLED,
        BACK
    }
}
