package com.rbmhtechnology.vind.solr.suggestion.result;

/**
 * Represents a simple facet POJO
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
class Facet {
    String name,value;
    int count;

    public Facet(String name, String value, int count) {
        this.name = name;
        this.value = value;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean equals(Object o) {
        try {
            return ((Facet)o).name.equals(this.name) && ((Facet)o).value.equals(this.value);
        } catch (Exception e) {
          return false;
        }
    }
}