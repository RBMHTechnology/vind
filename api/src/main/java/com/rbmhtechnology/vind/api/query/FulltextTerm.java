package com.rbmhtechnology.vind.api.query;

public class FulltextTerm {
  private final String fulltextSearchTerm;
  private final String minimumMatch;

  public FulltextTerm(String fulltextSearchTerm, String minimumMatch) {
    this.fulltextSearchTerm = fulltextSearchTerm;
    this.minimumMatch = minimumMatch;
  }

  public String getFulltextSearchTerm() {
    return fulltextSearchTerm;
  }

  public String getMinimumMatch() {
    return minimumMatch;
  }

  public FulltextTerm copy() {
    return new FulltextTerm(getFulltextSearchTerm(), getMinimumMatch());
  }
}
