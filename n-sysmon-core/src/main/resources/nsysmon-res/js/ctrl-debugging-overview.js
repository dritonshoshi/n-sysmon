angular.module('NSysMonApp').controller('CtrlOverviewDebugging', function($scope, $log, Rest, escapeHtml, $timeout) {

    $scope.refresh = function() {
        Rest.call('getData', displayData);
    };

    $scope.refresh();

    function renderDataAsHtml(data) {
        var htmlText = '';

        data.configurationParameters.forEach(function(entry) {
            htmlText += "<div>";
            htmlText += entry.key;
            htmlText += ":";
            htmlText += entry.value;
            htmlText += "</div>";
          }, this);

        return htmlText;
    }

    function displayData(data) {
        var htmlToInsert = renderDataAsHtml(data);

        var myNode = document.getElementById("theData");
        while (myNode && myNode.firstChild) {
            myNode.removeChild(myNode.firstChild);
        }

        $('#theData').html(htmlToInsert);

    }

});
