package com.mylittleatlas.app.domain

import kotlin.math.*

data class GeoPoint(val latitude: Double, val longitude: Double)
data class WorldPoint(val x: Double, val y: Double)

/** Same GCJ-02 transformation as the reference; stored locations stay WGS-84. */
object MapGeometry {
    fun gcj(wgs: GeoPoint): GeoPoint {
        val lng = wgs.longitude; val lat = wgs.latitude
        if(lng < 72.004 || lng > 137.8347 || lat < .8293 || lat > 55.8271) return wgs
        val x = lng-105; val y = lat-35
        var a = -100+2*x+3*y+.2*y*y+.1*x*y+.2*sqrt(abs(x))
        a += (20*sin(6*x*PI)+20*sin(2*x*PI))*2/3+(20*sin(y*PI)+40*sin(y/3*PI))*2/3+(160*sin(y/12*PI)+320*sin(y*PI/30))*2/3
        var b = 300+x+2*y+.1*x*x+.1*x*y+.1*sqrt(abs(x))
        b += (20*sin(6*x*PI)+20*sin(2*x*PI))*2/3+(20*sin(x*PI)+40*sin(x/3*PI))*2/3+(150*sin(x/12*PI)+300*sin(x/30*PI))*2/3
        val rad = lat/180*PI; val magic = 1-.00669342162296594323*sin(rad).pow(2); val root = sqrt(magic)
        return GeoPoint(lat+a*180/(6378245*(1-.00669342162296594323)/(magic*root)*PI), lng+b*180/(6378245/root*cos(rad)*PI))
    }
    fun position(place: Place): GeoPoint? {
        val lat = place.latitude ?: return null; val lng = place.longitude ?: return null
        if(!lat.isFinite() || !lng.isFinite() || lat !in -85.051129..85.051129 || lng !in -180.0..180.0) return null
        val a = place.gcjLatitude; val b = place.gcjLongitude
        return if(a != null && b != null && a.isFinite() && b.isFinite() && a in -85.051129..85.051129 && b in -180.0..180.0) GeoPoint(a,b)
            else gcj(GeoPoint(lat,lng))
    }
    fun project(point: GeoPoint, zoom: Int): WorldPoint {
        val n = 2.0.pow(zoom); val rad = point.latitude.coerceIn(-85.05112878,85.05112878)*PI/180
        return WorldPoint((point.longitude+180)/360*n, (1-ln(tan(rad)+1/cos(rad))/PI)/2*n)
    }
    fun unproject(point: WorldPoint, zoom: Int): GeoPoint {
        val n = 2.0.pow(zoom)
        return GeoPoint(atan(sinh(PI*(1-2*point.y/n)))*180/PI, point.x/n*360-180)
    }
    fun fit(points: List<GeoPoint>, width: Double, height: Double): Pair<GeoPoint,Int> {
        if(points.isEmpty()) return GeoPoint(31.2304,121.4737) to 12
        val world = points.map { project(it,0) }
        val center = WorldPoint((world.minOf { it.x }+world.maxOf { it.x })/2,(world.minOf { it.y }+world.maxOf { it.y })/2)
        val dx = (world.maxOf { it.x }-world.minOf { it.x }).coerceAtLeast(.000001)
        val dy = (world.maxOf { it.y }-world.minOf { it.y }).coerceAtLeast(.000001)
        val zoom = floor(log2(min(width.coerceAtLeast(120.0)/256/dx,height.coerceAtLeast(120.0)/256/dy))).toInt().coerceIn(3,16)
        return unproject(center,0) to zoom
    }
}
