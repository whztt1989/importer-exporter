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
package org.citydb.modules.citygml.importer.database.xlink.resolver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.citydb.modules.citygml.common.database.uid.UIDCacheEntry;
import org.citydb.modules.citygml.common.database.xlink.DBXlinkTextureAssociationTarget;
import org.citydb.modules.citygml.common.database.xlink.DBXlinkTextureParam;
import org.citydb.modules.citygml.common.database.xlink.DBXlinkTextureParamEnum;
import org.citydb.util.Util;
import org.citygml4j.model.citygml.CityGMLClass;

public class XlinkTextureParam implements DBXlinkResolver {
	private final Connection batchConn;
	private final DBXlinkResolverManager resolverManager;

	private PreparedStatement psTextureParam;

	private int batchCounter;

	public XlinkTextureParam(Connection batchConn, DBXlinkResolverManager resolverManager) throws SQLException {
		this.batchConn = batchConn;
		this.resolverManager = resolverManager;

		init();
	}

	private void init() throws SQLException {
		psTextureParam = batchConn.prepareStatement(new StringBuilder()
		.append("insert into TEXTUREPARAM (SURFACE_GEOMETRY_ID, IS_TEXTURE_PARAMETRIZATION, WORLD_TO_TEXTURE, SURFACE_DATA_ID) values ")
		.append("(?, ?, ?, ?)").toString());
	}

	public boolean insert(DBXlinkTextureParam xlink) throws SQLException {
		// check whether we deal with a local gml:id
		// remote gml:ids are not supported so far...
		if (Util.isRemoteXlink(xlink.getGmlId()))
			return false;

		UIDCacheEntry geometryEntry = resolverManager.getDBId(xlink.getGmlId(), CityGMLClass.ABSTRACT_GML_GEOMETRY, true);
		if (geometryEntry == null || geometryEntry.getId() == -1)
			return false;

		psTextureParam.setLong(1, geometryEntry.getId());
		psTextureParam.setInt(2, xlink.isTextureParameterization() ? 1 : 0);
		psTextureParam.setLong(4, xlink.getId());

		// worldToTexture
		if (xlink.getWorldToTexture() != null && xlink.getWorldToTexture().length() != 0)
			psTextureParam.setString(3, xlink.getWorldToTexture());
		else
			psTextureParam.setNull(3, Types.VARCHAR);
		
		psTextureParam.addBatch();
		if (++batchCounter == resolverManager.getDatabaseAdapter().getMaxBatchSize())
			executeBatch();

		if (xlink.getType() == DBXlinkTextureParamEnum.TEXCOORDGEN && xlink.getTexParamGmlId() != null) {
			// make sure xlinks to the corresponding texture parameterization can be resolved
			resolverManager.propagateXlink(new DBXlinkTextureAssociationTarget(
					xlink.getId(),
					geometryEntry.getId(),
					xlink.getTexParamGmlId()));
		}

		return true;
	}

	@Override
	public void executeBatch() throws SQLException {
		psTextureParam.executeBatch();
		batchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psTextureParam.close();
	}

	@Override
	public DBXlinkResolverEnum getDBXlinkResolverType() {
		return DBXlinkResolverEnum.TEXTUREPARAM;
	}

}
