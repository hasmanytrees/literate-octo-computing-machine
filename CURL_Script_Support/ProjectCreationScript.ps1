### Update number of projects required for each file (if required) and update CI account access
$SourceFile1 = $args[0]
$SourceFile2 = $args[1]
$numberOfProjects = $args[2]
$CompassionAccount = "<CI USER>@us.ci.org:<CI PASSWORD>"
###

$counter = 0
While ($counter -ne $numberOfProjects) {
	$counter += 1

Write-Output "`r`nCreating Projects... Iteration $counter"

& "curl.exe" "-k" "-X" "POST" "-ntlm" "-u" "$CompassionAccount" "https://hdc0136.ci.org/ws/compassion-api/v1/create_project" "-d" "@$SourceFile1"

& "curl.exe" "-k" "-X" "POST" "-ntlm" "-u" "$CompassionAccount" "https://hdc0136.ci.org/ws/compassion-api/v1/create_project" "-d" "@$SourceFile2"

}
	
