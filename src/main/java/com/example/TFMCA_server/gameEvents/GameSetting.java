package com.example.TFMCA_server.gameEvents;

//https://stackoverflow.com/questions/2497521/implementing-tostring-on-java-enums
public enum GameSetting {
    CORPORATE_ERA("corporateEra"),
    PRELUDE("prelude"),
    VENUS("venus"),
    COLONIES("colonies"),
    TURMOIL("turmoil"),
    EXTRA_CORPORATIONS("extraCorporations"),
    WORLD_GOVERNMENT_TERRAFORMING("worldGovernmentTerraforming"),
    MUST_MAX_VENUS("mustMaxVenus"),
    TURMOIL_TERRAFORMING_REVISION("turmoilTerraformingRevision");

    private String string;

    GameSetting(String name){string = name;}

    @Override
    public String toString() {
        return string;
    }
}