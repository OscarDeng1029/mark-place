[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$base = $PSScriptRoot
$products = foreach ($name in @('apple_initial.json','trip_lookup.json','apple_more.json')) {
  $data = Get-Content -LiteralPath (Join-Path $base "sources/$name") -Raw -Encoding UTF8 | ConvertFrom-Json
  $data.results | Select-Object trackId,trackName,version,releaseDate,currentVersionReleaseDate,averageUserRating,userRatingCount,formattedPrice,trackViewUrl,sellerUrl
}
$products | Sort-Object trackId -Unique | Export-Csv -LiteralPath (Join-Path $base 'competitor_snapshot.csv') -Encoding UTF8 -NoTypeInformation
$reviews = foreach ($pair in @(@('Placify','placify_reviews.json'),@('Pi Travel','trip_reviews.json'))) {
  $data = Get-Content -LiteralPath (Join-Path $base "sources/$($pair[1])") -Raw -Encoding UTF8 | ConvertFrom-Json
  foreach ($entry in $data.feed.entry) {
    [pscustomobject]@{product=$pair[0];id=$entry.id.label;sourceDate=$entry.updated.label;rating=$entry.'im:rating'.label;title=$entry.title.label;content=$entry.content.label;url=$entry.link.attributes.href}
  }
}
$reviews | Export-Csv -LiteralPath (Join-Path $base 'user_reviews_90.csv') -Encoding UTF8 -NoTypeInformation
$samples = Get-Content -LiteralPath (Join-Path $base 'xhs_samples.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$samples | ForEach-Object { $_ | Add-Member -NotePropertyName url -NotePropertyValue "https://www.xiaohongshu.com/explore/$($_.id)" -PassThru } | Export-Csv -LiteralPath (Join-Path $base 'xiaohongshu_notes_7.csv') -Encoding UTF8 -NoTypeInformation
$manifest = foreach ($file in Get-ChildItem -LiteralPath (Join-Path $base 'sources') -File) {
  [pscustomobject]@{file=$file.Name;bytes=$file.Length;sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash}
}
$manifest | Export-Csv -LiteralPath (Join-Path $base 'source_manifest.csv') -Encoding UTF8 -NoTypeInformation
[pscustomobject]@{products=@($products | Sort-Object trackId -Unique).Count;reviews=@($reviews).Count;xiaohongshuNotes=@($samples).Count;sourceFiles=@($manifest).Count} | ConvertTo-Json
