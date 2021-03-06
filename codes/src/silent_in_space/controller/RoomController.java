package silent_in_space.controller;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import silent_in_space.model.Characters.NPC;
import silent_in_space.model.Doors.Door;
import silent_in_space.model.Doors.LockedDoor;
import silent_in_space.model.Items.Computer;
import silent_in_space.model.Items.HealthStation;
import silent_in_space.model.Items.Item;
import silent_in_space.model.Location.Room;
import silent_in_space.model.Utils.Scalar2D;
import silent_in_space.view.*;

import java.io.IOException;
import java.util.Set;

import static silent_in_space.controller.GameController.DEFAULT_ROOMS_SIZE;


/* -----------------------------------------------------------------------------
 * Contrôleur des pièces du jeu:
 *
 * Rôle: S'occupe de charger les pièces du jeu. Elle charge aussi des événements
 * liés aux objets/portes/... du jeu tant que ces événements ne deviennent pas
 * trop complexes. Si c'est le cas, ces-derniers sont relayés à des contrôleurs
 * spécialisés qui connaissent ce contrôleur (comme pour le contrôleur de
 * l'inventaire)
 * ----------------------------------------------------------------------------- */

public class RoomController {
    private final GameController gameController;
    private Room currentRoomModel;
    private RoomView currentRoomView;
    
    //=============== CONSTRUCTEURS/INITIALISEURS ===============
    public RoomController(GameController c) {
        gameController = c;

        //On charge la première pièce:
        this.updateRoomView(DEFAULT_ROOMS_SIZE.getScalar2DCol(), DEFAULT_ROOMS_SIZE.getScalar2DLine());
    }


    //====================== GETTERS ==========================
    public Room getCurrentRoomModel() { return currentRoomModel; }
    public RoomView getCurrentRoomView() { return currentRoomView; }

    //====================== UPDATERS =========================

