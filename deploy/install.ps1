if (!$args[0]) {
	Write-Output "You must specify WS directory"
	Return
}

$webXMLPath = ($args[0] + "\WEB-INF\web.xml")
$webXMLOriginalPath = ($args[0] + "\WEB-INF\web.xml.original")
# $jsPath = ($args[0] + "\compassion_inc")
$jarPath = ($args[0] + "\WEB-INF\lib")

If (-Not (Test-Path $webXMLPath)) {
	Write-Output ("Path does not exist: " + $webXMLPath)
	Return
}

If (-Not (Test-Path $webXMLOriginalPath)) {
	Copy-Item $webXMLPath $webXMLOriginalPath | Out-Null
}

$webXML = [xml](Get-Content $webXMLOriginalPath)
$apiXML = [xml](Get-Content ".\webxml\deploy_create_external.xml") 

$servletName = $apiXML.SelectNodes("//servlet/servlet-name/text()").Item(0)."Value".Trim()
$count = $webXML.SelectNodes("//*[contains(text(),'" + $servletName + "')]").Count

# JS/HTML are auto-deployed via the application. They must be uploaded to (a single) AIS instance along with configuration files.
# Write-Output 'Copying JS/HTML includes ...'
# New-Item -ItemType directory -Path $jsPath -Force | Out-Null
# Copy-Item ".\include\*" $jsPath -recurse -Force | Out-Null

Write-Output 'Updating JAR ...'
Copy-Item (".\jars\*") $jarPath | Out-Null

If($count -eq 0) {
	Write-Output 'Updating web.xml ...'
	FOREACH ($child in $apiXML.get_DocumentElement().ChildNodes){
		$toImport = $webXML.ImportNode($child, $true) 
		$webXML.DocumentElement.AppendChild($toImport) | Out-Null
	}
	$webXML.Save($webXMLPath) | Out-Null
} Else {
	Write-Output 'web.xml already up-to-date ...'
}