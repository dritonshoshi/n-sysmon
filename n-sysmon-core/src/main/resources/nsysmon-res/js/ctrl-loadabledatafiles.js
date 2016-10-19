angular.module('NSysMonApp').controller('CtrlLoadableServerDataFiles', function ($scope, $log, Rest) {
    $('title').text("NSysmon - Loadable Files");
    $scope.refresh = function() {
        sendCommand('getFiles');
    };

    //load data in load of page
    $scope.refresh();

    function sendCommand(cmd) {
        Rest.call(cmd, initFromResponse);
    }

    $scope.getJsonDownloadLink= function() {
        return Rest.getDataUrl('loadFromFile');
    };

    function initFromResponse(data) {
        $scope.files = data.files;
        //console.log(data);
    }

});
