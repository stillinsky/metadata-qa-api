package de.gwdg.metadataqa.api.calculator;

import com.jayway.jsonpath.InvalidJsonException;
import de.gwdg.metadataqa.api.counter.CompletenessCounter;
import de.gwdg.metadataqa.api.counter.FieldCounter;
import de.gwdg.metadataqa.api.interfaces.Calculator;
import de.gwdg.metadataqa.api.json.FieldGroup;
import de.gwdg.metadataqa.api.json.JsonBranch;
import de.gwdg.metadataqa.api.model.JsonPathCache;
import de.gwdg.metadataqa.api.model.XmlFieldInstance;
import de.gwdg.metadataqa.api.schema.Schema;
import de.gwdg.metadataqa.api.util.CompressionLevel;
import de.gwdg.metadataqa.api.util.Converter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Péter Király <peter.kiraly at gwdg.de>
 * @param <T>
 */
public class CompletenessCalculator<T extends XmlFieldInstance> implements Calculator, Serializable {

	public static final String CALCULATOR_NAME = "completeness";

	private static final Logger LOGGER = Logger.getLogger(CompletenessCalculator.class.getCanonicalName());

	private String inputFileName;

	private CompletenessCounter completenessCounter;
	private FieldCounter<Boolean> existenceCounter;
	private FieldCounter<Integer> cardinalityCounter;

	// private Counters counters;
	private List<String> missingFields;
	private List<String> emptyFields;
	private List<String> existingFields;
	private Schema schema;

	private boolean collectFields = false;
	private boolean completeness = true;
	private boolean existence = true;
	private boolean cardinality = true;
	

	public CompletenessCalculator() {
		// this.recordID = null;
	}

	public CompletenessCalculator(Schema schema) {
		this.schema = schema;
	}

	@Override
	public String getCalculatorName() {
		return CALCULATOR_NAME;
	}

	@Override
	public void measure(JsonPathCache cache)
			throws InvalidJsonException {
		completenessCounter = new CompletenessCounter();
		existenceCounter = new FieldCounter<>();
		cardinalityCounter = new FieldCounter<>();
		if (collectFields) {
			missingFields = new ArrayList<>();
			emptyFields = new ArrayList<>();
			existingFields = new ArrayList<>();
		}

		if (schema.getCollectionPaths().isEmpty()) {
			for (JsonBranch jsonBranch : schema.getPaths()) {
				evaluateJsonBranch(jsonBranch, cache, completenessCounter, jsonBranch.getLabel(), null);
			}
		} else {
			for (JsonBranch collection : schema.getCollectionPaths()) {
				Object rawJsonFragment = cache.getFragment(collection.getJsonPath());
				if (rawJsonFragment == null) {
					for (JsonBranch child : collection.getChildren()) {
						handleValues(completenessCounter, child, null);
					}
				} else {
					List<Object> jsonFragments = Converter.jsonObjectToList(rawJsonFragment);
					for (int i = 0, len = jsonFragments.size(); i < len; i++) {
						Object jsonFragment = jsonFragments.get(i);
						for (JsonBranch child : collection.getChildren()) {
							String address = String.format("%s/%d/%s", collection.getJsonPath(), i, child.getJsonPath());
							evaluateJsonBranch(child, cache, completenessCounter, address, jsonFragment);
						}
					}
				}
			}
		}

		for (FieldGroup fieldGroup : schema.getFieldGroups()) {
			boolean existing = false;
			for (String field : fieldGroup.getFields()) {
				if (existenceCounter.get(field) == true) {
					existing = true;
					break;
				}
			}
			completenessCounter.increaseInstance(fieldGroup.getCategory(), existing);
		}
	}

	public void evaluateJsonBranch(
			JsonBranch jsonBranch,
			JsonPathCache cache,
			CompletenessCounter completenessCounter,
			String address,
			Object jsonFragment
	) {
		List<T> values = cache.get(address, jsonBranch.getJsonPath(), jsonFragment);
		handleValues(completenessCounter, jsonBranch, values);
	}

	private void handleValues(CompletenessCounter completenessCounter, JsonBranch jsonBranch, List<T> values) {
		if (completeness)
			completenessCounter.increaseTotal(jsonBranch.getCategories());

		if (values != null && !values.isEmpty()) {
			handleNonNullValues(completenessCounter, jsonBranch, values);
		} else {
			handleNullValues(jsonBranch);
		}
	}

