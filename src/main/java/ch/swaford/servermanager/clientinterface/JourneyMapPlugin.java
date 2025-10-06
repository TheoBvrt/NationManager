package ch.swaford.servermanager.clientinterface;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;

@journeymap.api.v2.client.JourneyMapPlugin(apiVersion = "2.0.0-SNAPSHOT")
public class JourneyMapPlugin implements IClientPlugin {

    private static IClientAPI jmApi;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        jmApi = jmClientApi;
    }

    @Override
    public String getModId() {
        return "servermanager";
    }

    public static IClientAPI getJmApi() {
        return jmApi;
    }
}
