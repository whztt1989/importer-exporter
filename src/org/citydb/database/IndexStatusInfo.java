/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2015,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.database;

import java.util.ArrayList;
import java.util.List;

import org.citydb.api.log.LogLevel;
import org.citydb.log.Logger;

public class IndexStatusInfo {
	private final Logger LOG = Logger.getInstance();
	private List<IndexInfoObject> indexes;
	private IndexType type;

	public enum IndexType {
		SPATIAL,
		NORMAL
	}

	public enum IndexStatus {
		VALID,
		DROPPED,
		INVALID,
		ERROR
	}
	
	private IndexStatusInfo() {
		// just to thwart instantiation
	}
	
	public static synchronized IndexStatusInfo createFromDatabaseQuery(String[] query, IndexType type) {
		IndexStatusInfo info = null;

		if (query != null) {
			info = new IndexStatusInfo();
			info.indexes = new ArrayList<IndexInfoObject>(query.length);
			info.type = type;
			
			for (String indexInfo : query) {
				String[] parts = indexInfo.split(":");

				if (parts.length > 3) {
					IndexInfoObject obj = new IndexInfoObject();				

					if (parts[0].equals("VALID"))
						obj.status = IndexStatus.VALID;
					else if (parts[0].equals("DROPPED"))
						obj.status = IndexStatus.DROPPED;
					else if (parts[0].equals("INVALID"))
						obj.status = IndexStatus.INVALID;
					else
						obj.status = IndexStatus.ERROR;

					obj.name = parts[1].toUpperCase();
					// parts[2] is the schema name
					obj.table = parts[3].toUpperCase();
					obj.column = parts[4].toUpperCase();

					if (parts.length > 5 && !parts[5].equals("0"))
						obj.errorMessage = parts[5];
					else
						obj.errorMessage = "";
					
					info.indexes.add(obj);
				}
			}
		}

		return info;
	}

	public List<IndexInfoObject> getIndexObjects() {
		return indexes;
	}
	
	public List<IndexInfoObject> getIndexObjects(IndexStatus status) {
		List<IndexInfoObject> tmp = new ArrayList<IndexInfoObject>();		
		for (IndexInfoObject obj : indexes)
			if (obj.status == status)
				tmp.add(obj);
		
		return tmp;
	}
	
	public int getNumberOfIndexes() {
		return indexes.size();
	}
	
	public int getNumberOfValidIndexes() {
		return getIndexObjects(IndexStatus.VALID).size();
	}
	
	public int getNumberOfInvalidIndexes() {
		return getIndexObjects(IndexStatus.INVALID).size();
	}
	
	public int getNumberOfDroppedIndexes() {
		return getIndexObjects(IndexStatus.DROPPED).size();
	}
	
	public int getNumberOfFaultyIndexes() {
		return getIndexObjects(IndexStatus.ERROR).size();
	}
	
	public void printStatusToConsole() {
		int on = getNumberOfValidIndexes();
		int all = getIndexObjects().size();
		
		StringBuilder msg = new StringBuilder();
		
		msg.append(type == IndexType.SPATIAL ? "Spatial" : "Normal").append(" indexes are ");
		
		if (on == 0)
			msg.append("disabled.");
		else if (on == all)
			msg.append("enabled.");
		else
			msg.append("partly enabled (").append(on).append(" / ").append(all).append(").");
		
		LOG.all(LogLevel.INFO, msg.toString());
	}
	
	public static final class IndexInfoObject {
		private String name;
		private String table;
		private String column;
		private IndexStatus status;
		private String errorMessage;

		public String getName() {
			return name;
		}

		public String getTable() {
			return table;
		}
		
		public String getColumn() {
			return column;
		}

		public IndexStatus getStatus() {
			return status;
		}

		public String getErrorMessage() {
			return errorMessage;
		}
		
		public boolean hasErrorMessage() {
			return errorMessage != null && errorMessage.length() > 0;
		}
		
		public String toString() {
			return name + " on " + table + "(" + column + ")";
		}
	}

}
