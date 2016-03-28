angular.module('NSysMonApp').controller('CtrlAggregated', function($scope, $log, Rest, escapeHtml, $timeout, $location) {

    $('.button-segment').affix({
        offset: {
            top: 95
        }
    });

    function reinitTooltips() {
        $('.btn').tooltip({
            container: 'body',
            html: true
        });
    }

    reinitTooltips();
    $scope.$watch('isStarted', function() {
        $('.btn').tooltip('hide');
        setTimeout(reinitTooltips, 0);
    });


    $scope.expansionModel = {};
    $scope.rootLevel = 0;
    $scope.hideTitleRows = 1;
    $scope.showDataTooltips = 0;

    $scope.hideSearchNonMatchingNodes = true;
    $scope.expandSearchMatchingNodes = true;

    /* Delayed Search - Start */
    var timeoutPromise;
    var searchDelayInMs = 500;
    $scope.$watch('nodeSearchText', function (val) {
        if ($scope.nodeSearchText){
            $timeout.cancel(timeoutPromise);
        }
        var tempFilterText = val;
        timeoutPromise = $timeout(function () {
            $scope.nodeSearchText = tempFilterText;
            //collapse all nodes when search is active
            $scope.expansionModel = [];
            renderTree();
        }, searchDelayInMs); // delay
    });
    /* Delayed Search - End */

    var nodesByFqn = {};

    $scope.$watch('hideTitleRows', renderTree);
    $scope.$watch('showDataTooltips', renderTree);
    $scope.$watch('hideSearchNonMatchingNodes', renderTree);

    function initFromResponse(data) {
//        $log.log('init from response');
        $scope.isStarted = data.isStarted;
        $scope.columnDefs = data.columnDefs.reverse();
        $scope.traces = data.traces;
        $scope.pickedTraces = $scope.traces; //TODO keep selection on 'refresh'

        nodesByFqn = {};
        initTraceNodes($scope.traces, 0, '');

//        $log.log('after init trace nodes');

        $scope.totalDataWidth = 0;
        for(var i=0; i<data.columnDefs.length; i++) {
            var cw = data.columnDefs[i].width.toLowerCase();
            if(cw === 'short') {
                $scope.totalDataWidth += 40;
            }
            if(cw === 'medium') {
                $scope.totalDataWidth += 60;
            }
            if(cw === 'long') {
                $scope.totalDataWidth += 100;
            }
        }

        renderTree();
        // unblock GUI after process
        blockGui(false);
    }

    // TODO : Analyse this why it computes so long. Firefox causes an Script error.
    function initTraceNodes(nodes, level, prefix) {
        if(nodes) {
            for(var i=0; i<nodes.length; i++) {
                nodes[i].level = level;
                var fqn = prefix + '\n' + (nodes[i].id || nodes[i].name);
                nodes[i].fqn = fqn;
                nodesByFqn[fqn] = nodes[i];
                initTraceNodes(nodes[i].children, level+1, fqn);
            }
        }
    }

    function sendCommand(cmd) {
        // block gui for long calls
        blockGui(true);
        Rest.call(cmd, initFromResponse);
    }

    $scope.refresh = function() {
        sendCommand('getData');
    };
    $scope.clear = function() {
        $scope.expansionModel = {};
        sendCommand('doClear');
    };
    $scope.start = function() {
        sendCommand('doStart');
    };
    $scope.stop = function() {
        sendCommand('doStop');
    };

    $scope.pickClass = function() {
        return $scope.isInPickMode ? 'btn-danger' : 'btn-default';
    };

    //disabled getData on load due to performance problems with huge data
    // $scope.refresh();

    // check if data from other sources should be loaded
    function loadExternalData() {
        var loadFileParam = $location.search().loadfile;
        if (loadFileParam) {
            // block gui for long calls
            blockGui(true);
            Rest.callOther("loadableServerDataFiles", "loadFromFile" + "/" + loadFileParam, initFromResponse);
        }
    }
    loadExternalData();

    function revIdx(idx) {
        return $scope.columnDefs.length - idx - 1;
    }
    $scope.revIdx = function(idx) {
        return $scope.columnDefs.length - idx - 1;
    };

    var thousandsSeparator = 1234.5.toLocaleString().charAt(1);
    var decimalSeparator   = 1234.5.toLocaleString().charAt(5);

    $scope.formatNumber = function(number, numFracDigits) {
        var parts = number.toFixed(numFracDigits).toString().split('.');
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, thousandsSeparator);
        return parts.join(decimalSeparator);
    };

    $scope.expandLongest = function() {

        function setExpandedMainLevel(nodes) {
            // expand all top level-entries
            if(nodes) {
                for(var i=0; i<nodes.length; i++) {
                    $scope.expansionModel[nodes[i].fqn] = true;
                    setExpandedLongest(nodes[i].children);
                }
            }
        }

        function setExpandedLongest(nodes) {
            //for all sub-entries only expand the longest call
            if(nodes) {
                var longestNode;
                for(var i=0; i<nodes.length; i++) {
                    if (!longestNode || (longestNode.data[1] < nodes[i].data[1] && !nodes[i].isNotSerial)) {
                        longestNode = nodes[i];
                    }
                }
                $scope.expansionModel[longestNode.fqn] = true;
                setExpandedLongest(longestNode.children);
            }
        }

        setExpandedMainLevel($scope.traces);
        renderTree();
    };

    $scope.expandAll = function() {
        function setExpanded(nodes) {
            if(nodes) {
                for(var i=0; i<nodes.length; i++) {
                    $scope.expansionModel[nodes[i].fqn] = true;
                    setExpanded(nodes[i].children);
                }
            }
        }

        setExpanded($scope.traces);
        renderTree();
    };

    $scope.collapseAll = function() {
        $scope.expansionModel = {};
        renderTree();
    };


    $scope.isPercentage = function(columnDef) {
        return columnDef.isPercentage;
    };
    $scope.progressWidthStyle = function(value) {
        return 'background-size: ' + (value + 2) + '% 100%';
    };
    $scope.colClass = function(idx) {
        return 'column-' + $scope.columnDefs[idx].width.toLowerCase();
    };

    $scope.nodeIconClass = function(node) {
        if(! node.fqn) {
            //TODO remove this heuristic
            node = nodesByFqn[node];
        }

        if(node.children && node.children.length) {
            return $scope.isExpanded(node) ? 'node-icon-expanded' : 'node-icon-collapsed';
        }
        return 'node-icon-empty';
    };
    $scope.nodeIconClassWasKilled = function(node) {
        //$log.log("xx="+node.wasKilled);
        if(node && node.wasKilled) {
            return 'node-icon-wasKilled';
        }
        return 'no-icon node-icon-empty';
    };
    $scope.expansionStyle = function(node) {
        return $scope.isExpanded(node) ? 'block' : 'none';
    };
    $scope.isExpanded = function(node) {
        return $scope.expansionModel[node.fqn];
    };
    $scope.clickTreeNode = function(event, node) {
        if($scope.isInPickMode) {
            pickTreeNode(node);
        }
        else {
            var clicked = $(event.target);
            var dataRow = clicked.parents('.data-row');
            toggleTreeNode(dataRow, node);
        }
    };

    function toggleTreeNode(dataRow, node) {
        var childrenDiv = dataRow.next();

        if(childrenDiv.hasClass('unrendered')) {
            childrenDiv.replaceWith(htmlForChildrenDiv(node, true));
            childrenDiv = dataRow.next();
            childrenDiv.find('.data-row.with-children').click(onClickNode);
        }

        childrenDiv.slideToggle(50, function() {
            $scope.$apply(function() {
                $scope.expansionModel[node.fqn] = !$scope.expansionModel[node.fqn];

                var nodeIconDiv = dataRow.children('.node-icon');
                if($scope.isExpanded(node)) {
                    nodeIconDiv.removeClass('node-icon-collapsed').addClass('node-icon-expanded');
                }
                else {
                    nodeIconDiv.addClass('node-icon-collapsed').removeClass('node-icon-expanded');
                }
            });
        });
    }

    $scope.$watch('traces === pickedTraces', function() {
        $('#unpick').attr('disabled', $scope.traces === $scope.pickedTraces);
    });
    function pickTreeNode(node) {
        $scope.$apply(function() {
            $scope.pickedTraces = [node];
            $scope.isInPickMode = false;
            $scope.rootLevel = node.level;
        });
        renderTree();
    }
    $scope.togglePickMode = function() {
        $scope.isInPickMode = ! $scope.isInPickMode;
    };
    $scope.unpick = function() {
        $scope.pickedTraces = $scope.traces;
        $scope.rootLevel = 0;
        renderTree();
    };


    
    $scope.getJsonDownloadLink= function() {
        return Rest.getDataUrl('getData');
    };

    $scope.getJsonFilename = function() {
        function pad2(n) {
            var result = n.toString();
            while(result.length < 2) {
                result = '0' + result;
            }
            return result;
        }
        var now = new Date();
        var formattedNow = now.getFullYear() + '-' + pad2((now.getMonth()+1)) + '-' + pad2(now.getDate()) + '-' + pad2(now.getHours()) + '-' + pad2(now.getMinutes()) + '-' + pad2(now.getSeconds());
        return "nsysmon-export-" + formattedNow + '.json';
    };
    
    $scope.doImportJSON = function() {
        $scope.uploadFile();
    };

    $scope.uploadFile = function(){
        var file = document.getElementById('file').files[0],
            reader = new FileReader();
            // block gui while uploading file
            blockGui(true);

            reader.onloadend = function(e){
            $scope.$apply(function() {
                initFromResponse(angular.fromJson(e.target.result));
            });

        };
        reader.readAsBinaryString(file);
    };

    function blockGui(boolean){
        var element = document.getElementById("blocker");
        if (boolean) {
            $scope.originClassName =  element.className;
            element.className+=" blocker";
        }else{
            element.classList.remove("blocker");
        }
    }

    $scope.doExportAsExcel = function() {
        function pad2(n) {
            var result = n.toString();
            while(result.length < 2) {
                result = '0' + result;
            }
            return result;
        }
        var now = new Date();
        var formattedNow = now.getFullYear() + '-' + pad2((now.getMonth()+1)) + '-' + pad2(now.getDate()) + '-' + pad2(now.getHours()) + '-' + pad2(now.getMinutes()) + '-' + pad2(now.getSeconds());

        var data = 'Name\tLevel';
        for(var i=0; i<$scope.columnDefs.length; i++) {
            data += '\t' + $scope.columnDefs[revIdx(i)].name;
        }

        function append(node) {
            var row = '\n';
            row += '                                                                                                     '.substring(0, 2*(node.level - $scope.rootLevel));
            row += node.name + '\t' + node.level;
            for(var i=0; i < $scope.columnDefs.length; i++) {
                row += '\t' + $scope.formatNumber(node.data[i], $scope.columnDefs[i].numFracDigits);
            }
            data += row;
            for(var j=0; j<(node.children || []).length; j++) {
                append(node.children[j]);
            }
        }
        for(var j=0; j<$scope.pickedTraces.length; j++) {
            append($scope.pickedTraces[j]);
        }

        var blob = new Blob([data], {type: "application/excel;charset=utf-8"});
        saveAs(blob, "nsysmon-export-" + formattedNow + '.xls');
    };


    function renderTree() {
        var hhttmmll = htmlForAllTrees();

        // it is an important performance optimization to explicitly unregister event listeners and remove old child
        //  elements from the DOM instead of implicitly removing them in the call to $(...).html(...) - the difference
        //  is seconds vs. minutes for large trees!
        $('.data-row.with-children').off();
        var myNode = document.getElementById("theTree");
        while (myNode && myNode.firstChild) {
            myNode.removeChild(myNode.firstChild);
        }

        $('#theTree').html(hhttmmll);

        $('.data-row.with-children').click(onClickNode);
        
        if ($scope.showDataTooltips == 1){
            $("[data-toggle=data-tooltip]").tooltip();
        }
    }

    function onClickNode() {
        var fqn = $(this).children('.fqn-holder').text();
        if($scope.isInPickMode) {
            pickTreeNode(nodesByFqn[fqn]);
        }
        else {
            toggleTreeNode($(this), nodesByFqn[fqn]);
        }
    }

    function htmlForAllTrees() {
        var htmlForTableHeader = (function(){
            var titles = '';

            angular.forEach($scope.columnDefs, function(curCol, colIdx) {
                titles += '<div class="' + $scope.colClass(colIdx) + '">' + curCol.name + '</div>';
            });

            return '' +
                '<div class="table-header">&nbsp;<div style="float:right;">' +
                titles +
                '</div></div>';
        }());

        var result = '';

        if ($scope.hideTitleRows) {
            result += '<div>' + htmlForTableHeader + '</div>';
            angular.forEach($scope.pickedTraces, function(rootNode) {
                if (shouldRenderNode(rootNode, $scope.nodeSearchText)){
                    result +=
                        '<div>' +
                        htmlForTreeNode(rootNode) +
                        '</div>';
                }
            });
        }
        else {
            angular.forEach($scope.pickedTraces, function(rootNode) {
                if (shouldRenderNode(rootNode, $scope.nodeSearchText)) {
                    result +=
                        '<div>' +
                        htmlForTableHeader +
                        htmlForTreeNode(rootNode) +
                        '</div>';
                }
            });
        }

        return result;
    }

    $scope.isValidSearchActive = function() {
        return isValidSearchActive($scope.nodeSearchText);
    };

    $scope.clearSearch = function() {
        $scope.nodeSearchText = undefined;
    };

    $scope.isSearchTooShort = function() {
        return ($scope.nodeSearchText && $scope.nodeSearchText.length < 4);

    };

    function isValidSearchActive(stringToSearchFor) {
        return !(!stringToSearchFor || stringToSearchFor.length < 4);
    }

    function nodeOrChildrenMatchSearch(curNode, stringToSearchFor) {
        var display = false;
        var search = stringToSearchFor.toLowerCase();
        if (curNode.name.toLowerCase().indexOf(search) > -1) {
            display = true;
            return true;
        }
        angular.forEach(curNode.children, function(child) {
            if (child.name.toLowerCase().indexOf(search) > -1) {
                display = true;
                return true;
            }
            display |= shouldRenderNode(child, stringToSearchFor);
            if (display){
                return true;
            }
        });
        return display;
    }

    /*
        Checks if this node should be displayed, used e.g. for the search-filter
     */
    function shouldRenderNode(curNode, stringToSearchFor) {
        if (!isValidSearchActive(stringToSearchFor)){
            return true;
        }

        return nodeOrChildrenMatchSearch(curNode, stringToSearchFor);
    }

    function htmlForTreeNode(curNode) {
        if (!shouldRenderNode(curNode, $scope.nodeSearchText) && $scope.hideSearchNonMatchingNodes){
            return '';
        }
        if (isValidSearchActive($scope.nodeSearchText) && $scope.expandSearchMatchingNodes){
            $scope.expansionModel[curNode.fqn] = true;
        }

        var dataRowSubdued = !curNode.isNotSerial ? '' : 'data-row-subdued';

        var dataCols = '';
        angular.forEach($scope.columnDefs, function(curCol, colIdx) {
            dataCols += '<div class="' + $scope.colClass(colIdx) + '">';

            var formattedValue = $scope.formatNumber(curNode.data[revIdx(colIdx)], $scope.columnDefs[colIdx].numFracDigits);

            if(curCol.isPercentage) {
                if(!curNode.isNotSerial)
                    dataCols += '<div class="aprogress-background"><div class="aprogress-bar" style="' + $scope.progressWidthStyle(curNode.data[revIdx(colIdx)]) + '">' + formattedValue + '</div></div>';
                else
                    dataCols += '<div class="subdued-progress-background">' + formattedValue + '</div>';
            }
            else {
                dataCols += formattedValue;
            }

            dataCols += '</div>';
        });

        var withChildrenClass = (curNode.children && curNode.children.length) ? ' with-children' : '';
        var result =
            '<div class="data-row data-row-' + (curNode.level - $scope.rootLevel) + withChildrenClass + ' ' + dataRowSubdued + '">' +
                '<div class="fqn-holder">' + escapeHtml(curNode.fqn) + '</div>' +
                '<div class="node-icon ' + $scope.nodeIconClass(curNode.fqn) + '">&nbsp;</div>' +
                '<div class="' + $scope.nodeIconClassWasKilled(curNode) + '">&nbsp;</div>' +
                dataCols +
                renderDisplayNameForNode(curNode) +
                '</div>';
        //$log.log($scope.pickedTraces); // The Tooltips has to be sent from the server. See ABottomUpPageDefinition and ATracePageDefinition
        //$log.log("xx="+curNode.tooltip);
        //$log.log("xx="+curNode.wasKilled);
        result += htmlForChildrenDiv(curNode);

        return result;
    }

    function renderDisplayNameForNode(curNode){
        var rc = '';
        if (!curNode || !curNode.name || !curNode.tooltip || $scope.showDataTooltips != 1) {
            rc += '<div class="node-text" style="margin-right: ' + $scope.totalDataWidth + 'px;">' + renderSql(curNode) + '</div>';
        } else {
            rc += '<div data-toggle="data-tooltip" class="node-text" style="margin-right: ' + $scope.totalDataWidth + 'px;" ' + renderTooltipp(curNode) + '>' + escapeHtml(curNode.name) + '</div>';
        }
        return rc;
    }

    function renderSql(curNode) {
        if (!curNode || !curNode.name || !curNode.tooltip){
            return escapeHtml(curNode.name);
        }
        var rc = curNode.name;
        curNode.tooltip.forEach(function(entry) {
            rc = rc.replace('?', entry.value);
        }, this);
        return rc;
    }

    function renderTooltipp(curNode) {
        if (!curNode || !curNode.tooltip || $scope.showDataTooltips != 1){
            return '';
        }
        var rc = '';
        curNode.tooltip.forEach(function(entry) {
            rc += '(';
            rc += entry.id;
            rc += '=';
            rc += entry.value;
            rc += ')';
            rc += ' ';
        }, this);
        return 'title="' + rc + '"';
    }

    function htmlForChildrenDiv(curNode, shouldRender) {
        if(! curNode.children || curNode.children.length === 0) {
            return '';
        }

        if(shouldRender || $scope.isExpanded(curNode)) {
            var result = '';
            result += '<div class="children" style="display: ' + $scope.expansionStyle(curNode) + ';">';
            angular.forEach(curNode.children, function(child) {
                result += htmlForTreeNode(child, $scope.rootLevel);
            });
            result += '</div>';
            return result;
        }
        else {
            return '<div class="children unrendered"></div>';
        }
    }
});
