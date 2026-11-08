$files = Get-ChildItem D:\DoodleStudio\app\build\test-results\testDebugUnitTest\TEST-*.xml
$totalTests = 0
$totalFailures = 0
foreach ($f in $files) {
    [xml]$x = Get-Content $f.FullName
    $tests = [int]$x.testsuite.tests
    $failures = [int]$x.testsuite.failures
    $totalTests += $tests
    $totalFailures += $failures
    Write-Host ("{0,-60} : {1,2} tests, {2} failures" -f $x.testsuite.name, $tests, $failures)
}
Write-Host ("-" * 75)
Write-Host ("TOTAL: {0} tests, {1} failures" -f $totalTests, $totalFailures)

