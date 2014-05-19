package ca.germuth.puzzled.database;

import android.os.Parcel;
import android.os.Parcelable;

public class SolveDB extends ObjectDB implements Parcelable{
	
	private int mId;
	private int mDuration;
	private String mReplay;
	private PuzzleDB mPuzzle;
	private long mDateTime;
	
	public SolveDB(int duration, String replay, PuzzleDB puz, long dateTime){
		mDuration = duration;
		mReplay = replay;
		mPuzzle = puz;
		mDateTime = dateTime;
	}
	
	public SolveDB(int id, int duration, String replay, PuzzleDB puz, long dateTime){
		mId = id;
		mDuration = duration;
		mReplay = replay;
		mPuzzle = puz;
		mDateTime = dateTime;
	}

	public int getmId() {
		return mId;
	}

	public void setmId(int mId) {
		this.mId = mId;
	}

	public int getmDuration() {
		return mDuration;
	}

	public void setmDuration(int mDuration) {
		this.mDuration = mDuration;
	}

	public String getmReplay() {
		return mReplay;
	}

	public void setmReplay(String mReplay) {
		this.mReplay = mReplay;
	}

	public PuzzleDB getmPuzzle() {
		return mPuzzle;
	}

	public void setmPuzzle(PuzzleDB mPuzzle) {
		this.mPuzzle = mPuzzle;
	}

	public long getmDateTime() {
		return mDateTime;
	}

	public void setmDateTime(long mDateTime) {
		this.mDateTime = mDateTime;
	}	
	
	@Override
	public boolean equals(Object o) {
		return mId == ((SolveDB)o).mId;
	}

	public SolveDB(Parcel in){
		this.mId = in.readInt();
		this.mDuration = in.readInt();
		this.mReplay = in.readString();
		this.mDateTime = in.readLong();
		this.mPuzzle = in.readParcelable(PuzzleDB.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeInt(mId);
		dest.writeInt(mDuration);
		dest.writeString(mReplay);
		dest.writeLong(mDateTime);
		dest.writeParcelable(this.mPuzzle, 0);
	}
	
}