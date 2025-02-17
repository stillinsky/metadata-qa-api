package de.gwdg.metadataqa.api.calculator;

import com.jayway.jsonpath.InvalidJsonException;
import de.gwdg.metadataqa.api.calculator.language.Multilinguality;
import de.gwdg.metadataqa.api.counter.FieldCounter;
import de.gwdg.metadataqa.api.interfaces.MetricResult;
import de.gwdg.metadataqa.api.json.JsonBranch;
import de.gwdg.metadataqa.api.model.pathcache.PathCache;
import de.gwdg.metadataqa.api.problemcatalog.FieldCounterBasedResult;
import de.gwdg.metadataqa.api.schema.Schema;
import de.gwdg.metadataqa.api.util.SkippedEntitySelector;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Péter Király <peter.kiraly at gwdg.de>
 */
public class MultilingualitySaturationCalculator extends BaseLanguageCalculator {

  public static final String CALCULATOR_NAME = "multilingualitySaturation";

  private static final Logger LOGGER = Logger.getLogger(MultilingualitySaturationCalculator.class.getCanonicalName());

  /**
   * The result type of multilinguality.
   */
  public enum ResultTypes {
    NORMAL(0),
    /**
     * Adds sum, average and normalized values.
     */
    EXTENDED(1);

    private final int value;

    ResultTypes(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }
  }

  private ResultTypes resultType = ResultTypes.NORMAL;
  private String inputFileName;

  private Schema schema;
  private SkippedEntryChecker skippedEntryChecker = null;
  private SkippedEntitySelector skippedEntitySelector = new SkippedEntitySelector();

  public MultilingualitySaturationCalculator() {
    // this.recordID = null;
  }

  public MultilingualitySaturationCalculator(Schema schema) {
    this.schema = schema;
  }

  @Override
  public String getCalculatorName() {
    return CALCULATOR_NAME;
  }

  @Override
  public List<String> getHeader() {
    List<String> headers = new ArrayList<>();
    for (JsonBranch jsonBranch : schema.getPaths()) {
      if (jsonBranch.isActive() && !schema.getNoLanguageFields().contains(jsonBranch.getLabel())) {
        switch (resultType) {
          case EXTENDED:
            headers.add("lang:sum:" + jsonBranch.getLabel());
            headers.add("lang:average:" + jsonBranch.getLabel());
            headers.add("lang:normalized:" + jsonBranch.getLabel());
            break;
          case NORMAL:
          default:
            headers.add("lang:" + jsonBranch.getLabel());
            break;
        }
      }
    }
    if (resultType.equals(ResultTypes.EXTENDED)) {
      headers.add(Multilinguality.SUM);
      headers.add(Multilinguality.AVERAGE);
    }
    headers.add(Multilinguality.NORMALIZED);

    return headers;
  }

  @Override
  public List<MetricResult> measure(PathCache cache)
      throws InvalidJsonException {

    Multilinguality multilinguality = new Multilinguality(schema, cache, resultType, skippedEntryChecker, skippedEntitySelector);
    FieldCounter<Double> saturationMap = multilinguality.measure();

    return List.of(new FieldCounterBasedResult<>(getCalculatorName(), saturationMap));
  }

  /*
  public Map<String, Double> getSaturationMap() {
    return saturationMap.getMap();
  }

  // @Override
  public Map<String, Map<String, ? extends Object>> getLabelledResultMap() {
    Map<String, Map<String, ? extends Object>> labelledResultMap = new LinkedHashMap<>();
//     labelledResultMap.put(getCalculatorName(), rawLanguageMap);
//    labelledResultMap.put(getCalculatorName(), saturationMap.getMap());
    labelledResultMap.put(getCalculatorName(), mergeMaps());
    return labelledResultMap;
  }
  */

  public ResultTypes getResultType() {
    return resultType;
  }

  public void setResultType(ResultTypes resultType) {
    this.resultType = resultType;
  }

  public SkippedEntryChecker getSkippedEntryChecker() {
    return skippedEntryChecker;
  }

  public void setSkippedEntryChecker(SkippedEntryChecker skippedEntryChecker) {
    this.skippedEntryChecker = skippedEntryChecker;
    skippedEntitySelector.setSkippedEntryChecker(skippedEntryChecker);
  }
}
