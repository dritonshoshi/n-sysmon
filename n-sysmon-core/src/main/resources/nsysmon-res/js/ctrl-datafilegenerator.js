angular.module('NSysMonApp').controller('CtrlDataFileGenerator', function($scope, $log, Rest, escapeHtml, $timeout) {
    $('title').text("NSysmon - Datafiles");
    $scope.refresh = function() {
        sendCommand('getData');
    };
    //getData on load
    $scope.refresh();

    function sendCommand(cmd) {
        Rest.call(cmd, initFromResponse);
    }

    function initFromResponse(data) {
        $scope.pages = data.pages;
        $scope.lastExportTimestamp = data.lastExportTimestamp;
        console.log(data);
    }

});
