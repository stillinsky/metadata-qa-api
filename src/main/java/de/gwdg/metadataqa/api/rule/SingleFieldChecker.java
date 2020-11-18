package de.gwdg.metadataqa.api.rule;

import de.gwdg.metadataqa.api.json.JsonBranch;

public abstract class SingleFieldChecker implements RuleChecker {

  protected JsonBranch field;
  protected String header;

  public SingleFieldChecker(JsonBranch field, String header) {
    this.field = field;
    this.header = header;
  }

  @Override
  public String getHeader() {
    return header;
  }

}
