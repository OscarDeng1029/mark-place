package com.mylittleatlas.app.data

import com.mylittleatlas.app.domain.AtlasData

/** Only used by explicit debug test launches, never as a new user's library. */
object PreviewAtlas {
    fun create(): AtlasData {
        val demo=DemoAtlas.create()
        return demo.copy(places=demo.places.map {
            when(it.id) {
                "green" -> it.copy(city="杭州")
                "tart" -> it.copy(city="南京")
                else -> it
            }
        })
    }
}
