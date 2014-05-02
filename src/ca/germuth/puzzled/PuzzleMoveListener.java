package ca.germuth.puzzled;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import ca.germuth.puzzled.openGL.MyGLSurfaceView;
import ca.germuth.puzzled.puzzle.Puzzle;
import ca.germuth.puzzled.puzzle.PuzzleTurn;

public class PuzzleMoveListener implements OnClickListener{
	private Puzzle mPuzzle;
	private ArrayList<PuzzleTurn> mPuzzleTurns; 
	private MyGLSurfaceView openGLView;

	public PuzzleMoveListener(Puzzle p, MyGLSurfaceView view){
		this.mPuzzle = p;
		this.mPuzzleTurns = p.getAllMoves();
		this.openGLView = view;
	}

	@Override
	public void onClick(View v) {
		Button btn = (Button) v;
		String name = (String) btn.getText();
		try {
			PuzzleTurn match = null;
			//search through all turns to find the matching turn
			for(int i = 0; i < mPuzzleTurns.size(); i++){
				PuzzleTurn current = mPuzzleTurns.get(i);
				if( current.getmName().equals( name )){
					//found the matching turn
					match = current;
					//get all of the methods and their arguments
					Method[] turns = current.getMethods();
					//TODO perhaps generic way of doing this rather than if length == 1, 2 etc
					Object[] args = current.getmArguments();
					//execute each method with it's arguments
					for(int j = 0; j < turns.length; j++){
						Method m = turns[j];
						Object[] argsj = (Object[]) args[j];
						if(argsj == null){
							m.invoke(mPuzzle, (Object[]) null);
						}else if(argsj.length == 1){
							m.invoke(mPuzzle, argsj[0]);
						}else if(argsj.length == 2){
							m.invoke(mPuzzle, argsj[0], argsj[1]);
						}
					}
					//turn found, no need to keep searching
					break;
				}
			}
			match.setmChangedTiles(mPuzzle.getChangedTiles());

			mPuzzle.moveFinished();
			//pass changed tiles from move to renderer
			this.openGLView.addPuzzleTurn( match );

		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}