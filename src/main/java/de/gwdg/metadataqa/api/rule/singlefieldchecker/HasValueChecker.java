package de.gwdg.metadataqa.api.rule.singlefieldchecker;

import de.gwdg.metadataqa.api.counter.FieldCounter;
import de.gwdg.metadataqa.api.json.JsonBranch;
import de.gwdg.metadataqa.api.model.XmlFieldInstance;
import de.gwdg.metadataqa.api.model.pathcache.PathCache;
import de.gwdg.metadataqa.api.rule.RuleCheckerOutput;
import de.gwdg.metadataqa.api.rule.RuleCheckingOutputType;

import java.util.List;

public class HasValueChecker extends SingleFieldChecker {

  private static final long serialVersionUID = 1114999259831619599L;
  public static final String PREFIX = "hasValue";
  protected String fixedValue;

  /**
   * @param field The field
   * @param fixedValue The fixed value  check against
   */
  public HasValueChecker(JsonBranch field, String fixedValue) {
    this(field, field.getLabel(), fixedValue);
  }

  public HasValueChecker(JsonBranch field, String header, String fixedValue) {
    super(field, header + ":" + PREFIX);
    this.fixedValue = fixedValue;
  }

  @Override
  public void update(PathCache cache, FieldCounter<RuleCheckerOutput> results, RuleCheckingOutputType outputType) {
    var allPassed = false;
    var isNA = true;
    List<XmlFieldInstance> instances = cache.get(field.getJsonPath());
    if (instances != null && !instances.isEmpty()) {
      for (XmlFieldInstance instance : instances) {
        if (instance.hasValue()) {
          isNA = false;
          if (instance.getValue().equals(fixedValue)) {
            allPassed = true;
            break;
          }
        }
      }
    }
    addOutput(results, isNA, allPassed, outputType);
  }

}