	private void handleNonNullValues(
			CompletenessCounter completenessCounter,
			JsonBranch jsonBranch,
			List<T> values
	) {
		final String label = jsonBranch.getLabel();

		if (completeness)
			completenessCounter.increaseInstance(jsonBranch.getCategories());

		if (existence)
			existenceCounter.put(label, true);

		if (cardinality)
			if (!cardinalityCounter.has(label)) {
				cardinalityCounter.put(label, values.size());
			} else {
				cardinalityCounter.put(label, values.size() + cardinalityCounter.get(label));
			}

		if (collectFields)
			existingFields.add(label);
	}

	private void handleNullValues(JsonBranch jsonBranch) {
		if (existence && !existenceCounter.has(jsonBranch.getLabel()))
			existenceCounter.put(jsonBranch.getLabel(), false);
		if (cardinality && !cardinalityCounter.has(jsonBranch.getLabel()))
			cardinalityCounter.put(jsonBranch.getLabel(), 0);
		if (collectFields) {
			if (!missingFields.contains(jsonBranch.getLabel()))
				missingFields.add(jsonBranch.getLabel());
		}
	}

	public void collectFields(boolean collectFields) {
		this.collectFields = collectFields;
	}

	public List<String> getMissingFields() {
		return missingFields;
	}

	public List<String> getEmptyFields() {
		return emptyFields;
	}

	public List<String> getExistingFields() {
		return existingFields;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	@Override
	public Map<String, ? extends Object> getResultMap() {
		Map<String, Object> resultMap = new LinkedHashMap<>();

		if (completeness)
			resultMap.putAll(completenessCounter.getFieldCounter().getMap());

		if (existence)
			for (Entry e : existenceCounter.getMap().entrySet())
				resultMap.put("existence:" + e.getKey(), e.getValue());

		if (cardinality)
			for (Entry e : cardinalityCounter.getMap().entrySet())
				resultMap.put("cardinality:" + e.getKey(), e.getValue());

		return resultMap;
	}

	@Override
	public Map<String, Map<String, ? extends Object>> getLabelledResultMap() {
		Map<String, Map<String, ? extends Object>> resultMap = new LinkedHashMap<>();
		if (completeness)
			resultMap.put("completeness", completenessCounter.getFieldCounter().getMap());
		if (existence)
			resultMap.put("existence", existenceCounter.getMap());
		if (cardinality)
			resultMap.put("cardinality", cardinalityCounter.getMap());
		return resultMap;
	}

	@Override
	public String getCsv(boolean withLabel, CompressionLevel compressionLevel) {
		List<String> csvs = new ArrayList<>();
		if (completeness)
			csvs.add(completenessCounter.getFieldCounter().getList(withLabel, compressionLevel));

		if (existence)
			csvs.add(existenceCounter.getList(withLabel, compressionLevel));

		if (cardinality)
			csvs.add(cardinalityCounter.getList(withLabel, compressionLevel));

		return StringUtils.join(csvs, ",");
	}

	@Override
	public List<String> getHeader() {
		List<String> headers = new ArrayList<>();

		if (completeness)
			for (String name : CompletenessCounter.getHeaders())
				headers.add("completeness:" + name);

		if (existence)
			for (JsonBranch jsonBranch : schema.getPaths())
				if (!jsonBranch.isCollection())
					headers.add("existence:" + jsonBranch.getLabel());

		if (cardinality)
			for (JsonBranch jsonBranch : schema.getPaths())
				if (!jsonBranch.isCollection())
					headers.add("cardinality:" + jsonBranch.getLabel());

		return headers;
	}

	public Map<String, Boolean> getExistenceMap() {
		return existenceCounter.getMap();
	}

	public Map<String, Integer> getCardinalityMap() {
		return cardinalityCounter.getMap();
	}

	public CompletenessCounter getCompletenessCounter() {
		return completenessCounter;
	}

	public FieldCounter<Boolean> getExistenceCounter() {
		return existenceCounter;
	}

	public FieldCounter<Integer> getCardinalityCounter() {
		return cardinalityCounter;
	}

	public boolean isCompleteness() {
		return completeness;
	}

	public void setCompleteness(boolean completeness) {
		this.completeness = completeness;
	}

	public boolean isExistence() {
		return existence;
	}

	public void setExistence(boolean existence) {
		this.existence = existence;
	}

	public boolean isCardinality() {
		return cardinality;
	}

	public void setCardinality(boolean cardinality) {
		this.cardinality = cardinality;
	}

	public Schema getSchema() {
		return schema;
	}
}
