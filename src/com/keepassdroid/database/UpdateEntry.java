/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database;

import java.lang.ref.WeakReference;

import org.phoneid.keepassj2me.PwEntry;
import org.phoneid.keepassj2me.PwGroup;

import android.os.Handler;

import com.keepassdroid.Database;

public class UpdateEntry extends RunnableOnFinish {
	private Database mDb;
	private PwEntry mOldE;
	private PwEntry mNewE;
	
	public UpdateEntry(Database db, PwEntry oldE, PwEntry newE, Handler handler, OnFinish finish) {
		super(finish, handler);
		
		mDb = db;
		mOldE = oldE;
		mNewE = newE;
		
		// Keep backup of original values in case save fails
		PwEntry backup = new PwEntry(mOldE);
		mFinish = new AfterUpdate(backup, finish, handler);
	}

	@Override
	public void run() {
		// Update entry with new values
		mOldE.assign(mNewE);
		
		// Commit to disk
		SaveDB save = new SaveDB(mDb, mHandler, mFinish);
		save.run();
	}
	
	private class AfterUpdate extends OnFinish {
		private PwEntry mBackup;
		
		public AfterUpdate(PwEntry backup, OnFinish finish, Handler handler) {
			super(finish, handler);
			
			mBackup = backup;
		}
		
		@Override
		public void run() {
			if ( mSuccess ) {
				// Mark group dirty if title changes
				if ( ! mBackup.title.equals(mNewE.title) ) {
					PwGroup parent = mBackup.parent;
					if ( parent != null ) {
						// Mark parent group dirty
						mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));
					}

					// Update search index
					mDb.searchHelper.updateEntry(mOldE);
				}
			} else {
				// If we fail to save, back out changes to global structure
				mOldE.assign(mBackup);
			}
			
			super.run();
		}
		
	}


}
