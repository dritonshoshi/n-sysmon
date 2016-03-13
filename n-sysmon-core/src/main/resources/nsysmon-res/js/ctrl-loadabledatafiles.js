angular.module('NSysMonApp').controller('CtrlLoadableServerDataFiles', function($scope, $log, Rest, escapeHtml, $timeout) {

    $scope.refresh = function() {
        sendCommand('getFiles');
    };

    //load data in load of page
    $scope.refresh();

    function sendCommand(cmd) {
        Rest.call(cmd, initFromResponse);
    }

    function initFromResponse(data) {
        $scope.files = data.files;
        //console.log(data);
    }

});