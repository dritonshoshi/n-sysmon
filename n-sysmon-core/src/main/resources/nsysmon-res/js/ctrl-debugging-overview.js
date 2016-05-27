angular.module('NSysMonApp').controller('CtrlOverviewDebugging', function($scope, $log, Rest, escapeHtml, $timeout) {

    $('title').text("NSysmon - Debugging Configuration");
    $scope.currentCollectSqlParameters = false;
    $scope.currentCollectTooltips = false;

    $scope.refresh = function() {
        Rest.call('getData', displayData);
    };

    $scope.startOverrideCollectTooltips = function() {
        Rest.call('startOverrideCollectTooltips', function(){$scope.refresh();});
    };

    $scope.stopOverrideCollectTooltips = function() {
        Rest.call('stopOverrideCollectTooltips', function(){$scope.refresh();});
    };

    $scope.startOverrideSqlParameters = function() {
        Rest.call('startOverrideSqlParameters', function(){$scope.refresh();});
    };

    $scope.stopOverrideSqlParameters = function() {
        Rest.call('stopOverrideSqlParameters', function(){$scope.refresh();});
    };

    $scope.refresh();

    function configureButtons(data) {
        //$scope.currentCollectTooltips = false;
        //$scope.currentCollectSqlParameters = false;
        data.configurationParameters.forEach(function(entry) {
            if (entry.key == "current collectSqlParameters"){
                $scope.currentCollectSqlParameters = entry.value;
                //console.log("current collectSqlParameters");
                //console.log($scope.currentCollectSqlParameters);
            } else if (entry.key == "current collectTooltips"){
                $scope.currentCollectTooltips = entry.value;
                //console.log("current collectTooltips");
                //console.log($scope.currentCollectTooltips);
            }
        }, this);
    }

    function renderDataAsHtml(data) {
        var htmlText = '';
        htmlText += renderConfigurationParametersAsHtml(data);
        htmlText += renderPageDefinitionsAsHtml(data);

        return htmlText;
    }

    function renderConfigurationParametersAsHtml(data) {
        var htmlText = '';
        htmlText += '<fieldset class="scalar-group">';
        htmlText += '<legend>Configuration</legend>';

        htmlText += '<table class="table table-condensed table-striped">';
        htmlText += '<tr><th class="scalar-name">Parameter</th><th class="scalar-value">Value</th></tr>';

        data.configurationParameters.sort(
            function (a, b) {
                return a.key.localeCompare(b.key);
            }
        ).forEach(function(entry) {
            htmlText += '<tr>';
            htmlText += '<td class="scalar-name">' + entry.key + '</td>';
            htmlText += '<td class="scalar-value">' + entry.value + '</td>';
            htmlText += '</tr>\n';
        }, this);

        htmlText += '</table>';
        htmlText += '</fieldset>';
        return htmlText;
    }

    function renderPageDefinitionsAsHtml(data) {
        var htmlText = '';
        htmlText += '<fieldset class="scalar-group">';
        htmlText += '<legend>Active Pages</legend>';

        htmlText += '<table class="table table-condensed table-striped">';
        htmlText += '<tr><th class="scalar-name">ID</th><th class="scalar-value">Value</th></tr>';

        data.pageDefinitions.sort(
            function (a, b) {
                return a.id.localeCompare(b.id);
            }
        ).forEach(function(entry) {
            htmlText += '<tr>';
            htmlText += '<td class="scalar-name">' + entry.id + '</td>';
            htmlText += '<td class="scalar-value">' + entry.fullLabel + '</td>';
            htmlText += '</tr>\n';
        }, this);

        htmlText += '</table>';
        htmlText += '</fieldset>';
        return htmlText;
    }

    function displayData(data) {
        configureButtons(data);

        var htmlToInsert = renderDataAsHtml(data);

        var myNode = document.getElementById("theData");
        while (myNode && myNode.firstChild) {
            myNode.removeChild(myNode.firstChild);
        }

        $('#theData').html(htmlToInsert);

    }

});
