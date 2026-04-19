package com.hdporner

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HDPornerProvider : MainAPI() {
    override var mainUrl = "https://hdporner.me"
    override var name = "HDPorner"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.Anime)

    private val ua = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/?filter=latest" to "Latest",
        "$mainUrl/c/brazzers/?filter=latest" to "Brazzers",
        "$mainUrl/c/realitykings/?filter=latest" to "RealityKings",
        "$mainUrl/c/pervmom/?filter=latest" to "PervMom",
        "$mainUrl/c/familystrokes/?filter=latest" to "FamilyStrokes",
        "$mainUrl/c/bangbros/?filter=latest" to "BangBros",
        "$mainUrl/c/freeusefantasy/?filter=latest" to "FreeUseFantasy",
        "$mainUrl/c/milf/?filter=latest" to "MILF",
        "$mainUrl/c/sislovesme/?filter=latest" to "SisLovesMe",
        "$mainUrl/c/teamskeet/?filter=latest" to "TeamSkeet",
        "$mainUrl/c/public-sex/?filter=latest" to "Public Sex",
        "$mainUrl/c/publicagent/?filter=latest" to "PublicAgent",
        "$mainUrl/c/mom-son-porn/?filter=latest" to "Mom Son"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            request.data
        } else {
            if (request.data.contains("/c/")) {
                request.data.replace("filter=latest", "page/$page?filter=latest")
            } else {
                request.data.replace("?filter=latest", "/page/$page?filter=latest").replace("filter=latest", "page/$page?filter=latest")
            }
        }
        
        val doc = app.get(url, headers = ua).document
        val items = doc.select("article.post").mapNotNull { el ->
            val a = el.selectFirst("a[href]") ?: return@mapNotNull null
            val href = a.attr("abs:href").ifBlank { return@mapNotNull null }
            val title = (el.selectFirst("h2, h3")?.text() ?: a.attr("title")).trim().ifBlank { return@mapNotNull null }
            val poster = el.selectFirst("img")?.let { it.attr("data-src").ifBlank { it.attr("src") } }
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
        return newHomePageResponse(request.name, items, page < 10)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}", headers = ua).document
        return doc.select("article.post").mapNotNull { el ->
            val a = el.selectFirst("a[href]") ?: return@mapNotNull null
            val href = a.attr("abs:href").ifBlank { return@mapNotNull null }
            val title = (el.selectFirst("h2, h3")?.text() ?: a.attr("title")).trim().ifBlank { return@mapNotNull null }
            val poster = el.selectFirst("img")?.let { it.attr("data-src").ifBlank { it.attr("src") } }
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = ua).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: doc.title().trim()
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val description = doc.selectFirst("meta[name=description]")?.attr("content")
        
        // Get iframe source for video
        val iframeSrc = doc.selectFirst("iframe")?.attr("src") ?: ""
        
        return newMovieLoadResponse(title, url, TvType.Movie, iframeSrc) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.isBlank() || !data.startsWith("http")) return false
        
        val quality = when {
            data.contains("1080") -> Qualities.P1080.value
            data.contains("720") -> Qualities.P720.value
            data.contains("480") -> Qualities.P480.value
            else -> Qualities.Unknown.value
        }
        
        callback(
            newExtractorLink(name, "$name [HD]", data, ExtractorLinkType.VIDEO) {
                this.quality = quality
                this.headers = ua + mapOf("Referer" to mainUrl)
            }
        )
        return true
    }
}
