angular.module('NSysMonApp').controller('CtrlMemoryUsage', function($scope, $log, Rest) {
    $('title').text("Nsysmon - Memory Usage");

    $scope.refresh = function() {
        Rest.call('getData', initFromResponse);
    };

    $scope.formatBytes = function(bytes) {
        if (bytes === null || bytes === undefined || bytes < 0) {
            return 'n/a';
        }
        if (bytes < 1024) {
            return bytes + ' B';
        }

        var units = ['kB', 'MB', 'GB', 'TB'];
        var value = bytes;
        var unit = -1;
        do {
            value = value / 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);

        return value.toFixed(1) + ' ' + units[unit];
    };

    function initFromResponse(data) {
        $scope.report = data;
    }

    $scope.refresh();
});
