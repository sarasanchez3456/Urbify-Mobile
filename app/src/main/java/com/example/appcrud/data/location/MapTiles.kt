package com.example.appcrud.data.location

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource

/**
 * Fuente de tiles del mapa.
 *
 * No usamos `TileSourceFactory.MAPNIK` (tile.openstreetmap.org) porque su
 * política de uso bloquea con 403 a apps sin User-Agent aprobado. Los basemaps
 * de CARTO son un CDN gratuito, sin API key, tolerante para uso ligero, y con
 * un estilo limpio que combina con Urbify.
 *
 * Atribución obligatoria (mostrar en la UI cerca del mapa):
 *   "© OpenStreetMap contributors © CARTO"
 */
object MapTiles {

    val CARTO_VOYAGER: ITileSource = XYTileSource(
        "CartoVoyager",
        0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
        ),
        "© OpenStreetMap contributors © CARTO",
    )

    const val ATRIBUCION = "© OpenStreetMap · © CARTO"
}
