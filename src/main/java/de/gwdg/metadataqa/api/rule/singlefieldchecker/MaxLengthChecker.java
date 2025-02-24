package de.gwdg.metadataqa.api.rule.singlefieldchecker;

import de.gwdg.metadataqa.api.counter.FieldCounter;
import de.gwdg.metadataqa.api.json.JsonBranch;
import de.gwdg.metadataqa.api.model.XmlFieldInstance;
import de.gwdg.metadataqa.api.model.pathcache.PathCache;
import de.gwdg.metadataqa.api.rule.RuleCheckerOutput;
import de.gwdg.metadataqa.api.rule.RuleCheckingOutputType;

import java.util.List;

public class MaxLengthChecker extends SingleFieldChecker {

  private static final long serialVersionUID = -3991731520985560338L;
  public static final String PREFIX = "maxLength";
  protected Integer maxLength;

  public MaxLengthChecker(JsonBranch field, Integer maxLength) {
    this(field, field.getLabel(), maxLength);
  }

  public MaxLengthChecker(JsonBranch field, String header, Integer maxLength) {
    super(field, header + ":" + PREFIX);
    this.maxLength = maxLength;
  }

  @Override
  public void update(PathCache cache, FieldCounter<RuleCheckerOutput> results, RuleCheckingOutputType outputType) {
    var allPassed = true;
    var isNA = true;
    List<XmlFieldInstance> instances = cache.get(field.getJsonPath());
    if (instances != null && !instances.isEmpty()) {
      for (XmlFieldInstance instance : instances) {
        if (instance.hasValue()) {
          isNA = false;
          if (instance.getValue().length() > maxLength) {
            allPassed = false;
            break;
          }
        }
      }
    }
    addOutput(results, isNA, allPassed, outputType);
  }

}
