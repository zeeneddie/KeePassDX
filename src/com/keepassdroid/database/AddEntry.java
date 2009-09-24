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
import org.phoneid.keepassj2me.Types;

import android.os.Handler;

import com.keepassdroid.Database;

public class AddEntry extends RunnableOnFinish {
	private Database mDb;
	private PwEntry mEntry;
	
	public AddEntry(Database db, PwEntry entry, Handler handler, OnFinish finish) {
		super(finish, handler);
		
		mDb = db;
		mEntry = entry;
		
		mFinish = new AfterAdd(mFinish, mHandler);
	}
	
	@Override
	public void run() {
		PwGroup parent = mEntry.parent;
		
		// Add entry to group
		parent.childEntries.add(mEntry);
		
		// Add entry to PwManager
		mDb.mPM.entries.add(mEntry);
		
		// Commit to disk
		SaveDB save = new SaveDB(mDb, mHandler, mFinish);
		save.run();
	}
	
	private class AfterAdd extends OnFinish {

		public AfterAdd(OnFinish finish, Handler handler) {
			super(finish, handler);
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				PwGroup parent = mEntry.parent;

				// Mark parent group dirty
				mDb.gDirty.put(parent, new WeakReference<PwGroup>(parent));

				// Add entry to global
				mDb.gEntries.put(Types.bytestoUUID(mEntry.uuid), new WeakReference<PwEntry>(mEntry));
				
				// Add entry to search index
				mDb.searchHelper.insertEntry(mEntry);
			} else {
				// Remove from group
				mEntry.parent.childEntries.removeElement(mEntry);
				
				// Remove from manager
				mDb.mPM.entries.removeElement(mEntry);

			}
			
			super.run();
		}
	}
	

}
