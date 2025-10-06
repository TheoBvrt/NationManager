package ch.swaford.servermanager;

public class Message {
    public static final String SUCCESS_NATION_CREATE = "§eVous venez de créer la nation ";
    public static final String SUCCESS_NATION_DELETE = "§eVous venez de supprimer votre nation";

    public static final String ERROR_PLAYER_ALREADY_HAS_FACTION = "§cVous appartenez déjà à une nation";
    public static final String ERROR_FACTION_ALREADY_EXIST = "§cCette nation existe déjà !";
    public static final String ERROR_FACTION_PERMISSION = "§cImpossible de gérer une nation qui ne vous appartient pas";
    public static final String ERROR_PLAYER_DONT_HAVE_FACTION = "§cVous ne faites partie d’aucune nation";
    public static final String ERROR_YOU_CANT_QUIT_YOUR_FACTION = "§cVous ne pouvez pas quitter une nation tant que vous en êtes le chef";
}
