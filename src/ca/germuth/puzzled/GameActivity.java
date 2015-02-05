package ca.germuth.puzzled;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import ca.germuth.puzzled.ReplayParser.ReplayMove;
import ca.germuth.puzzled.ShakeListener.OnShakeListener;
import ca.germuth.puzzled.database.PuzzledDatabase;
import ca.germuth.puzzled.database.SolveDB;
import ca.germuth.puzzled.gui.GameActivityLayout;
import ca.germuth.puzzled.openGL.MyGLSurfaceView;
import ca.germuth.puzzled.puzzle.Puzzle;
import ca.germuth.puzzled.puzzle.Puzzle.OnPuzzleSolvedListener;
import ca.germuth.puzzled.puzzle.PuzzleTurn;
import ca.germuth.puzzled.puzzle.cube.Cube;
import ca.germuth.puzzled.util.Chronometer;
import ca.germuth.puzzled.util.Chronometer.OnChronometerFinishListener;
import ca.germuth.puzzled.util.FontManager;

/**
 * TODO Ask for fonts You stole a font: AGENT_ORANGE Use this font for
 * non-commercial use only! If you plan to use it for commercial purposes,
 * contact me before doing so!
 * 
 * Have fun and enjoy!
 * 
 * Jakob Fischer jakob@pizzadude.dk www.pizzadude.dk
 */
/*
add message when solve deleted, and move to previous solve?
add 3 more graphs
make stats linkable
actually do user panel
change leaderboard look
make background of play screen black
make buttons look like buttons?
*/
public class GameActivity extends PuzzledActivity {
	private static final int SCRAMBLE_LENGTH = 25;
	private static final int INSPECTION_LENGTH = 15;

	private Puzzle mPuzzle;
	private SolveDB mSolve;
	private PuzzleMoveListener mPuzzleMoveListener;
	private MyGLSurfaceView mGlView;
	private Chronometer mTimer;
	private String mScramble;
	private PuzzleState mState;

	//this activity can be used both to play the game, or to watch a replay
	private GameActivityType mActivityType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(Cube.getLayout());

		mScramble = "";
		mState = PuzzleState.Playing;

		//grab arguments
		mPuzzle = ((PuzzledApplication) this.getApplication()).getPuzzle();
		if (mPuzzle == null) {
			Intent myIntent = new Intent(this, PuzzleSelectActivity.class);
			this.startActivity(myIntent);
		}
		//will be null if mActivityState = PLAY
		//otherwise, is the solve we are replaying
		mSolve = this.getIntent().getParcelableExtra("solve");
		mActivityType = this.getIntent().getParcelableExtra("activity_type");

		
		// grab and set up timer
		mTimer = (Chronometer) this.findViewById(R.id.activity_game_timer);
		mTimer.setTypeface(FontManager.getTypeface(this,
				FontManager.AGENT_ORANGE_FONT));
		// fires when inspection time runs out
		mTimer.setOnChronometerFinishListener(new OnChronometerFinishListener() {
			@Override
			public void onChronometerFinish(Chronometer chronometer) {
				disableButtons(false, true, true);
				mTimer.start();
				mState = PuzzleState.Solving;
			}
		});

		// grab openGL view
		mGlView = (MyGLSurfaceView) this
				.findViewById(R.id.activity_game_gl_surface_view);
		mGlView.initializeRenderer(mPuzzle);
		mPuzzleMoveListener = new PuzzleMoveListener(this, mPuzzle, mGlView);

		setOnPuzzleSolved();

		GameActivityLayout container = (GameActivityLayout) this
				.findViewById(R.id.activity_game_container);
		//removes touches
		if( mActivityType == GameActivityType.REPLAY){
			container.setOnTouchListener(null);			
		}
		container.setmActivity(this);
		container.setmGlView(mGlView);

		// add PuzzeMoveListener to each button
		addButtonListeners();

