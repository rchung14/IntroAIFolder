package src.pas.tetris.agents;


import java.util.ArrayList;
import java.util.Arrays;
// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Random;


// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Block;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.game.minos.Mino.MinoType;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU;  // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Pair;


public class TetrisQAgent
    extends QAgent
{

    public static final double EXPLORATION_PROB = 0.05;

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction() {
        final int numPixels = Board.NUM_ROWS * Board.NUM_COLS;
        final int numAdditionalFeatures = 11;
        final int numFeatures = numPixels + numAdditionalFeatures;  
        final int firstHiddenDim = numFeatures / 2;  
        final int secondHiddenDim = firstHiddenDim / 2; 
        final int outputDim = 1;  

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(numFeatures, firstHiddenDim));
        qFunction.add(new ReLU());
        qFunction.add(new Dense(firstHiddenDim, secondHiddenDim));
        qFunction.add(new ReLU());
        qFunction.add(new Dense(secondHiddenDim, outputDim)); 

        return qFunction;
    }


    /**
        This function is for you to figure out what your features
        are. This should end up being a single row-vector, and the
        dimensions should be what your qfunction is expecting.
        One thing we can do is get the grayscale image
        where squares in the image are 0.0 if unoccupied, 0.5 if
        there is a "background" square (i.e. that square is occupied
        but it is not the current piece being placed), and 1.0 for
        any squares that the current piece is being considered for.
        
        We can then flatten this image to get a row-vector, but we
        can do more than this! Try to be creative: how can you measure the
        "state" of the game without relying on the pixels? If you were given
        a tetris game midway through play, what properties would you look for?
     */

    @Override
    public Matrix getQFunctionInput(final GameView game, final Mino potentialAction) {
        try { 
            // Flatten grayscale image to single row vector
            Matrix grayscaleImage = game.getGrayscaleImage(potentialAction).flatten();
            int numPixels = Board.NUM_COLS * Board.NUM_ROWS;

            // Additional features
            int numAdditionalFeatures = 11;
            Matrix inputVector = Matrix.zeros(1, numPixels + numAdditionalFeatures);

            // Set grayscale pixels
            for (int i = 0; i < numPixels; i++) {
                inputVector.set(0, i, grayscaleImage.get(0, i));
            }

            // Extract and set additional features
            int[] features = {
                game.getScoreThisTurn(),
                getNextMino(game),
                game.getTotalScore(),
                getNumberOfHoles(game.getBoard()),
                getBumpiness(game.getBoard()),
                getMaxHeight(game.getBoard()),
                getMinHeight(game.getBoard()),
                getTotalHeight(game.getBoard()),
                // getTransitions(game.getBoard()),
                // calculateWellSums(game.getBoard()),
                // countBlockades(game.getBoard())
                0,
                0,
                0 // placeholders
            };

            for (int i = 0; i < numAdditionalFeatures; i++) {
                inputVector.set(0, numPixels + i, features[i]);
            }

            return inputVector;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null; 
        }
    }

    // helper functions for features
    private int getNextMino(GameView game) { 
        Mino.MinoType nextMino = null;
        List<Mino.MinoType> nextMinos = game.getNextThreeMinoTypes(); 

        if (!nextMinos.isEmpty()) {
            nextMino = nextMinos.get(0);
        }
        return nextMino.ordinal();
    }

    private int getNumberOfHoles(Board board) {
        int numberOfHoles = 0;

        for (int col = 0; col < Board.NUM_COLS; col++) {
            boolean foundBlock = false;
            for (int row = 0; row < Board.NUM_ROWS; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    foundBlock = true;
                } else if (foundBlock) {
                    numberOfHoles++;
                }
            }
        }
        return numberOfHoles;
    }

    private int getBumpiness(Board board) {
        int lastHeight = 0;
        int bumpiness = 0;
        boolean isFirstColumn = true;
    
        for (int col = 0; col < Board.NUM_COLS; col++) {
            int currentHeight = 0;
            for (int row = 0; row < Board.NUM_ROWS; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    currentHeight = Board.NUM_ROWS - row;
                    break;
                }
            }
            if (!isFirstColumn) {
                bumpiness += Math.abs(currentHeight - lastHeight);
            } else {
                isFirstColumn = false;
            }
            lastHeight = currentHeight;
        }
        return bumpiness;
    }    

    private int getMaxHeight(Board board) {
        int maxHeight = 0;

        for (int col = 0; col < Board.NUM_COLS; col++) {
            for (int row = 0; row < Board.NUM_ROWS; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    // column height from first occupied cell found in column
                    int colHeight = Board.NUM_ROWS - row;
                    maxHeight = Math.max(maxHeight, colHeight);
                    break;
                }
            }
        }
        return maxHeight;
    }
    
    private int getMinHeight(Board board) {
        // start w max height
        int minHeight = Board.NUM_ROWS;

        for (int col = 0; col < Board.NUM_COLS; col++) {
            for (int row = 0; row < Board.NUM_ROWS; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    // column height from first occupied cell found in column
                    int colHeight = Board.NUM_ROWS - row;
                    minHeight = Math.min(minHeight, colHeight);
                    break;
                }
            }
        }
        return minHeight;
    }
    
    private int getTotalHeight(Board board) {
        // cumulative height of all columns by adding up heights of first occupied 
        // cell found in each column from top down
        int totalHeight = 0;

        for (int col = 0; col < Board.NUM_COLS; col++) {
            for (int row = 0; row < Board.NUM_ROWS; row++) {
                if (board.isCoordinateOccupied(col, row)) {
                    totalHeight += (Board.NUM_ROWS - row);
                    break;
                }
            }
        }
        return totalHeight;
    }

    // private int getTransitions(Board board) {
    //     int transitions = 0;
    //     for (int row = 0; row < Board.NUM_ROWS; row++) {
    //         for (int col = 0; col < Board.NUM_COLS - 1; col++) {
    //             if (board.isCoordinateOccupied(col, row) != board.isCoordinateOccupied(col + 1, row)) {
    //                 transitions++;
    //             }
    //         }
    //     }
    //     for (int col = 0; col < Board.NUM_COLS; col++) {
    //         for (int row = 0; row < Board.NUM_ROWS - 1; row++) {
    //             if (board.isCoordinateOccupied(col, row) != board.isCoordinateOccupied(col, row + 1)) {
    //                 transitions++;
    //             }
    //         }
    //     }
    //     return transitions;
    // }

    // private int calculateWellSums(Board board) {
    //     int wellSum = 0;
    //     for (int col = 0; col < Board.NUM_COLS; col++) {
    //         for (int row = Board.NUM_ROWS - 1; row >= 0; row--) {
    //             if (!board.isCoordinateOccupied(col, row)) {
    //                 int depth = 1;
    //                 while (--row >= 0 && !board.isCoordinateOccupied(col, row)) {
    //                     depth++;
    //                 }
    //                 wellSum += depth;
    //             }
    //         }
    //     }
    //     return wellSum;
    // }

    // private int countBlockades(Board board) {
    //     int blockades = 0;
    //     for (int col = 0; col < Board.NUM_COLS; col++) {
    //         boolean holeFound = false;
    //         for (int row = Board.NUM_ROWS - 1; row >= 0; row--) {
    //             if (board.isCoordinateOccupied(col, row) && holeFound) {
    //                 blockades++;
    //             } else if (!board.isCoordinateOccupied(col, row)) {
    //                 holeFound = true;
    //             }
    //         }
    //     }
    //     return blockades;
    // }    
    
    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good" actions
     * over and over again. This can prevent us from discovering new, potentially even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of the time.
     * While this strategy is easy to implement, it often doesn't perform well and is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
    
    @Override
    public boolean shouldExplore(final GameView game,
                                final GameCounter gameCounter)
    {
        return this.getRandom().nextDouble() <= EXPLORATION_PROB;
    }

    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we decide
    * that we should ignore our policy, we now have to actually choose an action.
    *
    * You should come up with a way of choosing an action so that the model gets
    * to experience something new. The current implemention just chooses a random
    * option, which in practice doesn't work as well as a more guided strategy.
    * I would recommend devising your own strategy here.
    */
    @Override
    public Mino getExplorationMove(final GameView game)
    {
        int randIdx = this.getRandom().nextInt(game.getFinalMinoPositions().size());
        return game.getFinalMinoPositions().get(randIdx);
    }


    /**
     * This method is called by the TrainerAgent after we have played enough training games.
     * In between the training section and the evaluation section of a phase, we need to use
     * the exprience we've collected (from the training games) to improve the q-function.
     *
     * You don't really need to change this method unless you want to. All that happens
     * is that we will use the experiences currently stored in the replay buffer to update
     * our model. Updates (i.e. gradient descent updates) will be applied per minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient descent manner
     * (i.e. all at once)...this often works better and is an active area of research.
     *
     * Each pass through the data is called an epoch, and we will perform "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
                               LossFunction lossFunction,
                               Optimizer optimizer,
                               long numUpdates)
    {
        for(int epochIdx = 0; epochIdx < numUpdates; ++epochIdx)
        {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix> > batchIterator = dataset.iterator();

            while(batchIterator.hasNext())
            {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try
                {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                                                  lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the larger
     * the number, the more "pleasurable" it is to the model, and the smaller the number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced by the
     * points, however this is not all. In fact, just using the points earned this turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally the worse
     * (unless you have a long hole waiting for an I-block). When you design a reward
     * signal that is less sparse, you should see your model optimize this reward over time.
     */
    @Override
    public double getReward(final GameView game) {
        double score = game.getScoreThisTurn();
        Board board = game.getBoard();

        // Calculate penalties and bonuses
        double penaltyForHeight = 2000;
        // if difference from max height to longest stack is > 0
        if (Board.NUM_ROWS - getMaxHeight(board) > 0) {
            penaltyForHeight = (1 / (Board.NUM_ROWS-getMaxHeight(board))) * 1000; 
        }

        double penaltyForHoles = getNumberOfHoles(board) * 2;  
        double penaltyForBumpiness = getBumpiness(board) * 0.5; 
        // double penaltyForBlockades = countBlockades(board) * 1.5; 

        // Adjust reward calculation
        double reward = score - penaltyForHeight - penaltyForHoles - penaltyForBumpiness;

        return reward;
    }
}
