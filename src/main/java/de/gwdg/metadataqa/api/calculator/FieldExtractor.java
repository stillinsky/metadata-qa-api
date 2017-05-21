package de.gwdg.metadataqa.api.calculator;

import com.jayway.jsonpath.InvalidJsonException;
import de.gwdg.metadataqa.api.counter.FieldCounter;
import de.gwdg.metadataqa.api.interfaces.Calculator;
import de.gwdg.metadataqa.api.model.JsonPathCache;
import de.gwdg.metadataqa.api.model.XmlFieldInstance;
import de.gwdg.metadataqa.api.schema.Schema;
import de.gwdg.metadataqa.api.util.CompressionLevel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Péter Király <peter.kiraly at gwdg.de>
 */
public class FieldExtractor implements Calculator, Serializable {

	public static final String CALCULATOR_NAME = "fieldExtractor";

	private static final Logger logger = Logger.getLogger(FieldExtractor.class.getCanonicalName());

	public String FIELD_NAME = "recordId";
	private String idPath;
	protected FieldCounter<String> resultMap;
	protected Schema schema;

	public FieldExtractor() {
	}

	public FieldExtractor(Schema schema) {
		this.schema = schema;
		setIdPath(schema.getExtractableFields().get(FIELD_NAME));
	}

	public FieldExtractor(String idPath) {
		this.idPath = idPath;
	}

	@Override
	public String getCalculatorName() {
		return CALCULATOR_NAME;
	}

	@Override
	public void measure(JsonPathCache cache)
			throws InvalidJsonException {
		resultMap = new FieldCounter<>();
		List<XmlFieldInstance> instances = cache.get(getIdPath());
		if (instances == null || instances.size() == 0) {
			logger.severe("No record ID in " + cache.getJsonString());
			resultMap.put(FIELD_NAME, "");
			return;
		}
		String recordId = instances.get(0).getValue();
		cache.setRecordId(recordId);
		resultMap.put(FIELD_NAME, recordId);
		if (schema != null) {
			String path;
			for (String fieldName : schema.getExtractableFields().keySet()) {
				if (!fieldName.equals(FIELD_NAME)) {
					path = schema.getExtractableFields().get(fieldName);
					List<XmlFieldInstance> values = (List<XmlFieldInstance>) cache.get(path);
					String value = null;
					if (values == null || values.isEmpty() || values.size() == 0 || values.get(0) == null || values.get(0).getValue() == null) {
						logger.warning("Null value in field: " + fieldName + " (" + path + ")");
						value = null;
					} else {
						value = values.get(0).getValue();
					}
					resultMap.put(fieldName, value);
				}
			}
		}
	}

	public String getIdPath() {
		return idPath;
	}

	public void setIdPath(String idPath) {
		this.idPath = idPath;
	}

	@Override
	public Map<String, ? extends Object> getResultMap() {
		return resultMap.getMap();
	}

	@Override
	public Map<String, Map<String, ? extends Object>> getLabelledResultMap() {
		Map<String, Map<String, ? extends Object>> labelledResultMap = new LinkedHashMap<>();
		labelledResultMap.put(getCalculatorName(), resultMap.getMap());
		return labelledResultMap;
	}

	@Override
	public String getCsv(boolean withLabel, CompressionLevel compressionLevel) {
		return resultMap.getList(withLabel, CompressionLevel.ZERO); // the extracted fields should never be compressed!
	}

	@Override
	public List<String> getHeader() {
		List<String> headers = new ArrayList<>();
		headers.add(FIELD_NAME);
		return headers;
	}

}