		if( mActivityType == GameActivityType.PLAY){
			// add listener to scramble cube and start inspection time
			addShakeListener();			
		}else if(mActivityType == GameActivityType.REPLAY){
			//disable touch
			this.disableButtons(true, true, true);
			//start executing moves
			//TODO make slide moves work
			new Thread(new Runnable(){
				@Override
				public void run(){
					try {
						//wait 1.5 seconds before starting
						Thread.sleep(1500);
						
						ArrayList<PuzzleTurn> turns = mPuzzleMoveListener
								.getmPuzzleTurns();
						
						StringTokenizer s = new StringTokenizer(mSolve.getScramble());
						while(s.hasMoreTokens()){
							String move = s.nextToken();	
							//search puzzleTurns for move with same name
							for(int k = 0; k < turns.size(); k++){
								PuzzleTurn match = turns.get(k);
								if( match.getmName().equals(move)){
									//found match execute turn 
									final PuzzleTurn selected = match;
									GameActivity.this.runOnUiThread(new Runnable() {
										public void run() {
											mPuzzleMoveListener.execute(selected);
										}
									});
									//so scramble isn't ridiculously fast
									Thread.sleep(300);
									break;
								}
							}
						}
						
						//scrambling done
						//now to inspection and solve
						ReplayParser rp = new ReplayParser(mSolve.getReplay());
						
						//create arraylist of the puzzleturn to execute, and how long to wait inbetween moves
						ArrayList<PuzzleTurn> moves = new ArrayList<PuzzleTurn>();
						ArrayList<Integer> waitTimes = new ArrayList<Integer>();
						
						Iterator<ReplayMove>i = rp.iterator();
						//time starts at -15 seconds for inspection
						//TODO make inspection time not hardcoded
						int previousTime = -15000;
						
						while(i.hasNext()){
							ReplayMove curr = i.next();
							
							//search puzzleTurns for move with same name
							for(int k = 0; k < turns.size(); k++){
								PuzzleTurn match = turns.get(k);
								if( match.getmName().equals(curr.getMove())){
									//found match add to array
									moves.add(match);
									int difference = previousTime - curr.getTime();
									//if negative then reverse
									if(difference < 0){
										difference *= -1;
									}
									waitTimes.add(difference);
									previousTime = curr.getTime();
									break;
								}
							}
						}
						//replay is now prepared for execution
						GameActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								mTimer.startCountingDown(INSPECTION_LENGTH * 1000);
							}
						});
						for(int a = 0; a < moves.size(); a++){
							final PuzzleTurn curr = moves.get(a);
							//execute move
							GameActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									mPuzzleMoveListener.execute(curr);
								}
							});
							//sleep time inbetween moves
							Thread.sleep(waitTimes.get(a));
						}
						
						GameActivity.this.runOnUiThread(new Runnable() {
							public void run() {
								mTimer.stop();
							}
						});
						
					}catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	private void setOnPuzzleSolved(){
		if( mActivityType == GameActivityType.PLAY){
			// TODO receive puzzle somehow
			mPuzzle.setOnPuzzleSolvedListener(new OnPuzzleSolvedListener() {
				@Override
				public void onPuzzleSolved() {
					if (mState == PuzzleState.Solving) {
						mState = PuzzleState.Playing;
						mTimer.stop();

						// async task to insert solve in database
						// when done switch windows
						new AsyncTask<Void, Void, Void>() {
							@Override
							protected Void doInBackground(Void... params) {
								PuzzledDatabase db = new PuzzledDatabase(
										GameActivity.this);
								// int duration, String replay, PuzzleDB puz, long
								// dateTime){
								Date d = new Date();
								SolveDB ss = new SolveDB((int) mTimer
										.getTimeElapsed(), mPuzzleMoveListener.getReplay(), mScramble, db
										.convert(new Cube(3)), d.getTime());
								db.insertSolve(ss);
								return null;
							}

							@Override
							protected void onPostExecute(Void result) {
								super.onPostExecute(result);
								Intent mainIntent = new Intent(GameActivity.this,
										StatisticActivity.class);
								GameActivity.this.startActivity(mainIntent);
								GameActivity.this.finish();
							}
						}.execute((Void[]) null);
					}
				}
			});
		}else if( mActivityType == GameActivityType.REPLAY){
		}
	}
	
	public int getCurrentTime(){
		return (int)this.mTimer.getTimeElapsed();
	}

	private void addButtonListeners() {
		ViewGroup container = (ViewGroup) this
				.findViewById(R.id.activity_game_container);
		for (int i = 0; i < container.getChildCount(); i++) {
			View v = container.getChildAt(i);
			if (v instanceof LinearLayout) {
				LinearLayout l = (LinearLayout) v;
				for (int j = 0; j < l.getChildCount(); j++) {
					View v2 = l.getChildAt(j);
					if (v2 instanceof Button) {
						final Button btn = (Button) v2;
						btn.setOnClickListener(mPuzzleMoveListener);
					}
				}
			}
		}
	}

	/**
	 * Invokes SCRAMBLE_LENGTH random non-rotation moves to the cube. Is
	 * performed on another thread so it doesnt't freeze the gui
	 * 
	 * TODO: Also starts the timer (maybe should be removed from this method
	 */
	private void scramblePuzzle() {
		mState = PuzzleState.Scrambling;
		
		new Thread(new Runnable() {
			public void run() {				
				mScramble = "";
				ArrayList<PuzzleTurn> turns = mPuzzleMoveListener
						.getmPuzzleTurns();
				Random r = new Random();
				// TODO grab from preferences here
				for (int i = 0; i < SCRAMBLE_LENGTH; i++) {
					PuzzleTurn current = turns.get(r.nextInt(turns.size()));
					while (current.isRotation()) {
						current = turns.get(r.nextInt(turns.size()));
					}
					final PuzzleTurn selected = current;
					GameActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							mPuzzleMoveListener.execute(selected);
						}
					});
				
					mScramble += selected.getmName() + " ";
					// TODO only necessary because of concurrecy bug
					//maybe i wanna keep it because it looks better
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				GameActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// inspection length is in seconds, mTimer accepts
						// miliseconds
						mTimer.startCountingDown(INSPECTION_LENGTH * 1000);
						mState = PuzzleState.Inspection;
						disableButtons(false, false, true);
					}
				});

			}
		}).start();
	}

	public void changeButtonLevel(boolean advance) {
		// TODO should only increment if the cube needs it
		// TODO this might be SiGN specific
		for (Button btn : getAllButtons()) {
			String name = btn.getText().toString();
			if (advance) {
				if (Character.isLowerCase(name.charAt(0))) {
					btn.setText("3" + name);
				} else {
					btn.setText("2" + name);
				}
			} else {
				btn.setText(name.substring(1));
			}
		}
	}

	/**
	 * Disables all non-rotation buttons for the inspection period
	 */
	private void disableButtons(boolean disable, boolean normalMoves, boolean rotations) {
		ArrayList<PuzzleTurn> turns = mPuzzleMoveListener.getmPuzzleTurns();
		// iterate through puzzleTurns
		for (int i = 0; i < turns.size(); i++) {
			PuzzleTurn current = turns.get(i);

			if( !current.isRotation() && !normalMoves){
				continue;
			}
			
			if (current.isRotation() && !rotations) {
				continue;
			}

			// iterate through buttons
			for (Button btn : getAllButtons()) {
				if (btn.getText().toString().equals(current.getmName())) {
					btn.setEnabled(!disable);
					break;
				}
			}
		}
	}

	// TODO: change so this method is only ever called once and stored rather
	// than
	// recomputing it over and over
	private ArrayList<Button> getAllButtons() {
		ArrayList<Button> buttons = new ArrayList<Button>();
		ViewGroup container = (ViewGroup) this
				.findViewById(R.id.activity_game_container);
		// iterate through buttons
		for (int j = 0; j < container.getChildCount(); j++) {
			View v = container.getChildAt(j);
			if (v instanceof LinearLayout) {
				LinearLayout l = (LinearLayout) v;
				for (int k = 0; k < l.getChildCount(); k++) {
					View v2 = l.getChildAt(k);
					if (v2 instanceof Button) {
						final Button btn = (Button) v2;

						buttons.add(btn);
					}
				}
			}
		}
		return buttons;
	}

	private void addShakeListener() {
		ShakeListener mShaker = new ShakeListener(this);
		mShaker.setOnShakeListener(new OnShakeListener() {
			@Override
			public void onShake() {

				disableButtons(true, true, true);

				scramblePuzzle();
			}
		});
	}

	public Puzzle getPuzzle() {
		return mPuzzle;
	}

	public String getScramble() {
		return mScramble;
	}

	public PuzzleState getState() {
		return mState;
	}
}
