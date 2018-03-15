<html>
<head>
    <meta charset="utf-8"/>
    <link href="http://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css"/>
    <link href="http://cdn.datatables.net/1.10.15/css/jquery.dataTables.min.css" rel="stylesheet" type="text/css"/>
    <script src="http://code.jquery.com/jquery-1.12.4.js" type="text/javascript"></script>
    <script src="http://cdn.datatables.net/1.10.15/js/jquery.dataTables.min.js" type="text/javascript"></script>
    <!--script(type='text/javascript')--><!--  $(document).ready(function() {--><!--  $('table.data').DataTable();-->
    <!--  });-->
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
    <img src="https://image3.redbull.com/rbx00023/0001/1/210/403/logos/logos-homepage/brandlogos/jpg/red-bull-mediahouse-01.jpg"/>
    <h1>Vind - Report</h1>
    <h2>${prettyToday}</h2>
    <table>
        <tr>
            <th>Name:</th>
            <td>${applicationName}</td>
        </tr>
        <tr>
            <th>App:</th>
            <td>${applicationName}</td>
        </tr>
        <#list configuration.generalFilters as name, value>
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
                    <ol>
                    <#list topUsers as user, count>
                        <li>${user} (${count})</li>
                    </#list>
                    </ol>
                </td>
            </tr>
        </table>
    </div>
    <div class="section"><h2>Filters</h2>
        <p>This part shows which fields are set for filters in facets, suggestions and advanced and how they are used
            within the given timerange.
            The evaluation for each filter type is split in three parts:<i>Fields</i>, <i>Values</i> and <i>Tips</i>.
            In <i>Fields</i> we outline the current configuration together with the real field usage.
            In <i>Values</i> we show the top usages for the each field which has a standard deviation greater than 10 in
            usage.
            In <i>Tips</i> we give some hints how the search experience may be improved.</p></div>
    <div class="section"><h3>Facets</h3><h4>Facet Fields</h4>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Name</th>
                <th>#Usages</th>
                <#list configuration.facetFieldsExtension?keys as name>
                <th>${name}</th>
                </#list>
            </tr>
            </thead>
            <tbody>
            <#list topFacetFields as field, count>
                <tr>
                    <td>${field?counter}</td>
                    <td>${field}</td>
                    <td>${count}</td>
                    <#list configuration.facetFieldsExtension as column, fields>
                    <td>${fields[field]!"-"}</td>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
        <h4>Facet Values</h4>
        <p>Only considers fields with a standard deviation greater than 10, only considers top 25 values of the first 3
            search steps.
        <#list facetFieldsValues as field, values>
        <h5>Top 10 values for <i>${field}</i></h5>
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
                    <#list configuration.suggestionFieldsExtension?keys as name>
                    <th>${name}</th>
                    </#list>
                </tr>
            </thead>
            <tbody>
            <#list topSuggestionFields as field, count>
                <tr>
                    <td>${field?counter}</td>
                    <td>${field}</td>
                    <td>${count}</td>
                    <#list configuration.suggestionFieldsExtension as column, fields>
                    <td>${fields[field]!"-"}</td>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>
    <div class="section">
        <h2>Fulltext</h2>
        <h3>Fulltext Queries (Top 100)</h3>
        <p>${totalTopQueries} queries with ${topQueries?size} distinct search terms.</p>
        <table class="data">
            <thead>
                <tr>
                    <th>Ranking</th>
                    <th>Value</th>
                    <th>#Usages</th>
                    <#list configuration.fulltextQueryExtension?keys as name>
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
                    <#list configuration.fulltextQueryExtension as column, queries>
                    <td>${queries[query]!"-"}</td>
                    </#list>
                </tr>
            </#list>
            </tbody>
        </table>
        <h3>Filtered Fulltext Queries (Top 100)</h3>
        <p>Filter: without <#list configuration.queryFilters as filter>${filter}  </#list></p>
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