    // Crée le visuel d'un Container dans la piece (Ordinateur, Station de Vie...)
    public void addContainerInRoom(Item item, int col, int line){
        ContainerView containerView = new ContainerView("HealthStation");

        containerView.setOnMousePressed(e -> {
            if (e.isSecondaryButtonDown())
                item.describe();
            else {
                if (item instanceof HealthStation) {
                    item.isUsedOn(gameController.getPlayerModel());
                    gameController.getActorController().updatePlayerFrame();
                }
                else if(item instanceof Computer){
                    Computer computer = (Computer) item;
                    try {
                        new ComputerController(computer, gameController);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
                else
                    item.isUsed(gameController.getPlayerModel());
            }
        });

        currentRoomView.addInRoom(containerView, item.getTag(), col, line, "CENTER");
    }

    // Ajout un item dans la pièce, et gestion de ses réactions
    public void addItemInRoom(Item item, int col, int line){
        ItemView itemView = new ItemView();

        itemView.setOnMousePressed(e -> {
            if (e.isSecondaryButtonDown())
                item.describe();
            else
                gameController.getInventoryController().addInInventory(item);
        });

        currentRoomView.addInRoom(itemView, item.getTag(), col, line, "CENTER");
    }

    // Mise à jour de la vue de la pièce:
    public void updateRoomView(int nbCol, int nbLignes) {
        //On récupère le modèle:
        currentRoomModel = gameController.getPlayerModel().getRoom();

        //On met à jour la vue:
        gameController.getGameView().getMapPane().getChildren().remove(currentRoomView);
        currentRoomView = new RoomView(nbCol, nbLignes);
        gameController.getGameView().getRoomLabel().setText("Room " + currentRoomModel.getID());
        loadDoors();
        loadItems();
        loadPlayer();
        loadNPCs();
        loadHandlers();
        gameController.getGameView().getMapPane().getChildren().add(currentRoomView);

        //On signale à l'inventaire de mettre à jour la taille du tableau de gestionnaires d'événements utilisé
        //pour la gestion de la fonction use() des objets (quand on clique gauche sur un objet de la pièce):
        gameController.getInventoryController().resetUseItemHandlersArray(nbCol, nbLignes);

        //"Éteint" l'ordinateur si le joueur quitte la pièce sans appuyer sur le bouton 'quitter':
        gameController.getActorController().resetActorPanel();

        //À chaque nouvelle pièce chargée on vérifie si le jeu est terminé:
        gameController.isGameOver();
    }


    //====================== LOADERS ==========================
    public void loadDoors() {
        Set<Door> doors = currentRoomModel.getDoors().keySet();
        int[] roomSize = {currentRoomView.getNbCol(), currentRoomView.getNbLignes()};

        for(Door d : doors) {
            DoorView doorView;
            int[] doorPos = {d.getScalar2D().getScalar2DCol(), d.getScalar2D().getScalar2DLine()};

            if(d instanceof LockedDoor && ((LockedDoor) d).isLocked())
                doorView = new DoorView("locked");
            else if(d instanceof LockedDoor && !((LockedDoor) d).isLocked())
                doorView = new DoorView("normal");
            else
                doorView = new DoorView("normal");

            doorView.setDoorGeometry(roomSize, doorPos);
            currentRoomView.addInRoom(doorView, d.getTag(), doorPos[0], doorPos[1], doorView.getAlignment(roomSize, doorPos));

            doorView.setOnMousePressed(e -> {
                if(e.isSecondaryButtonDown())
                    d.describe();
                else{
                    gameController.getPlayerModel().go(d);
                    updateRoomView(DEFAULT_ROOMS_SIZE.getScalar2DCol(), DEFAULT_ROOMS_SIZE.getScalar2DLine());
                }
            });
        }
    }

    public void loadHandlers() {
        //On bind les sliders de la vue du jeu à la nouvelle pièce chargée:
        currentRoomView.layoutXProperty().bind(gameController.getGameView().getMapHorizontalSlider().valueProperty());
        currentRoomView.layoutYProperty().bind(gameController.getGameView().getMapVerticalSlider().valueProperty());

        //On bind les boutons de zoom à la vue de la nouvelle pièce chargée:
        gameController.getGameView().getZoomPlusButton().setOnAction(e -> {
            currentRoomView.setScaleX(currentRoomView.getScaleX() * 1.1);
            currentRoomView.setScaleY(currentRoomView.getScaleY() * 1.1);
        });
        gameController.getGameView().getZoomMinusButton().setOnAction(e -> {
            currentRoomView.setScaleX(currentRoomView.getScaleX() * 10.0/11.0);
            currentRoomView.setScaleY(currentRoomView.getScaleY() * 10.0/11.0);
        });

        //On bind les valeurs maximum des sliders du jeu pour que la nouvelle pièce chargées ne déborde pas
        //sur le panneau qui la contient (sauf si en cas de zoom mais pour ça on a setMapPaneClip() dans GameView:
        gameController.getGameView().getMapHorizontalSlider().maxProperty().bind(
                Bindings.subtract(gameController.getGameView().getMapPane().widthProperty(), currentRoomView.widthProperty()));
        gameController.getGameView().getMapVerticalSlider().maxProperty().bind(
                Bindings.subtract(gameController.getGameView().getMapPane().heightProperty(), currentRoomView.heightProperty()));
    }

    // Gestion de l'affichage des item dans la pièce
    public void loadItems() {
        Item[] items = currentRoomModel.getInventory().getItems();
        for (Item item : items) {
            if(item.isTakable())
                addItemInRoom(item, item.getScalar2D().getScalar2DCol(), item.getScalar2D().getScalar2DLine());
            else
                addContainerInRoom(item, item.getScalar2D().getScalar2DCol(), item.getScalar2D().getScalar2DLine());
        }
    }

    // Gestion des affichages des NPCs dans la pièce
    public void loadNPCs() {
        NPC[] npcs = currentRoomModel.getNPCs();

        if(npcs == null)
            return;

        for (NPC npc : npcs) {
            //On cherche une position disponible dans la pièce et on met à jour la position du modèle:
            int[] availableRoomPos = currentRoomView.getRandPos();
            Scalar2D npcPos = new Scalar2D(availableRoomPos[0], availableRoomPos[1]);

            if (!npc.isDead())
                npc.setPos(npcPos);

            ActorView actorView = gameController.getActorController().getNPCView(npc);
            actorView.setOnMousePressed(e -> {
                if(e.isSecondaryButtonDown())
                    npc.describe();
                else{
                    gameController.getActorController().updateNPCFrame(npc);
                    npc.talk();
                }

            });
            currentRoomView.addInRoom(actorView, npc.getName(), npc.getPos().getScalar2DCol(), npc.getPos().getScalar2DLine(), "CENTER");
        }
    }

    // Gestion de l'affichage du joueur dans la pièce
    public void loadPlayer() {
        int nbCol = currentRoomView.getNbCol();
        int nbLignes = currentRoomView.getNbLignes();

        gameController.getActorController().updatePlayerFrame();
        currentRoomView.addInRoom(gameController.getPlayerView(), gameController.getPlayerModel().getName(),
                (nbCol - 1)/2, (nbLignes-1)/2, "CENTER");
        gameController.getGameView().getActorImageView().setImage(new Image(getClass().getResource("../img/main_character.png").toString(), true));

        gameController.getPlayerView().setOnMousePressed(e -> {
            if(e.isSecondaryButtonDown())
                gameController.getPlayerModel().describe();
            else {
                gameController.getActorController().resetActorPanel();
                gameController.getActorController().updatePlayerFrame();
                gameController.getGameView().getActorImageView().setImage(new Image(getClass().getResource("../img/main_character.png").toString(), true));
            }
        });
    }


    //====================== UNLOADERS ========================
    public void unloadDoors() {
        Set<Door> doors = currentRoomModel.getDoors().keySet();

        for(Door d : doors)
            currentRoomView.removeFromRoom(d.getTag());
    }

    public void unloadNPCs(){
        NPC[] npcs = currentRoomModel.getNPCs();

        if(npcs == null)
            return;

        for (NPC npc : npcs)
            currentRoomView.removeFromRoom(npc.getName());
    }
}
