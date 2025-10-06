package ch.swaford.servermanager.classement;

import ch.swaford.servermanager.PlayerDataBase;

import java.util.HashMap;
import java.util.Map;

public class ClassementCache {
    //Cache de classement à action rapide ex : recolte de plantes
    public final static Map<String, Integer> agricultureCache = new HashMap<>();
    public final static Map<String, Integer> minageCache = new HashMap<>();
    public final static Map<String, Integer> commerceCache = new HashMap<>();


    //Met à jour les classement
    public static void SaveClassement() {
        SaveAggriculture();
        SaveMinage();
        SaveCommerceScore();
    }

    //Créer une snapshot du cache, clear le cache, et applique le nouveau score à chaque faction
    private static void SaveAggriculture() {
        Map<String, Integer> snapshot = new HashMap<>(agricultureCache);
        agricultureCache.clear();

        if (!ClassementManager.getStatusOfClassement("nation_agriculture")) {
            return;
        }

        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            String factionName = PlayerDataBase.getPlayerFaction(entry.getKey());
            //récupère le nation du joueur et incrémente le score. Créer une nouvelle entrée, si la nation n'est pas en cache.
            map.merge(factionName, entry.getValue(), Integer::sum);
        }

        //applique le nouveau score à chaque nation de chaque joueur en cache
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            ClassementManager.updateNationScore("nation_agriculture", entry.getKey(), entry.getValue());
        }
    }

    private static void SaveMinage() {
        Map<String, Integer> snapshot = new HashMap<>(minageCache);
        minageCache.clear();

        if (!ClassementManager.getStatusOfClassement("nation_minage")) {
            return;
        }

        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            String factionName = PlayerDataBase.getPlayerFaction(entry.getKey());
            //récupère le nation du joueur et incrémente le score. Créer une nouvelle entrée, si la nation n'est pas en cache.
            map.merge(factionName, entry.getValue(), Integer::sum);
        }

        //applique le nouveau score à chaque nation de chaque joueur en cache
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            ClassementManager.updateNationScore("nation_minage", entry.getKey(), entry.getValue());
        }
    }

    private static void SaveCommerceScore() {
        Map<String, Integer> snapshot = new HashMap<>(commerceCache);
        commerceCache.clear();

        if (!ClassementManager.getStatusOfClassement("nation_commerce")) {
            return;
        }

        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            String factionName = PlayerDataBase.getPlayerFaction(entry.getKey());
            //récupère le nation du joueur et incrémente le score. Créer une nouvelle entrée, si la nation n'est pas en cache.
            map.merge(factionName, entry.getValue(), Integer::sum);
        }

        //applique le nouveau score à chaque nation de chaque joueur en cache
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            ClassementManager.updateNationScore("nation_commerce", entry.getKey(), entry.getValue());
        }
    }
}
