package net.perfectdreams.objloadertests

data class WavefrontMTL(val materials: Map<String, Material>) {
    data class Material(var diffuseTextureMap: String? = null)
}