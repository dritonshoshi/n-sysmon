angular.module('NSysMonApp').controller('CtrlCorrelationFlow', function($scope, $log, Rest) {

    $('title').text("NSysmon - Correlations");
    $scope.refresh = function() {
        sendCommand('getData');
    };

    $scope.clear = function() {
        sendCommand('doClear');
    };

    $scope.expandAll = function() {
        $('#tree').treeview('expandAll');
    };

    $scope.collapseAll = function() {
        $('#tree').treeview('collapseAll');
    };

    //load data in load of page
    $scope.refresh();

    function sendCommand(cmd) {
        Rest.call(cmd, initFromResponse);
    }

    function initFromResponse(givenData) {
        $scope.data = givenData;
        //console.log(data);
        $('#tree').treeview({
            data: givenData.tree,
            levels: 1,
            showTags: true,
            selectable: false,
            multiSelect: false,
            collapseIcon: "glyphicon glyphicon-menu-down",
            expandIcon: "glyphicon glyphicon-menu-right",
            onNodeSelected : function(event, data) {
                if (data.state.expanded) {
                    $('#tree').treeview('collapseNode', data.nodeId);
                } else {
                    $('#tree').treeview('expandNode', data.nodeId);
                }
                $('#tree').treeview('unselectNode', data.nodeId);
            },
        });
    }
});
