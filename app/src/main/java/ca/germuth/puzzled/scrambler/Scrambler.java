package ca.germuth.puzzled.scrambler;

import ca.germuth.puzzled.puzzle.Puzzle;
import ca.germuth.puzzled.puzzle.PuzzleTurn;

public interface Scrambler {
	
	/**
	 * This method takes a given puzzle and a scramble length, and applies random moves
	 * to the puzzle from the puzzle's primary moves. It returns an Array of puzzleturn
	 * objects for each turn in the random scramble
	 * @param puz
	 * @param scramble_length
	 * @return
	 */
	public abstract PuzzleTurn[] scramble(Puzzle puz, int scramble_length);
}
