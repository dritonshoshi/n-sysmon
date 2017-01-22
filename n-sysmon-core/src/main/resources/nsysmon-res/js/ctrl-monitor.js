angular.module('NSysMonApp').controller('CtrlMonitor', function ($scope, $timeout, $log, Rest, $location) {

    $('title').text("NSysmon - Measurement Monitor");

    $scope.activePage = "";
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
        if (!$scope.autoRefreshMonitor) {
            return;
        }
        var oldCounter = autoRefreshMonitorCounter;
        $scope.activePage = $location.path();
        $timeout(function () {
            if (autoRefreshMonitorCounter !== oldCounter + 1) {
                return;
            }
            if ($location.path() == $scope.activePage) {
                $scope.refreshMonitor();
            }
        }, $scope.autoRefreshSecondsMonitor * 1000);
        autoRefreshMonitorCounter += 1;
    }

    function initMonitorDataFromResponse(data) {
        $scope.timedScalarsWithMonitoringData = data;
        triggerAutoRefreshMonitor();
    }

    $scope.refreshMonitor = function () {
        var selectedEntriesForServer = "";
        if (typeof $scope.timedScalars == 'undefined') {
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

    $scope.asDate = function (ts) {
        var date = new Date(ts);
        return date.toLocaleTimeString() + " " + date.toLocaleDateString();
        // var hours = date.getHours();
        // var minutes = "0" + date.getMinutes();
        // var seconds = "0" + date.getSeconds();
        // var formattedTime = hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);
        // return formattedTime;
    }

});
