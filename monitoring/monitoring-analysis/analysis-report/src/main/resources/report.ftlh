<html>
<head>
    <meta charset="utf-8"/>
    <link href="http://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css"/>
    <link href="http://cdn.datatables.net/1.10.18/css/jquery.dataTables.min.css" rel="stylesheet" type="text/css"/>
    <script src="https://code.jquery.com/jquery-1.12.4.min.js" integrity="sha256-ZosEbRLbNQzLpnKIkEdrPv7lOy9C27hHQ+Xp8a4MxAQ=" crossorigin="anonymous"></script>
    <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>
    <script src="http://cdn.datatables.net/1.10.18/js/jquery.dataTables.min.js" type="text/javascript"></script>
    <script>
        $(document).ready( function () {

            var url = new URL(window.location.href);
            var isFlat = url.searchParams.get("flat") ;

            if(isFlat === null || isFlat === 'false') {
                $('.data').DataTable();
                $('#content').tabs();
                $('.tabs').tabs();
                $('#actions').append(
                        $('<button>').text('Show flat report').click(function(){
                            window.location.href = window.location.protocol + "/" + window.location.host + "/" + window.location.pathname + '?flat';
                        })
                )
            } else {
                $('#actions').append(
                        $('<button>').text('Show compact report').click(function(){
                            window.location.href = window.location.protocol + "/" + window.location.host + "/" + window.location.pathname;
                        })
                );

            }
        });
    </script>
    <style>
    body {
        font-family: 'Roboto', sans-serif;
        font-size: 13px;
        color: #606163;
        margin: 0 30px 0 30px;
    }

    p {
        font-size: 16px;
    }

    thead {
        display: table-header-group;
    }

    .content {
        page-break-after: always;
    }

    .section {
        page-break-after: always;
    }

    .section ul {
        font-size: 16px;
    }

    .section table {
        border-collapse: collapse;
        font-size: 13px;
    }

    .section th, td {
        padding: 10px;
        text-align: left;
    }

    .section th {
        border-bottom: 2px solid #606163;
    }

    .section td {
        border-bottom: 1px solid #606163;
    }

    h1 {
        font-size: 44px;
    }

    h2 {
        font-size: 32px;
    }

    h3 {
        font-size: 24px;
    }

    h4 {
        font-size: 18px;
    }

    h5 {
        font-size: 15px;
    }

    p {
        font-size: 13px;
    }

    #header {
        text-align: center;
        width: 100%;
        page-break-after: always;
    }

    #header img {
        margin-top: 30px;
    }

    #header table {
        margin: 100px auto 0;
        max-width: 400px;
        border: 1px solid #606163;
        padding: 10px;
    }

    #header th {
        text-align: right;
        padding: 5px 10px;
    }

    #header td {
        text-align: left;
        padding: 5px 10px;
    }

    #content {
        margin-bottom:200px;
    }

    #content tr.unusedAll {
        color: red;
    }

    span.important {
        color: red;
    }

    #content tr.unused {
        color: orange;
    }

    #actions {
        margin-top:20px;
    }

    .overall {
        font-size: 15px !important;
    }

    .overall th, .overall td {
        vertical-align: top;
        border-bottom: 2px solid !important;
    }

    .overall ol {
        padding-left: 15px;
    }

    .tab-header {
        padding: 13px 0;
        margin-top:20px;
        margin-bottom: 40px;
        border-bottom: 1px solid lightgray;
    }
    .tab-header li.ui-state-active  a {
        border-bottom: 3px solid red;
        color: red;
    }

    .tab-header li{
        display:inline;
    }

    .tab-header li a {
        outline: none;
        padding: 10px 22px;
        border-bottom: 3px solid white;
        font-size: 18px;
        text-decoration: none;
        color: #606163;
    }

    .tab-header.small li a {
        padding: 5px 12px !important;
        font-size: 14px !important;
    }

    .tab-header.small{
        padding:2px;
    }

    .tab-header.small li {
        line-height: 2 !important;
    }
    </style>
