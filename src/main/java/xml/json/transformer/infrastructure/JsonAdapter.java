package xml.json.transformer.infrastructure;

public interface JsonAdapter {
    void writeJson(Object data, String path) throws Exception;
}
