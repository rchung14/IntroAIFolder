package src.pas.battleship.agents;

import java.util.Map;

// SYSTEM IMPORTS

// JAVA PROJECT IMPORTS
import java.util.Map;
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;
import edu.bu.battleship.game.EnemyBoard;
import edu.bu.battleship.game.Game;
import edu.bu.battleship.game.ships.Ship;
import edu.bu.battleship.game.ships.Ship.ShipType;

public class ProbabilisticAgent extends Agent {

    public ProbabilisticAgent(String name) {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
    }

    @Override
    public Coordinate makeMove(final GameView game) {
        int columns = game.getGameConstants().getNumCols();
        int rows = game.getGameConstants().getNumRows();

        int[][] coord_counter = new int[rows][columns];
        Map<ShipType, Integer> ships = game.getEnemyShipTypeToNumRemaining();
        
        // finding total combinations
        int boardsize = columns * rows; 
        int totalCombinations = 1;

        for (Ship.ShipType ship : ships.keySet()) {
            int numRemaining = ships.get(ship);
            
            int shipSize = ship.size(); 
            int requiredCells = shipSize * numRemaining;
            
            // Calculate the number of cells remaining on the board
            int remainingCells = boardsize - requiredCells;
            
            // Calculate the number of combinations for this ship type
            int combinations = remainingCells + 1;
            
            // Update the total combinations
            totalCombinations *= combinations;
        }

        for (Ship.ShipType ship : ships.keySet()) {
            if (ships.get(ship) == 0) {
                continue;
            }

            EnemyBoard.Outcome[][] board = game.getEnemyBoardView();

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    if (board[row][col] == Outcome.UNKNOWN || board[row][col] == Outcome.HIT) {
                        int hori_combo = 0;
                        int vert_combo = 0;
                        final int LARGEST_BOAT_SIZE = 5;

                        // check horizontal combinations
                        for (int i = col; i < columns; i++) {
                            if (hori_combo == LARGEST_BOAT_SIZE) {
                                break;
                            }

                            // if inbound
                            if (board[row][i] == Outcome.UNKNOWN || board[row][i] == Outcome.HIT) {
                                hori_combo++;
                            } else {
                                break;
                            }
                        }

                        // check vertical combinations
                        for (int i = row; i < rows; i++) {
                            if (vert_combo == LARGEST_BOAT_SIZE) {
                                break;
                            }

                            // if inbound
                            if (board[i][col] == Outcome.UNKNOWN || board[i][col] == Outcome.HIT) {
                                vert_combo++;
                            } else {
                                break;
                            }
                        }

                        // VALID COMBINATION CODE 
                        // patrol
                        int numPatrol = ships.get(Ship.ShipType.PATROL_BOAT);
                        if (hori_combo == 2) {
                            for (int i = row; i <= col + 1 && i < rows; i++) {
                                coord_counter[i][col] += numPatrol;
                            }
                        }
                        if (vert_combo >= 2) {
                            for (int i = col; i <= col + 1 && i < col; i++) {
                                coord_counter[row][i] += numPatrol;
                            }
                        }

                        // aircraft
                        int numAircraft = ships.get(Ship.ShipType.AIRCRAFT_CARRIER);
                        if (hori_combo == 5) {
                            for (int i = row; i <= col + 1 && i < rows; i++) {
                                coord_counter[i][col] += numAircraft;
                            }
                        }
                        if (vert_combo >= 5) {
                            for (int i = col; i <= col + 1 && i < col; i++) {
                                coord_counter[row][i] += numAircraft;
                            }
                        }

                        // battleship
                        int numBship = ships.get(Ship.ShipType.BATTLESHIP);
                        if (hori_combo == 4) {
                            for (int i = row; i <= col + 1 && i < rows; i++) {
                                coord_counter[i][col] += numBship;
                            }
                        }
                        if (vert_combo >= 4) {
                            for (int i = col; i <= col + 1 && i < col; i++) {
                                coord_counter[row][i] += numBship;
                            }
                        }

                        // destroyer
                        int numDestroyer = ships.get(Ship.ShipType.DESTROYER);
                        if (hori_combo == 3) {
                            for (int i = row; i <= col + 1 && i < rows; i++) {
                                coord_counter[i][col] += numDestroyer;
                            }
                        }
                        if (vert_combo >= 3) {
                            for (int i = col; i <= col + 1 && i < col; i++) {
                                coord_counter[row][i] += numDestroyer;
                            }
                        }

                        // submarine
                        int numSub = ships.get(Ship.ShipType.SUBMARINE);
                        if (hori_combo == 3) {
                            for (int i = row; i <= col + 1 && i < rows; i++) {
                                coord_counter[i][col] += numSub;
                            }
                        }
                        if (vert_combo >= 3) {
                            for (int i = col; i <= col + 1 && i < col; i++) {
                                coord_counter[row][i] += numSub;
                            }
                        }
                    }
                }
            }
        }

        int max_count = 0;
        Coordinate max_coord = new Coordinate(0, 0);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (coord_counter[row][col] > max_count) {
                    max_count = coord_counter[row][col];
                    max_coord = new Coordinate(row, col);
                }
            }
        }

        System.out.println("ATTACKING: " + max_coord);
        return max_coord;
    }

    @Override
    public void afterGameEnds(final GameView game) {
    }
}
