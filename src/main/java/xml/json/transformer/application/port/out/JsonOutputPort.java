package xml.json.transformer.application.port.out;

public interface JsonOutputPort {
    void writeJson(Object data, String path) throws Exception;
}
