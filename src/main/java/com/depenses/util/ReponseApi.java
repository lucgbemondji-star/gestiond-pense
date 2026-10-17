package com.depenses.util;

import java.util.HashMap;
import java.util.Map;

public final class ReponseApi {
    private ReponseApi() {
    }

    public static Map<String, Object> succes(Object donnees) {
        return reponse(true, donnees, null);
    }

    public static Map<String, Object> erreur(String message) {
        return reponse(false, null, message);
    }

    public static Map<String, Object> reponse(boolean succes, Object donnees, String erreur) {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("succes", succes);
        reponse.put("donnees", donnees);
        reponse.put("erreur", erreur);
        return reponse;
    }
}
