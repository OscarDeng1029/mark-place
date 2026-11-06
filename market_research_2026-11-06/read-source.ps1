param([string]$Path, [string]$Mode = 'html')
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$raw = Get-Content -LiteralPath $Path -Encoding UTF8 -Raw
function Plain([string]$s) {
  $s = [regex]::Replace($s, '(?is)<script\b[^>]*>.*?</script>|<style\b[^>]*>.*?</style>', '')
  $s = [regex]::Replace($s, '(?is)<[^>]+>', ' ')
  [System.Net.WebUtility]::HtmlDecode([regex]::Replace($s, '\s+', ' ')).Trim()
}
if ($Mode -eq 'search') {
  foreach ($m in [regex]::Matches($raw, '(?is)<li class="b_algo".*?</li>')) {
    $a = [regex]::Match($m.Value, '(?is)<h2[^>]*>.*?<a[^>]*href="([^"]+)"[^>]*>(.*?)</a>')
    $u = [System.Net.WebUtility]::HtmlDecode($a.Groups[1].Value)
    if ($u -match '[?&]u=a1([^&]+)') {
      try { $b = $Matches[1].Replace('-', '+').Replace('_', '/'); $b = $b.PadRight([int]([Math]::Ceiling($b.Length / 4) * 4), '='); $u = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b)) } catch {}
    }
    [pscustomobject]@{title=(Plain $a.Groups[2].Value);url=$u;text=(Plain $m.Value)} | ConvertTo-Json -Compress
  }
} elseif ($Mode -eq 'so') {
  foreach ($m in [regex]::Matches($raw, '(?is)<li class="res-list.*?</li>')) {
    $title = [regex]::Match($m.Value, '(?is)<h3[^>]*>(.*?)</h3>')
    if (!$title.Success) { continue }
    $urls = @([regex]::Matches($m.Value, 'data-mdurl="([^"]+)"') | ForEach-Object { [System.Net.WebUtility]::HtmlDecode($_.Groups[1].Value) } | Select-Object -Unique)
    [pscustomobject]@{title=(Plain $title.Groups[1].Value);urls=$urls;text=(Plain $m.Value)} | ConvertTo-Json -Compress
  }
} elseif ($Mode -eq 'xhs') {
  $m = [regex]::Match($raw, '(?s)window\.__INITIAL_STATE__=(.*?)(?:</script>)')
  if ($m.Success) {
    $json = [regex]::Replace($m.Groups[1].Value, ':undefined(?=[,}])', ':null')
    $d = $json | ConvertFrom-Json
    foreach ($p in $d.note.noteDetailMap.PSObject.Properties) {
      $n = $p.Value.note
      [pscustomobject]@{id=$n.noteId;title=$n.title;desc=$n.desc;time=$n.time;type=$n.type;nickname=$n.user.nickname;imageCount=$n.imageList.Count;interactInfo=$n.interactInfo;commentsLoaded=$p.Value.comments.list.Count} | ConvertTo-Json -Depth 6
    }
  }
} elseif ($Mode -eq 'apple') {
  ($raw | ConvertFrom-Json).results | Select-Object trackName,trackId,trackViewUrl,version,releaseDate,currentVersionReleaseDate,formattedPrice,averageUserRating,userRatingCount,sellerUrl,description,releaseNotes | ConvertTo-Json -Depth 5
} else {
  Plain $raw
}