</head>
<body>
<div id="header">
    <#if imageUrl?has_content>
        <img src="${imageUrl}"/>
    </#if>
    <h1>Vind - Report</h1>
    <h2>${prettyToday}</h2>
    <table>
        <!--<tr>
            <th>Name:</th>
            <td>${applicationName}</td>
        </tr>
        -->
        <tr>
            <th>AppId:</th>
            <td>${applicationName}</td>
        </tr>
        <#list configuration.reportWriterConfiguration.generalFilters as name, value>
        <tr>
            <th>${name}</th>
            <td>${value}</td>
        </tr>
        </#list>
        <tr>
            <th>from:</th>
            <td>${prettyFrom}</td>
        </tr>
        <tr>
            <th>until:</th>
            <td>${prettyTo}</td>
        </tr>
    </table>
    <div id="actions"></div>
</div>
<div id="content">
    <ul class="tab-header">
        <li><a href="#overall">Overall</a></li>
        <li><a href="#facets">Facets</a></li>
        <li><a href="#suggestions">Suggestions</a></li>
        <li><a href="#advanced">Advanced</a></li>
        <li><a href="#fulltext">Fulltext</a></li>
    </ul>
    <div id="overall" class="section">
        <table class="overall">
            <tr>
                <th>Number of Requests</th>
                <td>${requests}</td>
            </tr>
            <tr>
                <th>Top Day</th>
                <td>${formattedTopDays?keys[0]!"NA"} (${formattedTopDays?values[0]!"NA"})</td>
            </tr>
            <tr>
                <th>Top Searchers</th>
                <td>
                    <table class="data">
                        <thead>
                            <tr>
                                <th>Ranking</th>
                                <th>Name</th>
                                <th>Number of Queries</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list topUsers as user, count>
                            <tr>
                                <td>${user?counter}</td>
                                <td>${user}</td>
                                <td>${count}</td>
                            </tr>
                            </#list>
                        </tbody>
                    </table>
                </td>
            </tr>
        </table>
    </div>
    <div id="facets" class="section"><h3>Fields</h3>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Name</th>
                <th>#Usages</th>
                <th>#Usages as 1</th>
                <th>#Usages as 2</th>
                <th>#Usages as 3</th>
                <th>#Usages as 4</th>
                <#list configuration.reportWriterConfiguration.facetFieldsExtension?keys as name>
                <th>${name}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <#list topFacetFields as field, value>
                <tr>
                    <td>${field?counter}</td>
                    <td>${field}</td>
                    <td>${value.total}</td>
                    <td>${value.first}</td>
                    <td>${value.second}</td>
                    <td>${value.third}</td>
                    <td>${value.fourth}</td>
                    <#list configuration.reportWriterConfiguration.facetFieldsExtension as column, fields>
                    <td>${fields[field]!"-"}</td>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
        <p>
           Hint: <span class="important">${countOfUnusedFacetFields} are not in use.</span>
        </p>
        <h3>Values</h3>
        <div class="tabs">
            <ul class="tab-header small">
            <#list facetFieldsValues as field, values>
                <#if values?has_content >
                    <li><a href="#facet-${field}">${field}</a></li>
                </#if>
            </#list>
            </ul>
        <#list facetFieldsValues as field, values>
            <#if values?has_content >
                <div id="facet-${field}">
                    <table class="data">
                        <thead>
                        <tr>
                            <th>Ranking</th>
                            <th>Value</th>
                            <th>#Usages</th>
                        </tr>
                        </thead>
                        <tbody>
                            <#list values as value, count>
                            <tr>
                                <td>${value?counter}</td>
                                <td>${value}</td>
                                <td>${count}</td>
                            </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>
            </#if>
        </#list>
        </div>
    </div>
    <div id="suggestions" class="section"><h3>Fields</h3>
        <table class="data">
            <thead>
                <tr>
                    <th>Ranking</th>
                    <th>Name</th>
                    <th>#Usages</th>
                    <th>#Usages as 1</th>
                    <th>#Usages as 2</th>
                    <th>#Usages as 3</th>
                    <th>#Usages as 4</th>
                    <#list configuration.reportWriterConfiguration.suggestionFieldsExtension?keys as name>
                    <th>${name}</th>
                    </#list>
                </tr>
            </thead>
            <tbody>
            <#list topSuggestionFields as field, value>
                <tr>
                    <td>${field?counter}</td>
                    <td>${field}</td>
                    <td>${value.total}</td>
                    <td>${value.first}</td>
                    <td>${value.second}</td>
                    <td>${value.third}</td>
                    <td>${value.fourth}</td>
                    <#list configuration.reportWriterConfiguration.suggestionFieldsExtension as column, fields>
                    <td>${fields[field]!"-"}</td>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
        <p>
            Hint: <span class="important">${countOfUnusedSuggestionFields} are not in use.</span>
        </p>
        <h3>Values</h3>
        <div class="tabs">
        <ul class="tab-header small">
            <#list suggestionFieldsValues as field, values>
                <#if values?has_content >
                    <li><a href="#suggestion-${field}">${field}</a></li>
                </#if>
            </#list>
        </ul>
        <#list suggestionFieldsValues as field, values>
            <#if values?has_content >
                <div id="suggestion-${field}">
                    <table class="data">
                        <thead>
                        <tr>
                            <th>Ranking</th>
                            <th>Value</th>
                            <th>#Usages</th>
                        </tr>
                        </thead>
                        <tbody>
                        <#list values as value, count>
                            <tr>
                                <td>${value?counter}</td>
                                <td>${value}</td>
                                <td>${count}</td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                </div>
            </#if>
        </#list>
        </div>
    </div>
    <div id="advanced" class="section"><h3>Fields</h3>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Name</th>
                <th>#Usages</th>
                <th>#Usages as 1</th>
                <th>#Usages as 2</th>
                <th>#Usages as 3</th>
                <th>#Usages as 4</th>
                <#list configuration.reportWriterConfiguration.filterFieldsExtension?keys as name>
                <th>${name}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <#list topFilterFields as field, value>
            <tr>
                <td>${field?counter}</td>
                <td>${field}</td>
                <td>${value.total}</td>
                <td>${value.first}</td>
                <td>${value.second}</td>
                <td>${value.third}</td>
                <td>${value.fourth}</td>
                <#list configuration.reportWriterConfiguration.filterFieldsExtension as column, fields>
                <td>${fields[field]!"-"}</td>
                </#list>
            </tr>
            </#list>
            </tbody>
        </table>
        <p>
            Hint: <span class="important">${countOfUnusedFilterFields} are not in use.</span>
        </p>
        <h3>Values</h3>
        <div class="tabs">
            <ul class="tab-header small">
            <#list filterFieldsValues as field, values>
                <#if values?has_content >
                    <li><a href="#filter-${field}">${field}</a></li>
                </#if>
            </#list>
            </ul>
        <#list filterFieldsValues as field, values>
            <#if values?has_content >
                <div id="filter-${field}">
                    <table class="data">
                        <thead>
                        <tr>
                            <th>Ranking</th>
                            <th>Value</th>
                            <th>#Usages</th>
                        </tr>
                        </thead>
                        <tbody>
                            <#list values as value, count>
                            <tr>
                                <td>${value?counter}</td>
                                <td>${value}</td>
                                <td>${count}</td>
                            </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>
            </#if>
        </#list>
        </div>
    </div>
    <div id="fulltext" class="section">
        <h3>Top Fulltext Queries</h3>
        <p>${totalTopQueries} queries with ${topQueries?size} distinct search terms.</p>
        <table class="data">
            <thead>
                <tr>
                    <th>Ranking</th>
                    <th>Value</th>
                    <th>#Usages</th>
                    <#list configuration.reportWriterConfiguration.fulltextQueryExtension?keys as name>
                    <th>${name}</th>
                    </#list>
                </tr>
            </thead>
            <tbody>
            <#list topQueries as query, count>
                <tr>
                    <td>${query?counter}</td>
                    <td>${query}</td>
                    <td>${count}</td>
                    <#list configuration.reportWriterConfiguration.fulltextQueryExtension as column, queries>
                    <td>${queries[query]!"-"}</td>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
        <#if configuration.reportWriterConfiguration.queryFilter?has_content>
        <h3>Top Filtered Fulltext Queries</h3>
        <p>Filter: without ${configuration.reportWriterConfiguration.queryFilter}</p>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Value</th>
                <th>#Usages</th>
            </tr>
            </thead>
            <tbody>
            <#list topFilteredQueries as query, count>
            <tr>
                <td>${query?counter}</td>
                <td>${query}</td>
                <td>${count}</td>
            </tr>
            </#list>
            </tbody>
        </table>
        </#if>
    </div>
</div>
</body>
</html>
