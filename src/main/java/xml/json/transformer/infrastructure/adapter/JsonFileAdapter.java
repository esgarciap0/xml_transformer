package xml.json.transformer.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import xml.json.transformer.application.port.out.JsonOutputPort;

import java.io.File;

public class JsonFileAdapter implements JsonOutputPort {

    private final ObjectMapper mapper;

    public JsonFileAdapter() {
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    @Override
    public void writeJson(Object data, String path) throws Exception {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), data);
        System.out.println("JSON generado correctamente: " + path);
    }
}
