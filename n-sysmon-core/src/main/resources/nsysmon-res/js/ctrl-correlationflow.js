angular.module('NSysMonApp').controller('CtrlCorrelationFlow', function($scope, $log, Rest, escapeHtml, $timeout) {

    $scope.refresh = function() {
        sendCommand('getData');
    };

    //load data in load of page
    $scope.refresh();

    function sendCommand(cmd) {
        Rest.call(cmd, initFromResponse);
    }

    function initFromResponse(givenData) {
        $scope.data= givenData;
        $('#tree').treeview({
            data: givenData.tree,
            levels: 1,
            showTags: true,
            collapseIcon: "glyphicon glyphicon-menu-down",
            expandIcon: "glyphicon glyphicon-menu-right",
        });
        //console.log(data);
    }

});
