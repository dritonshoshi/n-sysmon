angular.module('NSysMonApp').controller('CtrlMonitor', function($scope, $timeout, $log, Rest, $location) {

    $('title').text("NSysmon - Monitoring");

    $scope.autoRefreshMonitor = true;
    $scope.autoRefreshSecondsMonitor = 10;
    $scope.entriesToLoadDataFor = [];
    $scope.timedScalarsWithMonitoringData = [];
    // to invalidate auto-refresh if there was a manual refresh in between
    var autoRefreshMonitorCounter = 0;

    $scope.$watch('autoRefreshSecondsMonitor', triggerAutoRefreshMonitor);
    $scope.$watch('autoRefreshMonitor', triggerAutoRefreshMonitor);

    function initFromResponse(data) {
        $scope.timedScalars = data.timedScalars;
        $scope.refreshMonitor();
        triggerAutoRefreshMonitor();
    }

    function initialLoadData() {
        Rest.call('getData', initFromResponse);
    }

    function triggerAutoRefreshMonitor() {
        if(! $scope.autoRefreshMonitor) {
            return;
        }
        if ($location.search().loadfile) {
            return;
        }

        var oldCounter = autoRefreshMonitorCounter;
        setTimeout(function() {
            if(autoRefreshMonitorCounter !== oldCounter+1) {
                return;
            }
            $scope.refreshMonitor();
        }, $scope.autoRefreshSecondsMonitor * 1000);
        autoRefreshMonitorCounter += 1;
    }

    function initMonitorDataFromResponse(data) {
        $scope.timedScalarsWithMonitoringData = data;
        triggerAutoRefreshMonitor();
    }

    $scope.refreshMonitor = function() {
        if ($location.search().loadfile) {
            return;
        }

        var selectedEntriesForServer = "";
        if (typeof $scope.timedScalars == 'undefined'){
            return;
        }

        for (var keyData in $scope.timedScalars) {
            selectedEntriesForServer = selectedEntriesForServer.concat(keyData);
            selectedEntriesForServer = selectedEntriesForServer.concat(",");
        }
        
        if (selectedEntriesForServer.length > 1) {
            Rest.call('getMonitoringData/' + selectedEntriesForServer, initMonitorDataFromResponse);
        }

    };

    initialLoadData();

});
