package com.rbmhtechnology.vind.monitoring.model.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbmhtechnology.vind.monitoring.model.Interface.Interface;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 01.08.16.
 */
public class InterfaceApplication implements Application {

    private String name;
    private String version;
    @JsonProperty("interface")
    private Interface iface;

    public InterfaceApplication() {
    }

    public InterfaceApplication(String name, String version, Interface iface) {
        this.name = name;
        this.version = version;
        this.iface = iface;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Interface getIface() {
        return iface;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setIface(Interface iface) {
        this.iface = iface;
    }

    @Override
    public String getId() {
        return name + " - " + version;
    }
}
