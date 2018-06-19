<html>
<head>
    <meta charset="utf-8"/>
    <link href="http://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css"/>
    <link href="http://cdn.datatables.net/1.10.18/css/jquery.dataTables.min.css" rel="stylesheet" type="text/css"/>
    <script src="https://code.jquery.com/jquery-1.12.4.min.js" integrity="sha256-ZosEbRLbNQzLpnKIkEdrPv7lOy9C27hHQ+Xp8a4MxAQ=" crossorigin="anonymous"></script>
    <script src="http://cdn.datatables.net/1.10.18/js/jquery.dataTables.min.js" type="text/javascript"></script>
    <script>
        $(document).ready( function () {
            $('.data').DataTable();
        } );
    </script>
    <style>
    body {
        font-family: 'Roboto', sans-serif;
        font-size: 13px;
        color: #606163;
        margin: 200px 30px 0 30px;
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

    #content tr.unusedAll {
        color: red;
    }

    #content tr.unused {
        color: orange;
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
    </style>
</head>
<body>
<div id="header">
    <img src="${imageUrl}"/>
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
</div>
<div id="content">
    <div class="section"><h2>Overall</h2>
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
    <div class="section"><h2>Filters</h2>
        <p>This part shows which fields are set for filters in facets, suggestions and advanced and how they are used
            within the given timerange.
            The evaluation for each filter type is split in two parts:<i>Fields</i> and <i>Values</i>

    <div class="section"><h3>Facets</h3><h4>Facet Fields</h4>
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
        <h4>Facet Values</h4>
        <p>
        <#list facetFieldsValues as field, values>
            <#if values?has_content >
        <h5>Top values for <i>${field}</i></h5>
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
            </#if>
        </#list>
        </p>
    </div>
    <div class="section"><h3>Suggestions</h3><h4>Suggestion Fields</h4>
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
        <h4>Suggestion Values</h4>
        <p>
        <#list suggestionFieldsValues as field, values>
            <#if values?has_content >

        <h5>Top values for <i>${field}</i></h5>
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
            </#if>
        </#list>
        </p>
    </div>
    <div class="section"><h3>Filters</h3><h4>Filter Fields</h4>
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
        <h4>Filter Values</h4>
        <p>
        <#list filterFieldsValues as field, values>
        <#if values?has_content >
            <h5>Top values for <i>${field}</i></h5>
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
        </#if>
        </#list>

        </p>
    </div>
    <div class="section">
        <h2>Fulltext</h2>
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
        <h3>Top Filtered Fulltext Queries</h3>
        <p>Filter: without <#list configuration.reportWriterConfiguration.queryFilters as filter>${filter}  </#list></p>
        <p>14769 queries with 3133 distinct search terms.</p>
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
    </div>
</div>
</body>
</html>
