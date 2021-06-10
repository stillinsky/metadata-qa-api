package de.gwdg.metadataqa.api.rule.logical;

import de.gwdg.metadataqa.api.configuration.schema.Rule;
import de.gwdg.metadataqa.api.counter.FieldCounter;
import de.gwdg.metadataqa.api.model.PathCacheFactory;
import de.gwdg.metadataqa.api.model.pathcache.CsvPathCache;
import de.gwdg.metadataqa.api.rule.RuleChecker;
import de.gwdg.metadataqa.api.rule.RuleCheckerOutput;
import de.gwdg.metadataqa.api.rule.RuleCheckingOutputType;
import de.gwdg.metadataqa.api.rule.singlefieldchecker.MaxCountChecker;
import de.gwdg.metadataqa.api.rule.singlefieldchecker.MinCountChecker;
import de.gwdg.metadataqa.api.rule.singlefieldchecker.MinLengthChecker;
import de.gwdg.metadataqa.api.schema.BaseSchema;
import de.gwdg.metadataqa.api.schema.CsvAwareSchema;
import de.gwdg.metadataqa.api.schema.Format;
import de.gwdg.metadataqa.api.schema.Schema;
import de.gwdg.metadataqa.api.util.CsvReader;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AndCheckerTest {

  protected Schema schema;
  protected CsvPathCache cache;

  @Before
  public void setUp() throws Exception {
    schema = new BaseSchema()
      .setFormat(Format.CSV)
      .addField("name")
      .addField("title")
      .addField("alt")
    ;
    schema.getPathByLabel("name").setRule(Arrays.asList(new Rule().withAnd(Arrays.asList(new Rule().withMinCount(1), new Rule().withMaxCount(1)))));

    cache = (CsvPathCache) PathCacheFactory.getInstance(schema.getFormat(), "a,b,a");
    cache.setCsvReader(new CsvReader().setHeader(((CsvAwareSchema) schema).getHeader()));
  }

  @Test
  public void header() {
    assertEquals("and:name", schema.getRuleCheckers().get(0).getHeader());
  }

  @Test
  public void update() {
    List<RuleChecker> checkers = schema.getRuleCheckers();
    AndChecker andChecker = (AndChecker) checkers.get(0);
    assertEquals(2, andChecker.getCheckers().size());

    assertEquals(MinCountChecker.class, andChecker.getCheckers().get(0).getClass());
    MinCountChecker minCountChecker = (MinCountChecker) andChecker.getCheckers().get(0);

    assertEquals(MaxCountChecker.class, andChecker.getCheckers().get(1).getClass());
    MaxCountChecker maxCountChecker = (MaxCountChecker) andChecker.getCheckers().get(1);

    FieldCounter<RuleCheckerOutput> fieldCounter = new FieldCounter<>();
    andChecker.update(cache, fieldCounter);

    assertEquals(RuleCheckingOutputType.PASSED, fieldCounter.get("and:name").getType());
  }

  @Test
  public void failure() {
    schema.getPathByLabel("name").setRule(Arrays.asList(new Rule().withAnd(Arrays.asList(new Rule().withMinCount(1), new Rule().withMinLength(10)))));

    List<RuleChecker> checkers = schema.getRuleCheckers();
    AndChecker andChecker = (AndChecker) checkers.get(0);
    assertEquals(2, andChecker.getCheckers().size());

    assertEquals(MinCountChecker.class, andChecker.getCheckers().get(0).getClass());
    MinCountChecker minCountChecker = (MinCountChecker) andChecker.getCheckers().get(0);

    assertEquals(MinLengthChecker.class, andChecker.getCheckers().get(1).getClass());
    MinLengthChecker maxCountChecker = (MinLengthChecker) andChecker.getCheckers().get(1);

    FieldCounter<RuleCheckerOutput> fieldCounter = new FieldCounter<>();
    andChecker.update(cache, fieldCounter);

    assertEquals(RuleCheckingOutputType.FAILED, fieldCounter.get("and:name").getType());
  }
}